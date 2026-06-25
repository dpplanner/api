package com.dp.dplanner.service;

import com.dp.dplanner.service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static com.dp.dplanner.exception.ErrorResult.KAKAO_LOGIN_FAILED;

/**
 * 카카오 OAuth(웹) 처리: 인가 코드 → 토큰 교환 → 사용자 정보 조회.
 */
@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.rest-api-key:}")
    private String restApiKey;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    /**
     * 인가 코드로 카카오 토큰을 교환하고 사용자 정보를 조회한다.
     */
    public KakaoUserInfo getUserInfo(String authorizationCode, String redirectUri) {
        String accessToken = exchangeToken(authorizationCode, redirectUri);
        return fetchUserInfo(accessToken);
    }

    private String exchangeToken(String authorizationCode, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", restApiKey);
        params.add("redirect_uri", redirectUri);
        params.add("code", authorizationCode);
        if (clientSecret != null && !clientSecret.isBlank()) {
            params.add("client_secret", clientSecret);
        }

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(TOKEN_URL, new HttpEntity<>(params, headers), Map.class);
            Object token = response.getBody() != null ? response.getBody().get("access_token") : null;
            if (token == null) {
                log.error("Kakao token exchange returned no access_token: {}", response.getBody());
                throw new ServiceException(KAKAO_LOGIN_FAILED);
            }
            return token.toString();
        } catch (RestClientException e) {
            log.error("Kakao token exchange failed: {}", e.getMessage());
            throw new ServiceException(KAKAO_LOGIN_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    private KakaoUserInfo fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    USER_INFO_URL, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new ServiceException(KAKAO_LOGIN_FAILED);
            }

            String email = null;
            String nickname = null;
            Object accountObj = body.get("kakao_account");
            if (accountObj instanceof Map) {
                Map<String, Object> account = (Map<String, Object>) accountObj;
                Object emailObj = account.get("email");
                email = emailObj != null ? emailObj.toString() : null;

                Object profileObj = account.get("profile");
                if (profileObj instanceof Map) {
                    Object nicknameObj = ((Map<String, Object>) profileObj).get("nickname");
                    nickname = nicknameObj != null ? nicknameObj.toString() : null;
                }
            }
            return new KakaoUserInfo(email, nickname);
        } catch (RestClientException e) {
            log.error("Kakao user info request failed: {}", e.getMessage());
            throw new ServiceException(KAKAO_LOGIN_FAILED);
        }
    }

    public record KakaoUserInfo(String email, String nickname) {
    }
}

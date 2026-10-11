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

import java.util.List;
import java.util.Map;

import static com.dp.dplanner.exception.ErrorResult.KAKAO_LOGIN_FAILED;

/**
 * 카카오 OAuth 처리.
 * - 웹: 인가 코드 → 토큰 교환 → 사용자 정보 조회
 * - 앱: 앱이 받은 액세스 토큰 → 우리 앱이 발급한 토큰인지(app_id) 확인 → 사용자 정보 조회
 */
@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";

    private final RestTemplate restTemplate;

    public KakaoOAuthClient() {
        this(new RestTemplate());
    }

    KakaoOAuthClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${kakao.rest-api-key:}")
    private String restApiKey;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    /** 카카오 개발자 콘솔의 앱 ID(숫자). 쉼표로 여러 개. 비어 있으면 app_id 확인을 건너뛴다(경고 로그). */
    @Value("${kakao.app-ids:}")
    private List<String> allowedAppIds = List.of();

    /**
     * 인가 코드로 카카오 토큰을 교환하고 사용자 정보를 조회한다.
     */
    public KakaoUserInfo getUserInfo(String authorizationCode, String redirectUri) {
        String accessToken = exchangeToken(authorizationCode, redirectUri);
        return fetchUserInfo(accessToken);
    }

    /**
     * 앱(네이티브 SDK)이 받은 액세스 토큰으로 사용자 정보를 조회한다.
     * 다른 카카오 앱에서 발급된 토큰을 끼워 넣는 것을 막기 위해 app_id 를 먼저 확인한다.
     */
    public KakaoUserInfo getUserInfoByAccessToken(String accessToken) {
        verifyAppId(accessToken);
        return fetchUserInfo(accessToken);
    }

    private void verifyAppId(String accessToken) {
        List<String> appIds = allowedAppIds.stream().map(String::trim).filter(id -> !id.isEmpty()).toList();
        if (appIds.isEmpty()) {
            log.warn("kakao.app-ids is not configured; skipping kakao app_id check");
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    TOKEN_INFO_URL, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Object appId = response.getBody() != null ? response.getBody().get("app_id") : null;
            if (appId == null || !appIds.contains(appId.toString())) {
                log.warn("Kakao access token issued for another app: app_id={}", appId);
                throw new ServiceException(KAKAO_LOGIN_FAILED);
            }
        } catch (RestClientException e) {
            log.warn("Kakao token info request failed: {}", e.getMessage());
            throw new ServiceException(KAKAO_LOGIN_FAILED);
        }
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
                // 인증되지 않은 이메일은 남의 이메일일 수 있으므로 식별에 쓰지 않는다
                boolean verified = Boolean.TRUE.equals(account.get("is_email_valid"))
                        && Boolean.TRUE.equals(account.get("is_email_verified"));
                email = emailObj != null && verified ? emailObj.toString() : null;

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

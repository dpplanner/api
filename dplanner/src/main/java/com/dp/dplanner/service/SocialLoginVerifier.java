package com.dp.dplanner.service;

import com.dp.dplanner.service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dp.dplanner.exception.ErrorResult.*;

/**
 * 앱이 보낸 소셜 로그인 토큰을 서버가 직접 검증해 이메일을 얻는다.
 * - apple / google: ID 토큰(JWT) 서명·iss·aud·만료 검증
 * - kakao: 액세스 토큰 → 토큰 정보(app_id) 확인 후 사용자 정보 조회
 * - naver: 액세스 토큰 → 사용자 정보 조회
 */
@Slf4j
@Component
public class SocialLoginVerifier {

    private static final String APPLE_JWK_URI = "https://appleid.apple.com/auth/keys";
    private static final Set<String> APPLE_ISSUERS = Set.of("https://appleid.apple.com");
    private static final String GOOGLE_JWK_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final JwtDecoder appleDecoder;
    private final JwtDecoder googleDecoder;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final RestTemplate restTemplate;

    @Autowired
    public SocialLoginVerifier(@Value("${auth.apple.client-ids:com.dancepozz.dplanner}") List<String> appleClientIds,
                               @Value("${auth.google.client-ids:}") List<String> googleClientIds,
                               KakaoOAuthClient kakaoOAuthClient) {
        this(idTokenDecoder(APPLE_JWK_URI, APPLE_ISSUERS, appleClientIds),
                idTokenDecoder(GOOGLE_JWK_URI, GOOGLE_ISSUERS, googleClientIds),
                kakaoOAuthClient,
                new RestTemplate());
    }

    SocialLoginVerifier(JwtDecoder appleDecoder, JwtDecoder googleDecoder,
                        KakaoOAuthClient kakaoOAuthClient, RestTemplate restTemplate) {
        this.appleDecoder = appleDecoder;
        this.googleDecoder = googleDecoder;
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.restTemplate = restTemplate;
    }

    public VerifiedUser verify(String provider, String token) {
        if (provider == null || token == null || token.isBlank()) {
            throw new ServiceException(REQUEST_IS_INVALID);
        }
        return switch (provider.toLowerCase()) {
            case "apple" -> verifyIdToken(appleDecoder, token, "apple");
            case "google" -> verifyIdToken(googleDecoder, token, "google");
            case "kakao" -> verifyKakao(token);
            case "naver" -> verifyNaver(token);
            default -> throw new ServiceException(REQUEST_IS_INVALID);
        };
    }

    private VerifiedUser verifyIdToken(JwtDecoder decoder, String idToken, String provider) {
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            log.warn("{} id token rejected: {}", provider, e.getMessage());
            throw new ServiceException(SOCIAL_LOGIN_FAILED);
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new ServiceException(SOCIAL_LOGIN_FAILED);
        }
        // apple 은 "true" 문자열, google 은 boolean 으로 온다. 클레임이 없으면(apple 일부 케이스) 통과.
        Object emailVerified = jwt.getClaims().get("email_verified");
        if (emailVerified != null && !"true".equalsIgnoreCase(emailVerified.toString())) {
            throw new ServiceException(SOCIAL_EMAIL_UNVERIFIED);
        }
        return new VerifiedUser(email, jwt.getClaimAsString("name"));
    }

    private VerifiedUser verifyKakao(String accessToken) {
        KakaoOAuthClient.KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfoByAccessToken(accessToken);
        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new ServiceException(KAKAO_EMAIL_REQUIRED);
        }
        return new VerifiedUser(userInfo.email(), userInfo.nickname());
    }

    @SuppressWarnings("unchecked")
    private VerifiedUser verifyNaver(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        Map<String, Object> body;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    NAVER_USER_INFO_URL, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            body = response.getBody();
        } catch (RestClientException e) {
            log.warn("naver user info request failed: {}", e.getMessage());
            throw new ServiceException(SOCIAL_LOGIN_FAILED);
        }
        if (body == null || !"00".equals(String.valueOf(body.get("resultcode")))
                || !(body.get("response") instanceof Map)) {
            throw new ServiceException(SOCIAL_LOGIN_FAILED);
        }
        Map<String, Object> user = (Map<String, Object>) body.get("response");
        Object email = user.get("email");
        if (email == null || email.toString().isBlank()) {
            throw new ServiceException(SOCIAL_LOGIN_FAILED);
        }
        Object name = user.get("name") != null ? user.get("name") : user.get("nickname");
        return new VerifiedUser(email.toString(), name != null ? name.toString() : null);
    }

    static JwtDecoder idTokenDecoder(String jwkSetUri, Set<String> issuers, List<String> audiences) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(idTokenValidator(issuers, audiences));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> idTokenValidator(Set<String> issuers, List<String> audiences) {
        Set<String> allowedAudiences = audiences.stream()
                .map(String::trim).filter(a -> !a.isEmpty()).collect(java.util.stream.Collectors.toSet());

        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String iss = jwt.getClaimAsString("iss");
            return iss != null && issuers.contains(iss)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "bad issuer: " + iss, null));
        };
        // aud 미설정이면 전부 거부 (설정 누락으로 아무 앱의 토큰이나 통과하는 일이 없도록)
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> aud = jwt.getAudience();
            return aud != null && aud.stream().anyMatch(allowedAudiences::contains)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "bad audience: " + aud, null));
        };
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuerValidator, audienceValidator);
    }

    public record VerifiedUser(String email, String name) {
    }
}

package com.dp.dplanner.service;

import com.dp.dplanner.service.exception.ServiceException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class SocialLoginVerifierTest {

    private static final String APPLE_AUD = "com.dancepozz.dplanner";
    private static final String GOOGLE_AUD = "web-client.apps.googleusercontent.com";

    private KeyPair keyPair;
    private KeyPair otherKeyPair;
    private RestTemplate kakaoRestTemplate;
    private RestTemplate naverRestTemplate;
    private MockRestServiceServer kakaoServer;
    private MockRestServiceServer naverServer;
    private SocialLoginVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        otherKeyPair = generator.generateKeyPair();

        kakaoRestTemplate = new RestTemplate();
        naverRestTemplate = new RestTemplate();
        kakaoServer = MockRestServiceServer.createServer(kakaoRestTemplate);
        naverServer = MockRestServiceServer.createServer(naverRestTemplate);

        KakaoOAuthClient kakaoOAuthClient = new KakaoOAuthClient(kakaoRestTemplate);
        ReflectionTestUtils.setField(kakaoOAuthClient, "allowedAppIds", List.of("12345"));

        verifier = new SocialLoginVerifier(
                decoder(Set.of("https://appleid.apple.com"), List.of(APPLE_AUD)),
                decoder(Set.of("https://accounts.google.com", "accounts.google.com"), List.of(GOOGLE_AUD)),
                kakaoOAuthClient,
                naverRestTemplate);
    }

    private JwtDecoder decoder(Set<String> issuers, List<String> audiences) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        decoder.setJwtValidator(SocialLoginVerifier.idTokenValidator(issuers, audiences));
        return decoder;
    }

    private String idToken(KeyPair signingKey, String iss, String aud, Instant exp, Object emailVerified) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(iss)
                .audience(aud)
                .subject("sub-1")
                .issueTime(Date.from(Instant.now().minusSeconds(10)))
                .expirationTime(Date.from(exp))
                .claim("email", "user@example.com");
        if (emailVerified != null) {
            claims.claim("email_verified", emailVerified);
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
        jwt.sign(new RSASSASigner((RSAPrivateKey) signingKey.getPrivate()));
        return jwt.serialize();
    }

    private Instant future() {
        return Instant.now().plusSeconds(600);
    }

    @Test
    @DisplayName("애플 ID 토큰이 유효하면 토큰 안의 이메일을 쓴다 (email_verified 문자열 \"true\")")
    void apple_valid() throws Exception {
        String token = idToken(keyPair, "https://appleid.apple.com", APPLE_AUD, future(), "true");

        SocialLoginVerifier.VerifiedUser user = verifier.verify("apple", token);

        assertThat(user.email()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("구글 ID 토큰이 유효하면 통과 (iss 접두사 없는 형태 포함)")
    void google_valid() throws Exception {
        assertThat(verifier.verify("google", idToken(keyPair, "accounts.google.com", GOOGLE_AUD, future(), true)).email())
                .isEqualTo("user@example.com");
        assertThat(verifier.verify("GOOGLE", idToken(keyPair, "https://accounts.google.com", GOOGLE_AUD, future(), true)).email())
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("다른 앱(aud)용 토큰은 거부")
    void wrongAudience() throws Exception {
        String token = idToken(keyPair, "https://accounts.google.com", "other-app", future(), true);

        assertThatThrownBy(() -> verifier.verify("google", token))
                .isInstanceOf(ServiceException.class)
                .extracting("errorResult").isEqualTo(SOCIAL_LOGIN_FAILED);
    }

    @Test
    @DisplayName("발급자(iss)가 다르면 거부 — 구글 토큰을 애플로 보내는 경우")
    void wrongIssuer() throws Exception {
        String token = idToken(keyPair, "https://accounts.google.com", APPLE_AUD, future(), true);

        assertThatThrownBy(() -> verifier.verify("apple", token))
                .extracting("errorResult").isEqualTo(SOCIAL_LOGIN_FAILED);
    }

    @Test
    @DisplayName("만료된 토큰은 거부")
    void expired() throws Exception {
        String token = idToken(keyPair, "https://appleid.apple.com", APPLE_AUD, Instant.now().minusSeconds(3600), "true");

        assertThatThrownBy(() -> verifier.verify("apple", token))
                .extracting("errorResult").isEqualTo(SOCIAL_LOGIN_FAILED);
    }

    @Test
    @DisplayName("서명이 다른 키로 된 토큰은 거부")
    void badSignature() throws Exception {
        String token = idToken(otherKeyPair, "https://appleid.apple.com", APPLE_AUD, future(), "true");

        assertThatThrownBy(() -> verifier.verify("apple", token))
                .extracting("errorResult").isEqualTo(SOCIAL_LOGIN_FAILED);
    }

    @Test
    @DisplayName("email_verified=false 면 거부")
    void unverifiedEmail() throws Exception {
        String token = idToken(keyPair, "https://accounts.google.com", GOOGLE_AUD, future(), false);

        assertThatThrownBy(() -> verifier.verify("google", token))
                .extracting("errorResult").isEqualTo(SOCIAL_EMAIL_UNVERIFIED);
    }

    @Test
    @DisplayName("aud 설정이 비어 있으면 모든 토큰 거부")
    void emptyAudienceConfig() throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        decoder.setJwtValidator(SocialLoginVerifier.idTokenValidator(Set.of("https://accounts.google.com"), List.of("")));
        SocialLoginVerifier strict = new SocialLoginVerifier(decoder, decoder, null, naverRestTemplate);

        String token = idToken(keyPair, "https://accounts.google.com", GOOGLE_AUD, future(), true);

        assertThatThrownBy(() -> strict.verify("google", token))
                .extracting("errorResult").isEqualTo(SOCIAL_LOGIN_FAILED);
    }

    @Test
    @DisplayName("알 수 없는 provider, 빈 토큰은 잘못된 요청")
    void invalidRequest() {
        assertThatThrownBy(() -> verifier.verify("facebook", "x"))
                .extracting("errorResult").isEqualTo(REQUEST_IS_INVALID);
        assertThatThrownBy(() -> verifier.verify("google", " "))
                .extracting("errorResult").isEqualTo(REQUEST_IS_INVALID);
        assertThatThrownBy(() -> verifier.verify(null, "x"))
                .extracting("errorResult").isEqualTo(REQUEST_IS_INVALID);
    }

    @Test
    @DisplayName("카카오: 우리 앱 토큰이고 이메일이 인증돼 있으면 통과")
    void kakao_valid() {
        kakaoServer.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andExpect(header(AUTHORIZATION, "Bearer kakao-token"))
                .andRespond(withSuccess("{\"id\":1,\"app_id\":12345,\"expires_in\":100}", MediaType.APPLICATION_JSON));
        kakaoServer.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1,\"kakao_account\":{\"email\":\"k@kakao.com\",\"is_email_valid\":true,"
                        + "\"is_email_verified\":true,\"profile\":{\"nickname\":\"닉\"}}}", MediaType.APPLICATION_JSON));

        SocialLoginVerifier.VerifiedUser user = verifier.verify("kakao", "kakao-token");

        assertThat(user.email()).isEqualTo("k@kakao.com");
        assertThat(user.name()).isEqualTo("닉");
        kakaoServer.verify();
    }

    @Test
    @DisplayName("카카오: 다른 앱에서 발급된 토큰은 사용자 정보 조회 전에 거부")
    void kakao_otherApp() {
        kakaoServer.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withSuccess("{\"id\":1,\"app_id\":99999}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("kakao", "kakao-token"))
                .extracting("errorResult").isEqualTo(KAKAO_LOGIN_FAILED);
        kakaoServer.verify();
    }

    @Test
    @DisplayName("카카오: 토큰이 무효면(401) 거부")
    void kakao_invalidToken() {
        kakaoServer.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> verifier.verify("kakao", "bad"))
                .extracting("errorResult").isEqualTo(KAKAO_LOGIN_FAILED);
    }

    @Test
    @DisplayName("카카오: 인증되지 않은 이메일은 쓰지 않는다")
    void kakao_unverifiedEmail() {
        kakaoServer.expect(requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withSuccess("{\"id\":1,\"app_id\":12345}", MediaType.APPLICATION_JSON));
        kakaoServer.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withSuccess("{\"id\":1,\"kakao_account\":{\"email\":\"victim@example.com\","
                        + "\"is_email_valid\":true,\"is_email_verified\":false}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> verifier.verify("kakao", "kakao-token"))
                .extracting("errorResult").isEqualTo(KAKAO_EMAIL_REQUIRED);
    }

    @Test
    @DisplayName("네이버: 사용자 정보 조회 성공 시 이메일·이름 사용")
    void naver_valid() {
        naverServer.expect(requestTo("https://openapi.naver.com/v1/nid/me"))
                .andExpect(header(AUTHORIZATION, "Bearer naver-token"))
                .andRespond(withSuccess("{\"resultcode\":\"00\",\"message\":\"success\","
                        + "\"response\":{\"id\":\"x\",\"email\":\"n@naver.com\",\"name\":\"네이버\"}}", MediaType.APPLICATION_JSON));

        SocialLoginVerifier.VerifiedUser user = verifier.verify("naver", "naver-token");

        assertThat(user.email()).isEqualTo("n@naver.com");
        assertThat(user.name()).isEqualTo("네이버");
    }

    @Test
    @DisplayName("네이버: 토큰이 무효면 거부")
    void naver_invalid() {
        naverServer.expect(requestTo("https://openapi.naver.com/v1/nid/me"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> verifier.verify("naver", "bad"))
                .extracting("errorResult").isEqualTo(SOCIAL_LOGIN_FAILED);
    }
}

package com.dp.dplanner.service;

import com.dp.dplanner.adapter.dto.LoginDto;
import com.dp.dplanner.adapter.dto.LoginResponseDto;
import com.dp.dplanner.config.security.JwtTokenProvider;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.service.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    MemberRepository memberRepository;
    @Mock
    ClubMemberRepository clubMemberRepository;
    @Mock
    JwtTokenProvider tokenProvider;
    @Mock
    KakaoOAuthClient kakaoOAuthClient;
    @Mock
    SocialLoginVerifier socialLoginVerifier;
    @InjectMocks
    AuthService authService;

    private LoginDto dto(String email, String name, String provider, String token) {
        return LoginDto.builder().email(email).name(name).provider(provider).token(token).build();
    }

    @Test
    @DisplayName("토큰 로그인: 요청 body 의 email 이 아니라 검증된 토큰의 email 로 회원을 찾는다")
    void tokenLogin_usesVerifiedEmail() {
        Member member = Member.builder().email("real@example.com").name("본인").build();
        given(socialLoginVerifier.verify("google", "id-token"))
                .willReturn(new SocialLoginVerifier.VerifiedUser("real@example.com", "구글이름"));
        given(memberRepository.findByEmail("real@example.com")).willReturn(Optional.of(member));
        given(tokenProvider.generateAccessToken(any())).willReturn("access");
        given(tokenProvider.generateRefreshToken(any())).willReturn("refresh");

        LoginResponseDto response = authService.login(dto("victim@example.com", "x", "google", "id-token"));

        assertThat(response.getAccessToken()).isEqualTo("access");
        then(memberRepository).should(never()).findByEmail("victim@example.com");
    }

    @Test
    @DisplayName("토큰 로그인 신규 가입: 앱이 보낸 이름이 있으면 그 이름, 없으면 토큰의 이름")
    void tokenLogin_newMemberName() {
        given(socialLoginVerifier.verify(any(), any()))
                .willReturn(new SocialLoginVerifier.VerifiedUser("new@example.com", "토큰이름"));
        given(memberRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willAnswer(inv -> inv.getArgument(0));

        authService.login(dto(null, null, "kakao", "t"));
        authService.login(dto(null, "앱이름", "apple", "t"));

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        then(memberRepository).should(times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Member::getName).containsExactly("토큰이름", "앱이름");
    }

    @Test
    @DisplayName("토큰 검증 실패 시 로그인 실패 (옛 방식으로 넘어가지 않음)")
    void tokenLogin_failure() {
        given(socialLoginVerifier.verify("google", "forged"))
                .willThrow(new ServiceException(SOCIAL_LOGIN_FAILED));

        assertThatThrownBy(() -> authService.login(dto("victim@example.com", "x", "google", "forged")))
                .extracting("errorResult").isEqualTo(SOCIAL_LOGIN_FAILED);
        then(memberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("옛 방식(email+name): 허용 중이면 기존처럼 로그인")
    void legacyLogin_enabled() {
        Member member = Member.builder().email("a@example.com").name("a").build();
        given(memberRepository.findByEmail("a@example.com")).willReturn(Optional.of(member));

        authService.login(dto("a@example.com", "a", null, null));

        then(socialLoginVerifier).shouldHaveNoInteractions();
        then(memberRepository).should().findByEmail("a@example.com");
    }

    @Test
    @DisplayName("옛 방식(email+name): 차단 설정이면 거부")
    void legacyLogin_disabled() {
        ReflectionTestUtils.setField(authService, "legacyLoginEnabled", false);

        assertThatThrownBy(() -> authService.login(dto("a@example.com", "a", null, null)))
                .extracting("errorResult").isEqualTo(LEGACY_LOGIN_DISABLED);
        then(memberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("옛 방식인데 email 이 없으면 잘못된 요청")
    void legacyLogin_missingEmail() {
        assertThatThrownBy(() -> authService.login(dto(null, "a", null, null)))
                .extracting("errorResult").isEqualTo(REQUEST_IS_INVALID);
    }
}

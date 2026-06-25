package com.dp.dplanner.service;

import com.dp.dplanner.adapter.dto.LoginResponseDto;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.adapter.dto.KakaoWebLoginDto;
import com.dp.dplanner.adapter.dto.LoginDto;
import com.dp.dplanner.adapter.dto.TokenDto;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.config.security.JwtTokenProvider;
import com.dp.dplanner.config.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.dp.dplanner.exception.ErrorResult.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final ClubMemberRepository clubMemberRepository;
//    private final ClubRepository clubRepository;
    private final JwtTokenProvider tokenProvider;
    private final KakaoOAuthClient kakaoOAuthClient;

    @Transactional
    public TokenDto refreshToken(TokenDto tokenDto) {

        String oldRefreshToken = tokenDto.getRefreshToken();

        if (!tokenProvider.verify(oldRefreshToken)) {
            throw new ServiceException(INVALID_TOKEN);
        }

        Member member = memberRepository.findByRefreshToken(oldRefreshToken).orElseThrow(() -> new ServiceException(INVALID_TOKEN)); // 저장된 refreshToken으로 member 정보 가져오기
        
        PrincipalDetails principal = new PrincipalDetails(member.getId(), null,null,"", null); // club_id , club_member_id 넣어줄 필요 없음
        Authentication authentication =  new UsernamePasswordAuthenticationToken(principal, "",null);
        
        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        return TokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
    @Transactional
    public LoginResponseDto login(LoginDto loginDto) {
        return issueLogin(loginDto.getEmail(), loginDto.getName());
    }

    /**
     * 웹 전용 카카오 로그인.
     * 프론트가 받은 인가 코드를 서버가 카카오와 토큰 교환 → 사용자 정보 조회 후
     * 기존 로그인 로직(email+name)을 재사용해 우리 JWT 를 발급한다.
     */
    @Transactional
    public LoginResponseDto loginWithKakaoWeb(KakaoWebLoginDto dto) {
        KakaoOAuthClient.KakaoUserInfo userInfo =
                kakaoOAuthClient.getUserInfo(dto.getAuthorizationCode(), dto.getRedirectUri());

        if (userInfo.email() == null || userInfo.email().isBlank()) {
            // 카카오 이메일 미동의/비공개 시 회원 식별 불가 → 프론트가 이메일 제공 동의를 다시 받도록 유도
            throw new ServiceException(KAKAO_EMAIL_REQUIRED);
        }
        String name = (userInfo.nickname() != null && !userInfo.nickname().isBlank())
                ? userInfo.nickname() : "카카오사용자";

        return issueLogin(userInfo.email(), name);
    }

    private LoginResponseDto issueLogin(String email, String name) {
        PrincipalDetails principalDetail;
        Authentication authentication;

        Optional<Member> optionalMember = memberRepository.findByEmail(email);
        Member member = optionalMember.orElseGet(() -> createMember(name, email));

        Optional<ClubMember> optionalClubMember = member.getRecentClub() != null ?
                clubMemberRepository.findByClubIdAndMemberId(member.getRecentClub().getId(), member.getId()) :
                Optional.empty();
        if (optionalClubMember.isPresent()) {
            principalDetail = PrincipalDetails.create(member, member.getRecentClub(), optionalClubMember.get(), null);
        } else {
            principalDetail = PrincipalDetails.create(member, null, null, null);
        }

        authentication = new UsernamePasswordAuthenticationToken(principalDetail, null, null);

        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .eula(member.getEula())
                .build();
    }

    @Transactional
    public void eula(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(MEMBER_NOT_FOUND));
        member.agreeEula();
    }

    private Member createMember(String name, String email) {

        return memberRepository.save(Member.builder()
                .name(name)
                .email(email)
                .build()
        );
    }
}

package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.LoginDto;
import com.dp.dplanner.dto.TokenDto;
import com.dp.dplanner.exception.ServiceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.security.JwtTokenProvider;
import com.dp.dplanner.security.PrincipalDetails;
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
    public TokenDto login(LoginDto loginDto) {
        String email = loginDto.getEmail();
        String name = loginDto.getName();
        PrincipalDetails principalDetail;
        Authentication authentication;

        Optional<Member> optionalMember = memberRepository.findByEmail(email);
        Member member = optionalMember.orElseGet(() -> createMember(name,email));

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

        return TokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    private Member createMember(String name, String email) {

        return memberRepository.save(Member.builder()
                .name(name)
                .email(email)
                .build()
        );
    }
}

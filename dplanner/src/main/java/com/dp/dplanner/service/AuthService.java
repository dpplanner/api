package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.dto.TokenDto;
import com.dp.dplanner.exception.AuthenticationException;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.security.JwtTokenProvider;
import com.dp.dplanner.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import static com.dp.dplanner.exception.ErrorResult.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider tokenProvider;


    public TokenDto refreshToken(TokenDto tokenDto) {

        String oldRefreshToken = tokenDto.getRefreshToken();

        if (!tokenProvider.verify(oldRefreshToken)) {
            throw new AuthenticationException(INVALID_TOKEN);
        }

        Member member = memberRepository.findByRefreshToken(oldRefreshToken).orElseThrow(() -> new AuthenticationException(INVALID_TOKEN)); // 저장된 refreshToken으로 member 정보 가져오기
        
        PrincipalDetails principal = new PrincipalDetails(member.getId(), null,null,"", null); // club_id , club_member_id 넣어줄 필요 없음
        Authentication authentication =  new UsernamePasswordAuthenticationToken(principal, "",null);
        
        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        return TokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

}

package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.dto.TokenDto;
import com.dp.dplanner.exception.AuthenticationException;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.security.JwtTokenProvider;
import com.dp.dplanner.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        Authentication authentication = tokenProvider.getAuthentication(tokenDto.getAccessToken());
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Long id = Long.valueOf(principal.getName());
        Member member = memberRepository.findById(id).orElseThrow(()
                -> new AuthenticationException(INVALID_TOKEN));

        String savedRefreshToken = member.getRefreshToken();

        if (!oldRefreshToken.equals(savedRefreshToken)) {
            throw new AuthenticationException(INVALID_TOKEN);
        }

        String newAccessToken = tokenProvider.generateAccessToken(authentication);
        return TokenDto.builder()
                .accessToken(newAccessToken)
                .build();
    }

}

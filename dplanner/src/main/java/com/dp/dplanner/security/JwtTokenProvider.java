package com.dp.dplanner.security;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.repository.MemberRepository;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final String secret = "ZHBsYW5uZXI=";
    private static final Long ACCESS_TOKEN_VALID_TIME = 30 * 60 * 1000L;            // 30 min
    private static final Long REFRESH_TOKEN_VALID_TIME = 3 * 30 * 24 * 30 * 60 * 1000L; // 3 month
    private final MemberRepository memberRepository;


    public boolean verify(String token) {
        try {
            Claims claims = getClaims(token);
            return !isExpired(claims);
        } catch (ExpiredJwtException e) {
            log.info("토큰 유효기간 만료");
        } catch (IllegalStateException e) {
            log.info("올바르지 않은 토큰");
        }
        return false;
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = getClaims(accessToken);
        Long id = Long.valueOf(claims.getSubject());

        PrincipalDetails principal = new PrincipalDetails(id, "", null);

        return new UsernamePasswordAuthenticationToken(principal, "");
    }

    public Token getToken(Authentication authentication) {
        return new Token(
                generateAccessToken(authentication),
                generateRefreshToken(authentication)
        );
    }

    //TODO accessToken 만료 && refreshToken 유효 -> accessToken 재발급
    public String refresh() {
        return null;
    }



    private static boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private String generateAccessToken(Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Claims claims = Jwts.claims().setSubject(principal.getName());
        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuer("dplanner")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_VALID_TIME))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    private String generateRefreshToken(Authentication authentication) {
        Date now = new Date();

        String refreshToken = Jwts.builder()
                .setIssuer("dplanner")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_VALID_TIME))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Long id = Long.valueOf(principal.getName());
        Member member = memberRepository.findById(id).orElseThrow(
                () -> new MemberException(ErrorResult.MEMBER_NOT_FOUND));

        member.updateRefreshToken(refreshToken);

        return refreshToken;
    }

    private Claims getClaims(String accessToken) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(accessToken)
                .getBody();
    }
}

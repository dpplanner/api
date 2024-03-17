package com.dp.dplanner.config.security;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MemberRepository;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final String secret = "ZHBsYW5uZXI=";
    private static final Long ACCESS_TOKEN_VALID_TIME = 30 * 60 * 1000L * 100;            // 3000 min
    private static final Long REFRESH_TOKEN_VALID_TIME = 3 * 30 * 24 * 30 * 60 * 1000L; // 3 month
    private final MemberRepository memberRepository;
    private final ClubMemberRepository clubMemberRepository;



    public boolean verify(String token) {
        try {
            Claims claims = getClaims(token);
            return !isExpired(claims);
        } catch (ExpiredJwtException e) {
            log.info("토큰 유효기간 만료, {}" , token);
        } catch (JwtException e) {
            log.info("올바르지 않은 토큰, {}", token);
        }
        return false;
    }

    public Authentication getAuthentication(String accessToken) {
        Claims claims = getClaims(accessToken);
        Long id = Long.valueOf(claims.getSubject());

        Long recentClubId = (claims.get("recent_club_id") != null) ? (long) (int) claims.get("recent_club_id") : null;
        Long clubMemberId =(claims.get("club_member_id") != null) ? (long) (int) claims.get("club_member_id") : null;


         // ToDO 추후에 authorities 일반 유저인지 개발자 유저인지 구분 필요
        PrincipalDetails principal = new PrincipalDetails(id,recentClubId,clubMemberId,"", null);

        return new UsernamePasswordAuthenticationToken(principal, "",null); // UsernamePasswordAuthenticationToken  생성자가 authorities 에 따라 다름.  setAuthenticated 차이가 있음.
    }

    public String generateAccessToken(Authentication authentication) {
        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Claims claims = Jwts.claims().setSubject(principal.getName());

        Member member = memberRepository.findById(principal.getId()).orElseThrow(() -> new ServiceException(ErrorResult.MEMBER_NOT_FOUND));
        Club recentClub = member.getRecentClub();
        Optional<ClubMember> optionalClubMember;

        if (recentClub != null) {
            optionalClubMember = clubMemberRepository.findByClubIdAndMemberId(recentClub.getId(), member.getId());
            claims.put("recent_club_id", optionalClubMember.map(ClubMember::getClub).map(Club::getId).orElse(null));
            claims.put("club_member_id", optionalClubMember.map(ClubMember::getId).orElse(null));
        } else {
            claims.put("recent_club_id", null);
            claims.put("club_member_id", null);
        }

        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuer("dplanner")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_VALID_TIME))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    @Transactional
    public String generateRefreshToken(Authentication authentication) {
        Date now = new Date();

        String refreshToken = Jwts.builder()
                .setIssuer("dplanner")
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_VALID_TIME))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

        PrincipalDetails principal = (PrincipalDetails) authentication.getPrincipal();
        Long id = Long.valueOf(principal.getName());
        memberRepository.updateRefreshToken(id, refreshToken);

        return refreshToken;
    }

    private static boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private Claims getClaims(String accessToken) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(accessToken)
                .getBody();
    }
}

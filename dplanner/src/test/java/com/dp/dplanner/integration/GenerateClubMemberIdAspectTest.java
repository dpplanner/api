package com.dp.dplanner.integration;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.security.config.CustomOAuth2UserService;
import jakarta.persistence.EntityManager;
import org.aopalliance.aop.AspectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
public class GenerateClubMemberIdAspectTest {


    @Autowired
    TestController targetClass;
    @Autowired
    CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    EntityManager entityManager;


    Member member;
    Club club;
    ClubMember clubMember;
    Long memberId;

    @BeforeEach
    public void setUp() {
        member = Member.builder().email("test").build();
        club = Club.builder().build();
        clubMember = ClubMember.builder().club(club).member(member).build();

        entityManager.persist(club);
        entityManager.persist(member);
        entityManager.persist(clubMember);

        memberId = member.getId();

        PrincipalDetails principalDetails = PrincipalDetails.create(member, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principalDetails, principalDetails.getPassword(), principalDetails.getAuthorities()));

    }

    @Test
    public void GeneratedClubMemberId() {

        Long clubMemberId = targetClass.targetMethod(null, club.getId());
        assertThat(clubMemberId).isNotNull();
        assertThat(clubMemberId).isGreaterThan(0);
        assertThat(clubMemberId).isEqualTo(clubMember.getId());
    }

    @Test
    public void GeneratedClubMemberId_Throw_Exception_ClubMemberCanNotFound() {
        Long wrongClubId = 100L;
        ClubMemberException clubMemberException = assertThrows(ClubMemberException.class, () -> targetClass.targetMethod(null,wrongClubId));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(CLUBMEMBER_NOT_FOUND);
    }

    @Test
    public void GeneratedClubMemberId_Throw_AspectException() throws Exception {
        AspectException aspectException = assertThrows(AspectException.class, () -> targetClass.wrongMethod(null));

        assertThat(aspectException.getMessage()).isEqualTo("메서드 인자에서 clubId를 찾을 수 없습니다.");
    }
    @Test
    public void GeneratedClubMemberId_Throw_AspectException2() throws Exception {
        AspectException aspectException = assertThrows(AspectException.class, () -> targetClass.wrongMethod2(null, club.getId()));

        assertThat(aspectException.getMessage()).isEqualTo("메서드 인자에서 clubMemberId를 찾을 수 없습니다.");
    }


}


@Component
class TestController {

    @Autowired
    TestService testService;

    public Long targetMethod(@GeneratedClubMemberId Long clubMemberId,Long clubId) {
        return testService.testMethod(clubMemberId);
    }

    public Long wrongMethod(@GeneratedClubMemberId Long clubMemberId) {
        return testService.testMethod(clubMemberId);
    }

    public Long wrongMethod2(@GeneratedClubMemberId Long Id,Long clubId) {
        return testService.testMethod(Id);
    }
}

@Component
class TestService{
    public Long testMethod(Long clubMemberId) {
        return clubMemberId;
    }
}




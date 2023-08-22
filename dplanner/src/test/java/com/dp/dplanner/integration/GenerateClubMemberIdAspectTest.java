package com.dp.dplanner.integration;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.security.PrincipalDetails;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
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
    UserDetailsService userDetailsService;

    @Autowired
    EntityManager entityManager;

    Member member;
    Club club;
    ClubMember clubMember;
    Long memberId;

    @BeforeEach
    public void setUp() {
        member = Member.builder().userName("test").build();
        club = Club.builder().build();
        clubMember = ClubMember.builder().club(club).member(member).build();

        entityManager.persist(club);
        entityManager.persist(member);
        entityManager.persist(clubMember);

        memberId = member.getId();


    }
    @Test
    @WithUserDetails(value = "test",setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void GeneratedClubMemberId(){

        Long clubMemberId = targetClass.targetMethod( null,club.getId());
        assertThat(clubMemberId).isNotNull();
        assertThat(clubMemberId).isGreaterThan(0);
        assertThat(clubMemberId).isEqualTo(clubMember.getId());
    }

    @Test
    @WithUserDetails(value = "test",setupBefore = TestExecutionEvent.TEST_EXECUTION)
    public void GeneratedClubMemberId_Throw_Exception_ClubMemberCanNotFound(){
        Long wrongClubId = 100L;
        ClubMemberException clubMemberException = assertThrows(ClubMemberException.class, () -> targetClass.targetMethod(wrongClubId, null));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    @Test
    public void UserDetailsService_Throw_Exception_MemberCanNotFound(){
        MemberException memberException = assertThrows(MemberException.class, () -> userDetailsService.loadUserByUsername("UserCanNotFound"));
        assertThat(memberException.getErrorResult()).isEqualTo(MEMBER_NOT_FOUND);
    }


    @Test
    public void UserDetailsService_ReturnUserDetails(){
        UserDetails userDetails = userDetailsService.loadUserByUsername("test");
        PrincipalDetails principalDetails = (PrincipalDetails) userDetails;

        Member foundMember = principalDetails.getMember();

        assertThat(foundMember).isNotNull();
        assertThat(foundMember).isEqualTo(member);
        assertThat(foundMember.getId()).isGreaterThan(0);

    }




}

@Component
class TestController {

    @Autowired
    TestService testService;

    public Long targetMethod(@GeneratedClubMemberId Long clubMemberId,Long clubId) {
        return testService.testMethod(clubMemberId);
    }
}

@Component
class TestService{
    public Long testMethod(Long clubMemberId) {
        return clubMemberId;
    }
}




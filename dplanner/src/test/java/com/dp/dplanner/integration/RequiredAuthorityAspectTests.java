package com.dp.dplanner.integration;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RequiredAuthorityAspectTests {

    @Autowired
    TestAopTargetClass targetClass; //RequiredAuthority 어노테이션이 붙은 메소드를 가진 클래스를 주입

    @Autowired
    EntityManager entityManager;


    Member member;
    Club club;
    @BeforeEach
    void setUp() {
        member = Member.builder().build();
        entityManager.persist(member);

        club = Club.builder().build();
        entityManager.persist(club);
    }

    @Test
    @DisplayName("관리자가 요청하면 예외가 발생하지 않음")
    public void requestByAdmin() throws Exception {
        //given
        ClubMember admin = ClubMember.builder().club(club).member(member).build();
        admin.setAdmin();
        entityManager.persist(admin);

        assert admin.checkRoleIs(ClubRole.ADMIN);

        //when
        //then
        assertDoesNotThrow(() -> targetClass.targetMethod(admin.getId()));
    }

    @Test
    @DisplayName("권한이 있는 매니저가 요청하면 예외가 발생하지 않음")
    public void requestByAuthorizedManager() throws Exception {
        //given
        ClubAuthority.createAuthorities(club, List.of(ClubAuthorityType.MEMBER_ALL));

        ClubMember manager = ClubMember.builder().club(club).member(member).build();
        manager.setManager();
        entityManager.persist(manager);

        assert manager.checkRoleIs(ClubRole.MANAGER)
                && manager.getClub().hasAuthority(ClubAuthorityType.MEMBER_ALL);

        //when
        //then
        assertDoesNotThrow(() -> targetClass.targetMethod(manager.getId()));
    }

    @Test
    @DisplayName("권한이 없는 매니저가 요청하면 IllegalStateException")
    public void requestByUnauthorizedManagerThenException() throws Exception {
        //given
        ClubMember manager = ClubMember.builder().club(club).member(member).build();
        manager.setManager();
        entityManager.persist(manager);

        assert manager.checkRoleIs(ClubRole.MANAGER)
                && !manager.getClub().hasAuthority(ClubAuthorityType.MEMBER_ALL);

        //when
        //then
        assertThatThrownBy(() -> targetClass.targetMethod(manager.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("일반 회원이 요청하면 IllegalStateException")
    public void requestByUserThenException() throws Exception {
        //given
        ClubMember clubMember = ClubMember.builder().club(club).member(member).build();
        entityManager.persist(clubMember);

        assert clubMember.getRole().equals(ClubRole.USER);

        //when
        //then
        assertThatThrownBy(() -> targetClass.targetMethod(clubMember.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}

/**
 * 테스트 대상 클래스
 */
@Component
class TestAopTargetClass {
    @RequiredAuthority(ClubAuthorityType.MEMBER_ALL)
    public void targetMethod(Long clubMemberId) throws IllegalStateException{
    }
}

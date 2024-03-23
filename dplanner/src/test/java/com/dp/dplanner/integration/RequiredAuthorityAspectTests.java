package com.dp.dplanner.integration;

import com.dp.dplanner.TestConfig;
import com.dp.dplanner.service.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.service.exception.ServiceException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import({TestConfig.class})
@Transactional
public class RequiredAuthorityAspectTests {

    @Autowired
    TestAopTargetClass targetClass; //RequiredAuthority 어노테이션이 붙은 메소드를 가진 클래스를 주입

    @Autowired
    TestAopTargetClass2 targetClass2; //RequiredAuthority 어노테이션이 붙은 메소드를 가진 클래스를 주입

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
        ClubAuthority clubAuthority = createClubAuthority(club, "name", "description", List.of(ClubAuthorityType.MEMBER_ALL));

        ClubMember manager = ClubMember.builder().club(club).member(member).build();
        manager.setManager();
        manager.updateClubAuthority(clubAuthority);

        entityManager.persist(clubAuthority);
        entityManager.persist(manager);

        assert manager.checkRoleIs(ClubRole.MANAGER)
                && manager.hasAuthority(ClubAuthorityType.MEMBER_ALL);

        //when
        //then
        assertDoesNotThrow(() -> targetClass.targetMethod(manager.getId()));
    }

    @Test
    @DisplayName("권한이 없는 매니저가 요청하면 ServiceException")
    public void requestByUnauthorizedManagerThenException() throws Exception {
        //given
        ClubMember manager = ClubMember.builder().club(club).member(member).build();
        manager.setManager();
        entityManager.persist(manager);

        assert manager.checkRoleIs(ClubRole.MANAGER)
                && !manager.hasAuthority(ClubAuthorityType.MEMBER_ALL);

        //when
        //then
        assertThatThrownBy(() -> targetClass.targetMethod(manager.getId()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("일반 회원이 요청하면 ServiceException")
    public void requestByUserThenException() throws Exception {
        //given
        ClubMember clubMember = ClubMember.builder().club(club).member(member).build();
        entityManager.persist(clubMember);

        assert clubMember.getRole().equals(ClubRole.USER);

        //when
        //then
        assertThatThrownBy(() -> targetClass.targetMethod(clubMember.getId()))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("관리자 역할만 접근할 수 있음")
    public void requestRoleByAdmin() throws Exception
    {
        //given
        ClubMember admin = ClubMember.builder().club(club).member(member).build();
        admin.setAdmin();
        entityManager.persist(admin);

        assert admin.getRole().equals(ClubRole.ADMIN);

        //when
        //then
        assertDoesNotThrow(() -> targetClass2.targetMethod(admin.getId()));

    }

    @Test
    @DisplayName("관리자 역할이 아니면 ServiceException")
    public void requestRoleByManagerUser() throws Exception
    {
        //given
        Member member2 = Member.builder().build();
        entityManager.persist(member2);

        ClubMember clubMember = ClubMember.builder().club(club).member(member2).build();
        entityManager.persist(clubMember);

        ClubMember manager = ClubMember.builder().club(club).member(member).build();
        manager.setManager();
        entityManager.persist(manager);

        assert clubMember.getRole().equals(ClubRole.USER);
        assert manager.getRole().equals(ClubRole.MANAGER);
        assert !clubMember.getId().equals(manager.getId());
        //when
        //then
        assertThatThrownBy(() -> targetClass2.targetMethod(clubMember.getId()))
                .isInstanceOf(ServiceException.class);

        assertThatThrownBy(() -> targetClass2.targetMethod(manager.getId()))
                .isInstanceOf(ServiceException.class);
    }

    private static ClubAuthority createClubAuthority(Club club, String name, String description, List<ClubAuthorityType> clubAuthorityTypes) {

        return ClubAuthority.builder()
                .club(club)
                .clubAuthorityTypes(clubAuthorityTypes)
                .name(name)
                .description(description)
                .build();

    }
}

/**
 * 권한 검증 테스트 대상 클래스
 */
@Component
class TestAopTargetClass {
    @RequiredAuthority(authority = ClubAuthorityType.MEMBER_ALL)
    public void targetMethod(Long clubMemberId) throws IllegalStateException {

    }
}


/**
 * 역할 검증 테스트 대상 클래스
 */
@Component
class TestAopTargetClass2 {
    @RequiredAuthority(role = ClubRole.ADMIN)
    public void targetMethod(Long clubMemberId) throws IllegalStateException {

    }
}

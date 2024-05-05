package com.dp.dplanner.service.aop.aspect;

import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.service.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubRole;
import com.dp.dplanner.service.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import static com.dp.dplanner.domain.club.ClubRole.ADMIN;
import static com.dp.dplanner.exception.ErrorResult.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequiredAuthorityAspect {

    private final ClubMemberRepository clubMemberRepository;

    @Before("@annotation(com.dp.dplanner.service.aop.annotation.RequiredAuthority) " +
            "&& @annotation(requiredAuthority) " +
            "&& args(clubMemberId, ..)")
    public void checkAuthority(Long clubMemberId, RequiredAuthority requiredAuthority) throws Throwable {
        if(!requiredAuthority.authority().equals(ClubAuthorityType.NONE) ){
            if (!hasAuthority(clubMemberId, requiredAuthority.authority())) {
                throw new ServiceException(AUTHORIZATION_DENIED);
            }
        } else if (!requiredAuthority.role().equals(ClubRole.NONE)) {
            if (!hasRole(clubMemberId, requiredAuthority.role())) {
                throw new ServiceException(AUTHORIZATION_DENIED);
            }
        }
    }

    private boolean hasAuthority(Long clubMemberId, ClubAuthorityType authority) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        return clubMember.hasAuthority(authority);
    }

    private boolean hasRole(Long clubMemberId, ClubRole role) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        return clubMember.checkRoleIs(ADMIN) || clubMember.checkRoleIs(role);
    }

}

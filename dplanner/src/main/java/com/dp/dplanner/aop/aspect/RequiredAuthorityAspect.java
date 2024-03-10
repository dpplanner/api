package com.dp.dplanner.aop.aspect;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubRole;
import com.dp.dplanner.exception.ServiceException;
import com.dp.dplanner.service.ClubMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import static com.dp.dplanner.exception.ErrorResult.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequiredAuthorityAspect {

    private final ClubMemberService clubMemberService;

    @Before("@annotation(com.dp.dplanner.aop.annotation.RequiredAuthority) " +
            "&& @annotation(requiredAuthority) " +
            "&& args(clubMemberId, ..)")
    public void checkAuthority(Long clubMemberId, RequiredAuthority requiredAuthority) throws Throwable {
        if(!requiredAuthority.authority().equals(ClubAuthorityType.NONE) ){
            if (!clubMemberService.hasAuthority(clubMemberId, requiredAuthority.authority())) {
                throw new ServiceException(AUTHORIZATION_DENIED);
            }
        } else if (!requiredAuthority.role().equals(ClubRole.NONE)) {
            if (!clubMemberService.hasRole(clubMemberId, requiredAuthority.role())) {
                throw new ServiceException(AUTHORIZATION_DENIED);
            }
        }
    }
}

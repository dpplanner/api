package com.dp.dplanner.aop.aspect;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.service.ClubMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

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
        if (!clubMemberService.hasAuthority(clubMemberId, requiredAuthority.value())) {
            throw new IllegalStateException();
        }
    }
}

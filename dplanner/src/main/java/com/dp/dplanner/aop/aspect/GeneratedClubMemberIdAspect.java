package com.dp.dplanner.aop.aspect;

import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.AspectException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import static com.dp.dplanner.exception.ErrorResult.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class GeneratedClubMemberIdAspect {

    @Autowired
    private final ClubMemberRepository clubMemberRepository;

    @Around("execution(* *(.., @com.dp.dplanner.aop.annotation.GeneratedClubMemberId (*), ..))")
    public Object generateClubMemberId(ProceedingJoinPoint joinPoint) throws Throwable {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        Object[] parameterValues = joinPoint.getArgs();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        List<String> parameterNames = Arrays.stream(method.getParameters()).map(Parameter::getName).toList();

        Long clubId = null;
        Integer index = null;

        for (int i = 0; i < parameterNames.size(); i++) {
            String parameterName = parameterNames.get(i);
            if (parameterName.equals("clubId")) {
                clubId = (Long) parameterValues[i];
            } else if (parameterName.equals("clubMemberId")) {
                index = i;
            }
        }
        if (clubId == null) {
            throw new AspectException("메서드 인자에서 clubId를 찾을 수 없습니다.");
        }

        if (index == null) {
            throw new AspectException("메서드 인자에서 clubMemberId를 찾을 수 없습니다.");
        }

        ClubMember clubMember = clubMemberRepository.findByClubIdAndMemberId(clubId, principalDetails.getId())
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isConfirmed()) {
            throw new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED);
        }

        parameterValues[index] = clubMember.getId();

        return joinPoint.proceed(parameterValues);
    }

}

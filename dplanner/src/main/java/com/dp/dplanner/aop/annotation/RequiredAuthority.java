package com.dp.dplanner.aop.annotation;


import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 어노테이션 사용시 검증하고자 하는 clubMemberId가 첫 번째 인자로 와야함
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredAuthority {

    ClubRole role() default ClubRole.NONE;

    ClubAuthorityType authority() default ClubAuthorityType.NONE;
    

}

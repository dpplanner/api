package com.dp.dplanner.aop.annotation;


import com.dp.dplanner.domain.club.ClubAuthorityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredAuthority {
    ClubAuthorityType value();
}

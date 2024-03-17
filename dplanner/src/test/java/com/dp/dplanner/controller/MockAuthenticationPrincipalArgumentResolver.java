package com.dp.dplanner.controller;

import com.dp.dplanner.config.security.PrincipalDetails;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class MockAuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
    private Long memberId;
    private Long clubId;
    private Long clubMemberId;

    public MockAuthenticationPrincipalArgumentResolver(Long memberId, Long clubId, Long clubMemberId) {
        this.memberId = memberId;
        this.clubId = clubId;
        this.clubMemberId = clubMemberId;
    }
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(PrincipalDetails.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return new PrincipalDetails(memberId, clubId, clubMemberId, "email", null);
    }
}

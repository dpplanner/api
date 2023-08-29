package com.dp.dplanner.controller;

import com.dp.dplanner.aop.aspect.GeneratedClubMemberIdAspect;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.GlobalExceptionHandler;
import com.dp.dplanner.service.ClubMemberService;
import com.dp.dplanner.service.ClubService;
import com.nimbusds.jose.shaded.gson.Gson;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ClubMemberControllerTests {

    @InjectMocks
    ClubController proxy;
    @Mock
    ClubMemberService clubMemberService;
    @Mock
    GeneratedClubMemberIdAspect aspect;

    MockMvc mockMvc;
    Gson gson;

    @BeforeEach
    void setUp() {
        ClubMemberController target = new ClubMemberController(clubMemberService);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        proxy = factory.getProxy();

        gson = new Gson();

        mockMvc = MockMvcBuilders
                .standaloneSetup(proxy)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("내 클럽 회원 전체 조회시 200 OK")
    public void findMyClubMembers_OK() throws Exception {
        //given
        
        //when
        
        //then
    }
    
    /**
     * utility methods
     */
    private <T> T getResponse(ResultActions resultActions, Class<T> responseType) throws UnsupportedEncodingException {
        return gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), responseType);
    }

    private void answerClubMemberId() throws Throwable {
        Long clubMemberId = 1L;
        given(aspect.generateClubMemberId(any(ProceedingJoinPoint.class)))
                .willAnswer(invocation -> {
                    ProceedingJoinPoint joinPoint = invocation.getArgument(0);
                    Object[] args = joinPoint.getArgs();
                    args[0] = clubMemberId;
                    return joinPoint.proceed(args);
                } );
    }
}

package com.dp.dplanner.controller;


import com.dp.dplanner.aop.aspect.GeneratedClubMemberIdAspect;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.InviteDto;
import com.dp.dplanner.exception.ClubException;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.GlobalExceptionHandler;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ClubService;
import com.nimbusds.jose.shaded.gson.Gson;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ClubControllerTest {

    @InjectMocks
    ClubController proxy;
    @Mock
    ClubService clubService;
    @Mock
    GeneratedClubMemberIdAspect aspect;

    MockMvc mockMvc;
    Gson gson;

    @BeforeEach
    void setUp() {
        ClubController target = new ClubController(clubService);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        proxy = factory.getProxy();

        gson = new Gson();

        mockMvc = MockMvcBuilders
                .standaloneSetup(proxy)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("클럽이 정상적으로 생성되면 201 CREATED")
    public void createClub_OK() throws Exception {
        //given
        ClubDto.Response responseDto = ClubDto.Response.builder()
                .id(1L)
                .clubName("club")
                .info("info")
                .build();
        given(clubService.createClub(any(Long.class), any(ClubDto.Create.class))).willReturn(responseDto);

        //when
        ClubDto.Create createDto = ClubDto.Create.builder()
                .clubName("club")
                .info("info")
                .build();

        ResultActions resultActions = mockMvc.perform(post("/clubs")
                .content(gson.toJson(createDto))
                .contentType(APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isCreated());

        ClubDto.Response response = getResponse(resultActions, ClubDto.Response.class);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getClubName()).isEqualTo("club");
        assertThat(response.getInfo()).isEqualTo("info");
    }

    @Test
    @DisplayName("클럽 생성시 회원 정보가 없으면 404 NOT_FOUND")
    public void createClub_NOTFOUND() throws Exception {
        //given
        given(clubService.createClub(any(Long.class), any(ClubDto.Create.class)))
                .willThrow(new MemberException(MEMBER_NOT_FOUND));

        //when
        ClubDto.Create createDto = ClubDto.Create.builder()
                .clubName("club")
                .info("info")
                .build();

        ResultActions resultActions = mockMvc.perform(post("/clubs")
                .content(gson.toJson(createDto))
                .contentType(APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("클럽 조회에 성공하면 200 OK")
    public void findClubById_OK() throws Exception {
        //given
        Long clubId = 1L;
        ClubDto.Response responseDto = ClubDto.Response.builder()
                .id(1L)
                .clubName("club")
                .info("info")
                .build();
        given(clubService.findClubById(clubId)).willReturn(responseDto);

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}", clubId));

        //then
        resultActions.andExpect(status().isOk());

        ClubDto.Response response = getResponse(resultActions, ClubDto.Response.class);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getClubName()).isEqualTo("club");
        assertThat(response.getInfo()).isEqualTo("info");
    }

    @Test
    @DisplayName("클럽 조회시 클럽 정보가 없으면 404 NOT_FOUND")
    public void findClubById_NOTFOUND() throws Exception {
        //given
        Long clubId = 1L;
        given(clubService.findClubById(clubId))
                .willThrow(new ClubException(CLUB_NOT_FOUND));

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}", clubId));

        //then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("내가 가입한 클럽들을 조회하면 200 OK")
    public void findMyClubs_OK() throws Exception {
        //given
        Long memberId = 1L;
        int responseNum = 3;
        List<ClubDto.Response> responseList = new ArrayList<>();
        for (int i = 0; i < responseNum; i++) {
            ClubDto.Response response = ClubDto.Response.of(Club.builder().clubName("club" + i).build());
            responseList.add(response);
        }
        given(clubService.findMyClubs(memberId)).willReturn(responseList);

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs"));

        //then
        resultActions.andExpect(status().isOk());

        List<ClubDto.Response> responses = getResponse(resultActions, List.class);
        assertThat(responses.size()).isEqualTo(responseNum);
    }

    @Test
    @DisplayName("가입한 클럽 조회시 회원정보가 없으면 404 NOT_FOUND")
    public void findMyClubs_NOTFOUND() throws Exception {
        //given
        Long memberId = 1L;
        given(clubService.findMyClubs(memberId)).willThrow(new MemberException(MEMBER_NOT_FOUND));

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs"));

        //then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("클럽 정보 수정시 200 OK")
    public void updateClubInfo_OK() throws Throwable {
        //given
        Long clubId = 1L;
        answerClubMemberId();
        ClubDto.Response responseDto = ClubDto.Response.builder()
                .id(clubId)
                .clubName("club")
                .info("updatedInfo")
                .build();
        given(clubService.updateClubInfo(any(Long.class), any(ClubDto.Update.class))).willReturn(responseDto);

        //when
        ClubDto.Update updateDto = ClubDto.Update.builder()
                .clubId(clubId)
                .info("updatedInfo")
                .build();

        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}", clubId)
                .content(gson.toJson(updateDto))
                .contentType(APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isOk());

        ClubDto.Response response = getResponse(resultActions, ClubDto.Response.class);

        assertThat(response.getInfo()).isEqualTo("updatedInfo");
    }

    @Test
    @DisplayName("클럽 정보 수정시 수정 권한이 없으면 403 FORBIDDEN")
    public void updateClubInfo_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        answerClubMemberId();
        given(clubService.updateClubInfo(any(Long.class), any(ClubDto.Update.class)))
                .willThrow(new ClubException(UPDATE_AUTHORIZATION_DENIED));

        //when
        ClubDto.Update updateDto = ClubDto.Update.builder()
                .clubId(clubId)
                .info("updatedInfo")
                .build();

        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}", clubId)
                .content(gson.toJson(updateDto))
                .contentType(APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("클럽 정보 수정시 클럽회원 데이터가 없으면 404 NOT_FOUND")
    public void updateClubInfo_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        answerClubMemberId();
        given(clubService.updateClubInfo(any(Long.class), any(ClubDto.Update.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        //when
        ClubDto.Update updateDto = ClubDto.Update.builder()
                .clubId(clubId)
                .info("updatedInfo")
                .build();

        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}", clubId)
                .content(gson.toJson(updateDto))
                .contentType(APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("클럽 초대코드 조회시 200 OK")
    public void inviteClub_OK() throws Throwable {
        //given
        Long clubId = 1L;
        answerClubMemberId();
        given(clubService.inviteClub(any(Long.class)))
                .willReturn(InviteDto.builder()
                        .clubId(clubId)
                        .inviteCode("inviteCode")
                        .build());

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/invite", clubId));

        //then
        resultActions.andExpect(status().isOk());

        InviteDto response = getResponse(resultActions, InviteDto.class);

        assertThat(response.getClubId()).isNotNull();
        assertThat(response.getInviteCode()).isEqualTo("inviteCode");
    }

    @Test
    @DisplayName("클럽 초대코드 조회시 초대권한이 없으면 403 FORBIDDEN")
    public void inviteClub_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        answerClubMemberId();
        given(clubService.inviteClub(any(Long.class))).willThrow(new ClubMemberException(AUTHORIZATION_DENIED));

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/invite", clubId));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("클럽 초대코드 조회시 클럽 회원 데이터가 없으면 404 NOT_FOUND")
    public void inviteClub_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        answerClubMemberId();
        given(clubService.inviteClub(any(Long.class))).willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/invite", clubId));

        //then
        resultActions.andExpect(status().isNotFound());

    }

    @Test
    @DisplayName("클럽 가입시 201 CREATED")
    public void joinClub_CREATED() throws Exception {
        //given
        Long clubId = 1L;
        Long clubMemberId = 2L;
        ClubMember clubMember = ClubMember.createClubMember(Member.builder().build(), Club.builder().build());

        ClubMemberDto.Response responseDto = ClubMemberDto.Response.of(clubMember);
        responseDto.setId(clubMemberId);
        given(clubService.joinClub(any(Long.class), any(InviteDto.class))).willReturn(responseDto);

        //when
        InviteDto inviteDto = InviteDto.builder().clubId(clubId).inviteCode("inviteCode").build();
        ResultActions resultActions = mockMvc.perform(post("/clubs/{clubId}/join", clubId)
                .content(gson.toJson(inviteDto))
                .contentType(APPLICATION_JSON)
        );

        //then
        resultActions.andExpect(status().isCreated());

        ClubMemberDto.Response response = getResponse(resultActions, ClubMemberDto.Response.class);
        assertThat(response.getId()).isEqualTo(clubMemberId);
    }

    @Test
    @DisplayName("클럽 가입시 클럽 정보가 없으면 404 NOT_FOUND")
    public void joinClub_NOTFOUND() throws Exception {
        //given
        Long clubId = 1L;
        given(clubService.joinClub(any(Long.class), any(InviteDto.class))).willThrow(new ClubException(CLUB_NOT_FOUND));

        //when
        InviteDto inviteDto = InviteDto.builder().clubId(clubId).inviteCode("inviteCode").build();
        ResultActions resultActions = mockMvc.perform(post("/clubs/{clubId}/join", clubId)
                .content(gson.toJson(inviteDto))
                .contentType(APPLICATION_JSON)
        );

        //then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("클럽 가입시 초대 코드가 다르면 400 BAD_REQUEST")
    public void joinClub_BADREQUEST() throws Exception {
        //given
        Long clubId = 1L;
        given(clubService.joinClub(any(Long.class), any(InviteDto.class))).willThrow(new ClubException(WRONG_INVITE_CODE));

        //when
        InviteDto inviteDto = InviteDto.builder().clubId(clubId).inviteCode("inviteCode").build();
        ResultActions resultActions = mockMvc.perform(post("/clubs/{clubId}/join", clubId)
                .content(gson.toJson(inviteDto))
                .contentType(APPLICATION_JSON)
        );

        //then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("매니저 권한 조회시 200 OK")
    public void findManagerAuthorities_OK() throws Throwable {
        //given
        Long clubId = 1L;
        ClubAuthorityDto.Response responseDto = ClubAuthorityDto.Response.builder()
                .clubId(clubId)
                .authorities(List.of(MEMBER_ALL.name(), SCHEDULE_ALL.name()))
                .build();
        given(clubService.findClubManagerAuthorities(any(Long.class), any(ClubAuthorityDto.Request.class)))
                .willReturn(responseDto);
        answerClubMemberId();

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/authorities", clubId));

        //then
        resultActions.andExpect(status().isOk());
        ClubAuthorityDto.Response response = getResponse(resultActions, ClubAuthorityDto.Response.class);

        assertThat(response.getClubId()).isEqualTo(clubId);
        assertThat(response.getAuthorities()).containsExactly(MEMBER_ALL.name(), SCHEDULE_ALL.name());
    }

    @Test
    @DisplayName("매니저 권한 조회시 권한이 없으면 403 FORBIDDEN")
    public void findManagerAuthorities_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        given(clubService.findClubManagerAuthorities(any(Long.class), any(ClubAuthorityDto.Request.class)))
                .willThrow(new ClubException(READ_AUTHORIZATION_DENIED));
        answerClubMemberId();

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/authorities", clubId));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("매니저 권한 조회시 클럽 회원 데이터가 없으면 404 NOT_FOUND")
    public void findManagerAuthorities_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        given(clubService.findClubManagerAuthorities(any(Long.class), any(ClubAuthorityDto.Request.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));
        answerClubMemberId();

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/authorities", clubId));

        //then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("매니저 권한 설정시 200 OK")
    public void setManagerAuthorities_OK() throws Throwable {
        //given
        Long clubId = 1L;
        ClubAuthorityDto.Response responseDto = ClubAuthorityDto.Response.builder()
                .clubId(clubId)
                .authorities(List.of(MEMBER_ALL.name(), SCHEDULE_ALL.name()))
                .build();
        given(clubService.setManagerAuthority(any(Long.class), any(ClubAuthorityDto.Update.class)))
                .willReturn(responseDto);
        answerClubMemberId();

        //when
        ClubAuthorityDto.Update updateDto = ClubAuthorityDto.Update.builder()
                .clubId(clubId)
                .authorities(List.of(MEMBER_ALL.name(), SCHEDULE_ALL.name()))
                .build();
        ResultActions resultActions = mockMvc.perform(put("/clubs/{clubId}/authorities", clubId)
                .content(gson.toJson(updateDto))
                .contentType(APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isOk());
        ClubAuthorityDto.Response response = getResponse(resultActions, ClubAuthorityDto.Response.class);

        assertThat(response.getClubId()).isEqualTo(clubId);
        assertThat(response.getAuthorities()).containsExactly(MEMBER_ALL.name(), SCHEDULE_ALL.name());
    }

    @Test
    @DisplayName("매니저 권한 설정시 수정 권한이 없으면 403 FORBIDDEN")
    public void setManagerAuthorities_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        given(clubService.setManagerAuthority(any(Long.class), any(ClubAuthorityDto.Update.class)))
                .willThrow(new ClubException(UPDATE_AUTHORIZATION_DENIED));
        answerClubMemberId();

        //when
        ClubAuthorityDto.Update updateDto = ClubAuthorityDto.Update.builder()
                .clubId(clubId)
                .authorities(List.of(MEMBER_ALL.name(), SCHEDULE_ALL.name()))
                .build();
        ResultActions resultActions = mockMvc.perform(put("/clubs/{clubId}/authorities", clubId)
                .content(gson.toJson(updateDto))
                .contentType(APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("매니저 권한 설정시 클럽 회원 데이터가 없으면 404 NOT_FOUND")
    public void setManagerAuthorities_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        given(clubService.setManagerAuthority(any(Long.class), any(ClubAuthorityDto.Update.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));
        answerClubMemberId();

        //when
        ClubAuthorityDto.Update updateDto = ClubAuthorityDto.Update.builder()
                .clubId(clubId)
                .authorities(List.of(MEMBER_ALL.name(), SCHEDULE_ALL.name()))
                .build();
        ResultActions resultActions = mockMvc.perform(put("/clubs/{clubId}/authorities", clubId)
                .content(gson.toJson(updateDto))
                .contentType(APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isNotFound());
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

    static class MockAuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().isAssignableFrom(PrincipalDetails.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
            return new PrincipalDetails(1L, "email", null);
        }
    }

}

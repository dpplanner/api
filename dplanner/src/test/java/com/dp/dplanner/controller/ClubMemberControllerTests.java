package com.dp.dplanner.controller;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.GlobalExceptionHandler;
import com.dp.dplanner.service.ClubMemberService;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ClubMemberControllerTests {

    @InjectMocks
    ClubMemberController target;
    @Mock
    ClubMemberService clubMemberService;

    MockMvc mockMvc;
    Gson gson;

    @BeforeEach
    void setUp() {
        target = new ClubMemberController(clubMemberService);

        gson = new Gson();

        mockMvc = MockMvcBuilders
                .standaloneSetup(target)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver(1L, 1L, 1L))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    /**
     * GET /clubs/{clubId}/club-members
     */
    @Test
    @DisplayName("내 클럽 회원 전체 조회시 200 OK")
    public void findMyClubMembers_OK() throws Throwable {
        //given
        Long clubId = 1L;
        Club club = Club.builder().clubName("club").build();

        List<ClubMember> clubMembers = getConfirmedClubMembers(club, 5);

        Member member = Member.builder().name("me").build();
        clubMembers.add(ClubMember.createClubMember(member, club));

        List<ClubMemberDto.Response> responseDto = ClubMemberDto.Response.ofList(clubMembers);

        given(clubMemberService.findMyClubMembers(any(Long.class),any(Long.class))).willReturn(responseDto);

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/club-members", clubId));

        //then
        resultActions.andExpect(status().isOk());

        List<ClubMemberDto.Response> response = getResponse(resultActions, List.class);
        assertThat(response.size()).isEqualTo(6);
    }

    @Test
    @DisplayName("해당 클럽에 가입되지 않았을 때 클럽 회원 전체를 조회하면 404 NOT_FOUND")
    public void findMyClubMembers_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        given(clubMemberService.findMyClubMembers(any(Long.class),any(Long.class))).willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/club-members", clubId));

        //then
        resultActions.andExpect(status().isNotFound());
    }


    /**
     * GET /clubs/{clubId}/club-members?confirmed=
     */
    @Test
    @DisplayName("승인된 회원 조회시 200 OK")
    public void findConfirmedClubMembers_OK() throws Throwable {
        //given
        Long clubId = 1L;

        List<ClubMember> clubMembers = getConfirmedClubMembers(Club.builder().build(), 5);
        List<ClubMemberDto.Response> responseDto = ClubMemberDto.Response.ofList(clubMembers);

        given(clubMemberService.findMyClubMembers(any(Long.class),any(Long.class), eq(true))).willReturn(responseDto);

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/club-members", clubId)
                .param("confirmed", "true"));

        //then
        resultActions.andExpect(status().isOk());
        List<ClubMemberDto.Response> response = getResponse(resultActions, List.class);
        assertThat(response.size()).isEqualTo(5);

    }
    @Test
    @DisplayName("승인되지 않은 회원 조회시 200 OK")
    public void findUnconfirmedClubMembers_OK() throws Throwable {
        //given
        Long clubId = 1L;

        List<ClubMember> clubMembers = getUnconfirmedClubMembers(Club.builder().build(), 5);
        List<ClubMemberDto.Response> responseDto = ClubMemberDto.Response.ofList(clubMembers);

        given(clubMemberService.findMyClubMembers(any(Long.class),any(Long.class), eq(false))).willReturn(responseDto);

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/club-members", clubId)
                .param("confirmed", "false"));

        //then
        resultActions.andExpect(status().isOk());
        List response = getResponse(resultActions, List.class);
        assertThat(response.size()).isEqualTo(5);
    }

    @Test
    @DisplayName("승인여부로 필터링하여 회원 조회시 본인의 데이터가 없으면 404 NOT FOUND")
    public void findFilteredMyClubMembers_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;

        given(clubMemberService.findMyClubMembers(any(Long.class),any(Long.class), any(Boolean.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        //when
        ResultActions resultActions = mockMvc.perform(get("/clubs/{clubId}/club-members", clubId)
                .param("confirmed", "false"));

        //then
        resultActions.andExpect(status().isNotFound());
    }


    /**
     * DELETE /clubs/{clubId}/club-members
     */
    @Test
    @DisplayName("클럽 회원 여러명을 퇴출하는 경우 200 OK")
    @Disabled("해당 api 삭제")
    public void deleteClubMembers_OK() throws Throwable {
        //given
        Long clubId = 1L;

        List<ClubMember> notDeletedClubMembers = getConfirmedClubMembers(Club.builder().build(), 2); // 삭제된 회원을 반환
        List<ClubMemberDto.Response> responseDto = ClubMemberDto.Response.ofList(notDeletedClubMembers);
        given(clubMemberService.kickOutAll(any(Long.class), any(Long.class), any(List.class))).willReturn(responseDto);

        //when
        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(List.of(2L, 3L, 4L));
        ResultActions resultActions = mockMvc.perform(delete("/clubs/{clubId}/club-members", clubId)
                .content(gson.toJson(requestDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isOk());

        List<ClubMemberDto.Response> response = getResponse(resultActions, List.class);
        assertThat(response.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("본인의 데이터가 없을 때 클럽 회원 여러명을 퇴출하는 경우 404 NOT FOUND")
    @Disabled("해당 api 삭제")
    public void deleteClubMembers_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;

        given(clubMemberService.kickOutAll(any(Long.class), any(Long.class), any(List.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        //when
        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(List.of(2L, 3L, 4L));
        ResultActions resultActions = mockMvc.perform(delete("/clubs/{clubId}/club-members", clubId)
                .content(gson.toJson(requestDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isNotFound());
    }


    /**
     * PATCH /clubs/{clubId}/club-members/confirm
     */
    @Test
    @DisplayName("클럽 회원 여러명을 승인하면 204 NO CONTENT")
    public void confirmClubMembers_NOCONTENT() throws Throwable {
        //given
        Long clubId = 1L;

        doNothing().when(clubMemberService).confirmAll(any(Long.class), any(List.class));

        //when
        ClubMemberDto.Request requestDto = new ClubMemberDto.Request(2L);
//        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(List.of(2L, 3L, 4L));
        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/confirm", clubId)
                .content(gson.toJson(requestDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("다른 클럽 회원 여러명을 승인하면 403 FORBIDDEN")
    public void confirmClubMembers_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;

        doThrow(new ClubMemberException(DIFFERENT_CLUB_EXCEPTION))
                .when(clubMemberService).confirmAll(any(Long.class), any(List.class));

        //when
        ClubMemberDto.Request requestDto = new ClubMemberDto.Request(2L);
//        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(List.of(2L, 3L, 4L));
        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/confirm", clubId)
                .content(gson.toJson(requestDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("클럽 회원 승인시 본인의 데이터가 없으면 404 NOT FOUND")
    public void confirmClubMembers_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;

        doThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND))
                .when(clubMemberService).confirmAll(any(Long.class), any(List.class));

        //when
        ClubMemberDto.Request requestDto = new ClubMemberDto.Request(2L);
//        List<ClubMemberDto.Request> requestDto = ClubMemberDto.Request.ofList(List.of(2L, 3L, 4L));
        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/confirm", clubId)
                .content(gson.toJson(requestDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isNotFound());
    }


    /**
     * GET /clubs/{clubId}/club-members/{clubMemberId}
     */
    @Test
    @DisplayName("클럽 회원 id로 조회시 200 OK")
    public void findById_OK() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        //when
        ResultActions resultActions = mockMvc.perform(
                get("/clubs/{clubId}/club-members/{clubMemberId}", clubId, clubMemberId));

        //then
        resultActions.andExpect(status().isOk());

    }

    @Test
    @DisplayName("승인되지 않은 클럽 회원을 id로 조회시 401 UNAUTHORIZED")
    public void findById_UNAUTHORIZED() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.findById(any(Long.class), any(ClubMemberDto.Request.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED));

        //when
        ResultActions resultActions = mockMvc.perform(
                get("/clubs/{clubId}/club-members/{clubMemberId}", clubId, clubMemberId));
        //then
        resultActions.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("다른 클럽의 회원을 id로 조회시 403 FORBIDDEN")
    public void findById_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.findById(any(Long.class), any(ClubMemberDto.Request.class)))
                .willThrow(new ClubMemberException(DIFFERENT_CLUB_EXCEPTION));

        //when
        ResultActions resultActions = mockMvc.perform(
                get("/clubs/{clubId}/club-members/{clubMemberId}", clubId, clubMemberId));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("클럽 회원 id로 조회시 데이터가 없으면 404 NOT FOUND")
    public void findById_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.findById(any(Long.class), any(ClubMemberDto.Request.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        //when
        ResultActions resultActions = mockMvc.perform(
                get("/clubs/{clubId}/club-members/{clubMemberId}", clubId, clubMemberId));
        //then
        resultActions.andExpect(status().isNotFound());
    }


    /**
     * PATCH /clubs/{clubId}/club-members/{clubMemberId}
     */
    @Test
    @DisplayName("클럽 회원의 정보를 수정하면 200 OK")
    public void updateClubMember_OK() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        ClubMemberDto.Response responseDto = ClubMemberDto.Response.builder()
                .id(clubMemberId)
                .name("updatedName")
                .info("updatedInfo")
                .isConfirmed(true)
                .role("USER")
                .build();

        given(clubMemberService.update(any(Long.class), any(ClubMemberDto.Update.class))).willReturn(responseDto);

        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder()
                .name("updatedName")
                .info("updatedInfo")
                .build();

        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isOk());
        ClubMemberDto.Response response = getResponse(resultActions, ClubMemberDto.Response.class);

        assertThat(response.getName()).isEqualTo("updatedName");
        assertThat(response.getInfo()).isEqualTo("updatedInfo");
    }

    @Test
    @DisplayName("승인되지 않은 클럽 회원의 정보를 수정하면 401 UNAUTHORIZED")
    public void updateClubMember_UNAUTHORIZED() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.update(any(Long.class), any(ClubMemberDto.Update.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED));

        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder()
                .name("updatedName")
                .info("updatedInfo")
                .build();

        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("다른 클럽 회원의 정보를 수정하면 403 FORBIDDEN")
    public void updateClubMember_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.update(any(Long.class), any(ClubMemberDto.Update.class)))
                .willThrow(new ClubMemberException(UPDATE_AUTHORIZATION_DENIED));

        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder()
                .name("updatedName")
                .info("updatedInfo")
                .build();

        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("클럽 회원 정보 수정시 클럽 회원 데이터가 없으면 404 NOT FOUND")
    public void updateClubMember_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.update(any(Long.class), any(ClubMemberDto.Update.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder()
                .name("updatedName")
                .info("updatedInfo")
                .build();

        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isNotFound());
    }


    /**
     * PATCH /clubs/{clubId}/club-members/{clubMemberId}/role
     */
    @Test
    @DisplayName("클럽 회원 역할 수정시 200 OK")
    public void updateClubMemberRole_OK() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        ClubMemberDto.Response responseDto = ClubMemberDto.Response.builder().role("MANAGER").build();
        given(clubMemberService.updateClubMemberClubAuthority(any(Long.class), any(Long.class), any(ClubMemberDto.Update.class)))
                .willReturn(responseDto);


        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder().role("MANAGER").build();
        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}/role", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isOk());
        ClubMemberDto.Response response = getResponse(resultActions, ClubMemberDto.Response.class);
        assertThat(response.getRole()).isEqualTo("MANAGER");
    }

    @Test
    @DisplayName("승인되지 않은 클럽 회원의 역할을 수정하려 하면 401 UNAUTHORIZED")
    public void updateClubMemberRole_UNAUTHORIZED() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.updateClubMemberClubAuthority(any(Long.class), any(Long.class), any(ClubMemberDto.Update.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED));


        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder().role("MANAGER").build();
        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}/role", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("본인의 역할을 수정하려 하면 403 FORBIDDEN")
    public void updateClubMemberRole_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.updateClubMemberClubAuthority(any(Long.class), any(Long.class), any(ClubMemberDto.Update.class)))
                .willThrow(new ClubMemberException(UPDATE_AUTHORIZATION_DENIED));


        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder().role("MANAGER").build();
        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}/role", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("다른 클럽 회원의 역할을 수정하려 하면 403 FORBIDDEN")
    public void updateClubMemberRole_FORBIDDEN2() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.updateClubMemberClubAuthority(any(Long.class), any(Long.class), any(ClubMemberDto.Update.class)))
                .willThrow(new ClubMemberException(DIFFERENT_CLUB_EXCEPTION));


        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder().role("MANAGER").build();
        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}/role", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("데이터가 없는 클럽 회원의 역할을 수정하려 하면 404 NOT FOUND")
    public void updateClubMemberRole_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        given(clubMemberService.updateClubMemberClubAuthority(any(Long.class), any(Long.class), any(ClubMemberDto.Update.class)))
                .willThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND));


        //when
        ClubMemberDto.Update updateDto = ClubMemberDto.Update.builder().role("MANAGER").build();
        ResultActions resultActions = mockMvc.perform(patch("/clubs/{clubId}/club-members/{clubMemberId}/role", clubId, clubMemberId)
                .content(gson.toJson(updateDto))
                .contentType(MediaType.APPLICATION_JSON));

        //then
        resultActions.andExpect(status().isNotFound());
    }




    /**
     * DELETE /clubs/{clubId}/club-members/{clubMemberId}/leave
     */
    @Test
    @DisplayName("클럽 탈퇴 요청시 204 NO CONTENT")
    public void deleteClubMember_NOCONTENT() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        doNothing().when(clubMemberService).leaveClub(any(Long.class), any(ClubMemberDto.Request.class));

        //when
        ResultActions resultActions = mockMvc.perform(
                delete("/clubs/{clubId}/club-members/{clubMemberId}/leave", clubId, clubMemberId));

        //then
        resultActions.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자인 경우엔 탈퇴 할 수 없음. 클럽 탈퇴 권한이 없으면 403 FORBIDDEN")
    public void deleteClubMember_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        doThrow(new ClubMemberException(DELETE_AUTHORIZATION_DENIED))
                .when(clubMemberService).leaveClub(any(Long.class), any(ClubMemberDto.Request.class));

        //when
        ResultActions resultActions = mockMvc.perform(
                delete("/clubs/{clubId}/club-members/{clubMemberId}/leave", clubId, clubMemberId));

        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("클럽 탈퇴시 클럽 회원 데이터가 없으면 404 NOT FOUND")
    public void deleteClubMember_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        doThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND))
                .when(clubMemberService).leaveClub(any(Long.class), any(ClubMemberDto.Request.class));

        //when
        ResultActions resultActions = mockMvc.perform(
                delete("/clubs/{clubId}/club-members/{clubMemberId}/leave", clubId, clubMemberId));


        //then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("클럽 탈퇴시 클럽 회원 데이터 요청 데이터가 다르면 403")
    public void deleteDifferentClubMember_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        doThrow(new ClubMemberException(DIFFERENT_CLUB_EXCEPTION))
                .when(clubMemberService).leaveClub(any(Long.class), any(ClubMemberDto.Request.class));

        //when
        ResultActions resultActions = mockMvc.perform(
                delete("/clubs/{clubId}/club-members/{clubMemberId}/leave", clubId, clubMemberId));


        //then
        resultActions.andExpect(status().isForbidden());

    }



    /**
     * DELETE /clubs/{clubId}/club-members/{clubMemberId}/kickOut
     */
    @Test
    @DisplayName("클럽 회원을 퇴출하는 경우 204 NO CONTENT")
    public void deleteClubMemberForce_NOCONTENT() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        doNothing().when(clubMemberService).kickOut(any(Long.class), any(Long.class), any(ClubMemberDto.Request.class));

        //when
        ResultActions resultActions = mockMvc.perform(
                delete("/clubs/{clubId}/club-members/{clubMemberId}/kickOut", clubId, clubMemberId));

        //then
        resultActions.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("퇴출 권한이 없는 경우 403 FORBIDDEN")
    public void deleteClubMemberForce_FORBIDDEN() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        doThrow(new ClubMemberException(DELETE_AUTHORIZATION_DENIED))
                .when(clubMemberService).kickOut(any(Long.class), any(Long.class), any(ClubMemberDto.Request.class));

        //when
        ResultActions resultActions = mockMvc.perform(
                delete("/clubs/{clubId}/club-members/{clubMemberId}/kickOut", clubId, clubMemberId));


        //then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("데이터가 없는 클럽 회원을 퇴출하는 경우 404 NOT FOUND")
    public void deleteClubMemberForce_NOTFOUND() throws Throwable {
        //given
        Long clubId = 1L;
        Long clubMemberId = 1L;

        doThrow(new ClubMemberException(CLUBMEMBER_NOT_FOUND))
                .when(clubMemberService).kickOut(any(Long.class), any(Long.class), any(ClubMemberDto.Request.class));

        //when
        ResultActions resultActions = mockMvc.perform(
                delete("/clubs/{clubId}/club-members/{clubMemberId}/kickOut", clubId, clubMemberId));


        //then
        resultActions.andExpect(status().isNotFound());
    }


    /**
     * utility methods
     */
    private <T> T getResponse(ResultActions resultActions, Class<T> responseType) throws UnsupportedEncodingException {
        Type type = TypeToken.getParameterized(CommonResponse.class, responseType).getType();
        return ((CommonResponse<T>) gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), type)).getData();
    }

//    private void answerClubMemberId() throws Throwable {
//        Long clubMemberId = 1L;
//        given(aspect.generateClubMemberId(any(ProceedingJoinPoint.class)))
//                .willAnswer(invocation -> {
//                    ProceedingJoinPoint joinPoint = invocation.getArgument(0);
//                    Object[] args = joinPoint.getArgs();
//                    args[0] = clubMemberId;
//                    return joinPoint.proceed(args);
//                } );
//    }

    private static List<ClubMember> getUnconfirmedClubMembers(Club club, int n) {
        List<ClubMember> clubMembers = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            ClubMember clubMember = ClubMember.createClubMember(Member.builder().build(), club);
            clubMembers.add(clubMember);
        }
        return clubMembers;
    }

    private static List<ClubMember> getConfirmedClubMembers(Club club, int n) {
        List<ClubMember> clubMembers = getUnconfirmedClubMembers(club, n);

        clubMembers.forEach(ClubMember::confirm);

        return clubMembers;
    }

}

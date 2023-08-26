package com.dp.dplanner.controller;

import com.dp.dplanner.aop.aspect.GeneratedClubMemberIdAspect;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.exception.ClubException;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.GlobalExceptionHandler;
import com.dp.dplanner.exception.PostException;
import com.dp.dplanner.service.PostService;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.dp.dplanner.dto.PostDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PostControllerTest {


    @InjectMocks
    private PostController proxy;
    @Mock
    private PostService postService;
    @Mock
    private GeneratedClubMemberIdAspect aspect;

    private MockMvc mockMvc;
    private Gson gson;
    Long postId;
    Long clubId;
    Long memberId;
    Long clubMemberId;
    Club club;
    Member member;
    ClubMember clubMember;



    @BeforeEach
    public void setUp() throws Throwable {


        PostController target = new PostController(postService);
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        proxy = factory.getProxy();

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();

        mockMvc = MockMvcBuilders
                .standaloneSetup(proxy)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();


        postId = -123L;
        clubId = -1L;
        memberId = -11L;
        clubMemberId = 11L;

        club = Club.builder().build();
        member = Member.builder().build();
        clubMember = ClubMember.createClubMember(member, club);
        ReflectionTestUtils.setField(club, "id", clubId);
        ReflectionTestUtils.setField(member, "id", memberId);
        ReflectionTestUtils.setField(clubMember, "id", clubMemberId);
    }

    private void doAnswerAspect() throws Throwable {
        doAnswer(invocation -> {
             ProceedingJoinPoint joinPoint = invocation.getArgument(0);
            Object[] args = joinPoint.getArgs();
            args[0] = clubMemberId;
            return joinPoint.proceed(args);
        }).when(aspect).generateClubMemberId(any(ProceedingJoinPoint.class));
    }

    private <T> T getResponse(ResultActions resultActions, Class<T> classOfT) throws UnsupportedEncodingException {
        return gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), classOfT);
    }

    private ResultActions mockCreatePost(Create createDto) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.post("/posts")
                        .param("clubId",clubId.toString())
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void PostController_CreatePost_CREATED() throws Throwable {

        final Create createDto = Create.builder()
                .content("test")
                .clubId(clubId)
                .build();

        final Response responseDto = Response.builder().id(1L).content("test").likeCount(0).commentCount(0).build();

        doAnswerAspect();
        doReturn(responseDto).when(postService).createPost(anyLong(),any(Create.class));

        final ResultActions resultActions = mockCreatePost(createDto);

        resultActions.andExpect(status().isCreated());

        final Response response = getResponse(resultActions, Response.class);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getContent()).isEqualTo("test");
        verify(postService, times(1)).createPost(anyLong(),any(Create.class));
    }

    @Test
    public void PostController_CreatePost_FORBIDDEN() throws Throwable {

        final Create createDto = Create.builder()
                .content("test")
                .clubId(clubId)
                .build();

        doThrow(new ClubMemberException(DIFFERENT_CLUB_EXCEPTION)).when(postService).createPost(anyLong(), any(Create.class));
        doAnswerAspect();

        final ResultActions resultActions = mockCreatePost(createDto);

        resultActions.andExpect(status().isForbidden());
        verify(postService, times(1)).createPost(anyLong(),any(Create.class));


    }

    @Test
    public void PostController_CreatePost_NOTFOUND() throws Throwable {

        final Create createDto = Create.builder()
                .content("test")
                .clubId(clubId)
                .build();

        doThrow(new ClubException(CLUB_NOT_FOUND)).when(postService).createPost(anyLong(), any(Create.class));
        doAnswerAspect();

        final ResultActions resultActions = mockCreatePost(createDto);

        resultActions.andExpect(status().isNotFound());
        verify(postService, times(1)).createPost(anyLong(),any(Create.class));



    }


    @Test
    @DisplayName("Post 생성 요청할 때 requestBody 중 clubId null 인 경우 BadRequest 응답")
    public void PostController_CreatePost_BADREQUEST() throws Throwable {

        final Create createDto = Create.builder()
                .content("test")
                .build();

        final ResultActions resultActions = mockCreatePost(createDto);

        resultActions.andExpect(status().isBadRequest());
        verify(postService, times(0)).createPost(anyLong(),any(Create.class));

    }


    @Test
    public void PostController_GetPostsByClubId_OK() throws Throwable {

        doAnswerAspect();
        doAnswer(invocation -> new SliceResponse(List.of(), Pageable.unpaged(), false))
                .when(postService).getPostsByClubId(anyLong(), anyLong(), any(Pageable.class));


        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts")
                        .param("clubId",clubId.toString())
                .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isOk());
        verify(postService,times(1)).getPostsByClubId(anyLong(),anyLong(), any(Pageable.class));
    }

    @Test
    public void PostController_GetPostsByClubId_FORBIDDEN() throws Throwable {

        doAnswerAspect();
        doThrow(new ClubMemberException(DIFFERENT_CLUB_EXCEPTION)).when(postService).getPostsByClubId(anyLong(), anyLong(), any(Pageable.class));

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts")
                        .param("clubId",clubId.toString())
                .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isForbidden());
        verify(postService,times(1)).getPostsByClubId(anyLong(),anyLong(), any(Pageable.class));

    }

    @Test
    public void PostController_GetMyPostsByClubId_OK() throws Throwable {

        doAnswer(invocation -> new SliceResponse(List.of(), PageRequest.of(0,10), false))
                .when(postService).getMyPostsByClubId(anyLong(), anyLong(), any(Pageable.class));

        doAnswerAspect();

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/members/{memberId}/posts", memberId)
                        .param("clubId", clubId.toString()));

        resultActions.andExpect(status().isOk());

        verify(postService, times(1)).getMyPostsByClubId(anyLong(), anyLong(), any(Pageable.class));
    }

    @Test
    public void PostController_GetMyPostsByClubId_FORBIDDEN() throws Throwable {

        doAnswerAspect();
        doThrow(new ClubMemberException(DIFFERENT_CLUB_EXCEPTION)).when(postService).getMyPostsByClubId(anyLong(), anyLong(), any(Pageable.class));

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/members/{memberId}/posts", memberId)
                        .param("clubId",clubId.toString()));

        resultActions.andExpect(status().isForbidden());
        verify(postService, times(1)).getMyPostsByClubId(anyLong(), anyLong(), any(Pageable.class));

    }


    @Test
    public void PostController_GetPost_OK() throws Throwable {

        Post post = Post.builder().clubMember(clubMember).club(club).build();
        ReflectionTestUtils.setField(post, "id", postId);

        doReturn(Response.of(post, 0, 0)).when(postService).getPostById(anyLong(), anyLong());
        doAnswerAspect();

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/{postId}", postId)
                        .param("clubId",clubId.toString()));

        resultActions.andExpect(status().isOk());
        Response response = getResponse(resultActions, Response.class);
        assertThat(response.getId()).isEqualTo(postId);
        verify(postService, times(1)).getPostById(clubMemberId, postId);
    }



    @Test
    public void PostController_GetPost_FORBIDDEN() throws Throwable {

        doAnswerAspect();
        doThrow(new ClubMemberException(DIFFERENT_CLUB_EXCEPTION)).when(postService).getPostById(anyLong(), anyLong());

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/{postId}", postId)
                        .param("clubId",clubId.toString()));

        resultActions.andExpect(status().isForbidden());
        verify(postService, times(1)).getPostById(clubMemberId, postId);

    }


    @Test
    public void PostController_DeletePost_NOCONTENT() throws Throwable {

        doAnswerAspect();

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/posts/{postId}", postId)
                        .param("clubId",clubId.toString()));

        resultActions.andExpect(status().isNoContent());

        verify(postService, times(1)).deletePostById(clubMemberId, postId);
    }

    @Test
    public void PostController_DeletePost_FORBIDDEN() throws Throwable {

        doAnswerAspect();
        doThrow(new PostException(DELETE_AUTHORIZATION_DENIED)).when(postService).deletePostById(anyLong(), anyLong());

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/posts/{postId}", postId)
                        .param("clubId",clubId.toString()));

        resultActions.andExpect(status().isForbidden());

        verify(postService, times(1)).deletePostById(clubMemberId, postId);
    }


    @Test
    public void PostController_UpdatePost_OK() throws Throwable
    {
        Update updateDto = Update.builder()
                .id(postId)
                .content("update")
                .build();

        doAnswerAspect();
        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/posts/{postId}", postId)
                        .param("clubId",clubId.toString())
                        .content(gson.toJson(updateDto))
                        .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isOk());
    }

    @Test
    public void PostController_updatePost_FORBIDDEN() throws Throwable {

        Update updateDto = Update.builder()
                .id(postId)
                .content("update")
                .build();

        doAnswerAspect();
        doThrow(new PostException(UPDATE_AUTHORIZATION_DENIED)).when(postService).updatePost(anyLong(), any(Update.class));

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/posts/{postId}", postId)
                        .param("clubId",clubId.toString())
                        .content(gson.toJson(updateDto))
                        .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isForbidden());

        verify(postService, times(1)).updatePost(anyLong(), any(Update.class));
    }

    @Test
    public void PostController_LikePost_OK() throws Throwable
    {

        doAnswerAspect();
        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/posts/{postId}/like", postId)
                        .param("clubId",clubId.toString()));

        resultActions.andExpect(status().isOk());

        verify(postService, times(1)).likePost(clubMemberId, postId);
    }


}

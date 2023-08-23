package com.dp.dplanner.controller;

import com.dp.dplanner.aop.aspect.GeneratedClubMemberIdAspect;
import com.dp.dplanner.dto.CommentDto.Create;
import com.dp.dplanner.exception.CommentException;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.exception.GlobalExceptionHandler;
import com.dp.dplanner.service.CommentService;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static com.dp.dplanner.dto.CommentDto.*;
import static com.dp.dplanner.exception.ErrorResult.DELETE_AUTHORIZATION_DENIED;
import static com.dp.dplanner.exception.ErrorResult.UPDATE_AUTHORIZATION_DENIED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CommentControllerTest {

    @InjectMocks
    private CommentController proxy;

    @Mock
    private CommentService commentService;

    @Mock
    private GeneratedClubMemberIdAspect aspect;

    private MockMvc mockMvc;
    private Gson gson;

    Long clubMemberId;
    Long postId;
    Long memberId;
    Long clubId;
    Long commentId;
    @BeforeEach
    public void setUp() {
        CommentController target = new CommentController(commentService);
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

        clubMemberId = 123L;
        postId = 12L;
        memberId = 1L;
        clubId = 10L;
        commentId = -1L;

    }

    private void doAnswerAspect() throws Throwable {
        doAnswer(invocation -> {
            ProceedingJoinPoint joinPoint = invocation.getArgument(0);
            Object[] args = joinPoint.getArgs();
            args[0] = clubMemberId;
            return joinPoint.proceed(args);
        }).when(aspect).generateClubMemberId(any(ProceedingJoinPoint.class));
    }

    @Test
    public void CommentController_CreateComment_CREATED() throws Throwable
    {
        Create createDto = Create.builder()
                .postId(postId)
                .parentId(null)
                .content("test")
                .build();

        doAnswerAspect();
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/comments")
                        .param("clubId",clubId.toString())
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isCreated());

        verify(commentService, times(1)).createComment(anyLong(), any(Create.class));
    }

    @Test
    public void CommentController_CreateComment_BADREQUEST() throws Throwable
    {
        Create createDto = Create.builder()
                .postId(postId)
                .parentId(null)
                .content("test")
                .build();

        doAnswerAspect();
        doThrow(new CommentException(ErrorResult.CREATE_COMMENT_DENIED)).when(commentService).createComment(anyLong(), any(Create.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/comments")
                        .param("clubId",clubId.toString())
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isBadRequest());

        verify(commentService, times(1)).createComment(anyLong(), any(Create.class));
    }

    @Test
    public void CommentController_getComments() throws Throwable
    {
        doAnswerAspect();
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/{postId}/comments", postId)
                        .param("clubId",clubId.toString())
        );

        resultActions.andExpect(status().isOk());

        verify(commentService, times(1)).getCommentsByPostId(clubMemberId, postId);

    }

    @Test
    public void CommentController_getMyComments() throws Throwable {
        doAnswerAspect();
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/members/{memberId}/comments", memberId)
                        .param("clubId",clubId.toString())
        );

        resultActions.andExpect(status().isOk());

        verify(commentService, times(1)).getCommentsByClubMemberId(clubMemberId, clubId);
    }

    @Test
    public void CommentController_UpdateComment_OK() throws Throwable
    {
        Long commentId = -1L;

        Update updateDto = Update.builder()
                .content("update")
                .build();

        doAnswerAspect();
        doReturn(Response.builder().id(commentId).content(updateDto.getContent()).build())
                .when(commentService).updateComment(anyLong(), any(Update.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/comments/{commentId}",commentId)
                        .param("clubId",clubId.toString())
                        .content(gson.toJson(updateDto))
                        .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isOk());

        Response response = gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(), Response.class);

        Assertions.assertThat(response.getContent()).isEqualTo("update");

        verify(commentService, times(1)).updateComment(anyLong(), any(Update.class));
    }

    @Test
    public void CommentController_UpdateComment_FORBIDDEN() throws Throwable
    {

        Update updateDto = Update.builder()
                .content("update")
                .build();

        doAnswerAspect();
        doThrow(new CommentException(UPDATE_AUTHORIZATION_DENIED)).when(commentService).updateComment(anyLong(), any(Update.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/comments/{commentId}",commentId)
                        .param("clubId",clubId.toString())
                        .content(gson.toJson(updateDto))
                        .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isForbidden());

        verify(commentService, times(1)).updateComment(anyLong(), any(Update.class));
    }
    
    @Test
    public void CommentController_deleteComment_NOCONTENT() throws Throwable
    {
        doAnswerAspect();

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/comments/{commentId}", commentId)
                        .param("clubId", clubId.toString()));

        resultActions.andExpect(status().isNoContent());

        verify(commentService, times(1)).deleteComment(clubMemberId, commentId);
    }

    @Test
    public void CommentController_deleteComment_FORBIDDEN() throws Throwable
    {
        doAnswerAspect();
        doThrow(new CommentException(DELETE_AUTHORIZATION_DENIED)).when(commentService).deleteComment(clubMemberId, commentId);
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/comments/{commentId}", commentId)
                        .param("clubId", clubId.toString()));

        resultActions.andExpect(status().isForbidden());

        verify(commentService, times(1)).deleteComment(clubMemberId, commentId);

    }

    @Test
    public void CommentController_likeComment_OK() throws Throwable
    {
        doAnswerAspect();

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/comments/{commentId}/like", commentId)
                        .param("clubId", clubId.toString()));

        resultActions.andExpect(status().isOk());

        verify(commentService, times(1)).likeComment(clubMemberId, commentId);


    }




}

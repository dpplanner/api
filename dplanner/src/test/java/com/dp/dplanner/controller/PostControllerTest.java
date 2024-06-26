package com.dp.dplanner.controller;

import com.dp.dplanner.adapter.controller.PostController;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.adapter.controller.GlobalExceptionHandler;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.service.PostService;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.dp.dplanner.adapter.dto.PostDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PostControllerTest {


    @InjectMocks
    private PostController target;
    @Mock
    private PostService postService;

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


        target = new PostController(postService);

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();

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

        mockMvc = MockMvcBuilders
                .standaloneSetup(target)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver(memberId, clubId, clubMemberId), new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    /**
     * utility methods
     */
    private <T> T getResponse(ResultActions resultActions, Class<T> responseType) throws UnsupportedEncodingException {
        Type type = TypeToken.getParameterized(CommonResponse.class, responseType).getType();
        return ((CommonResponse<T>) gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), type)).getData();
    }


    private ResultActions mockCreatePost(Create createDto) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.multipart("/posts")
                        .file(new MockMultipartFile("create","",MediaType.APPLICATION_JSON_VALUE,"{\"clubId\": -1, \"content\":\"hello\"}".getBytes()))
                        .file(new MockMultipartFile("files", "fileName" + "." + "jpg", "jpg", "image".getBytes()))
                        .file(new MockMultipartFile("files", "fileName2" + "." + "jpg", "jpg", "image2".getBytes()))
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)

        );
    }
    @Test
    public void PostController_CreatePost_CREATED() throws Throwable {

        final Create createDto = Create.builder()
                .content("test")
                .clubId(clubId)
                .build();

        final Response responseDto = Response.builder().id(1L).content("test").likeCount(0L).commentCount(0L).build();

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

        doThrow(new ServiceException(DIFFERENT_CLUB_EXCEPTION)).when(postService).createPost(anyLong(), any(Create.class));

        final ResultActions resultActions = mockCreatePost(createDto);

        resultActions.andExpect(status().isNotFound());
        verify(postService, times(1)).createPost(anyLong(),any(Create.class));


    }

    @Test
    public void PostController_CreatePost_NOTFOUND() throws Throwable {

        final Create createDto = Create.builder()
                .content("test")
                .clubId(clubId)
                .build();

        doThrow(new ServiceException(CLUB_NOT_FOUND)).when(postService).createPost(anyLong(), any(Create.class));

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

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.multipart("/posts")
                        .file(new MockMultipartFile("create", "", MediaType.APPLICATION_JSON_VALUE, "{\"content\":\"hello\"}" .getBytes()))
                        .file(new MockMultipartFile("files", "fileName" + "." + "jpg", "jpg", "image" .getBytes()))
                        .file(new MockMultipartFile("files", "fileName2" + "." + "jpg", "jpg", "image2" .getBytes()))
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE));

        resultActions.andExpect(status().isBadRequest());
        verify(postService, times(0)).createPost(anyLong(),any(Create.class));

    }


    @Test
    public void PostController_GetPostsByClubId_OK() throws Throwable {

        doAnswer(invocation -> new SliceResponse(List.of(), Pageable.unpaged(), false))
                .when(postService).getPostsByClubId(anyLong(), anyLong(), any(Pageable.class));


        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/clubs/{clubId}",clubId)
                .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isOk());
        verify(postService,times(1)).getPostsByClubId(anyLong(),anyLong(), any(Pageable.class));
    }

    @Test
    public void PostController_GetPostsByClubId_FORBIDDEN() throws Throwable {

        doThrow(new ServiceException(DIFFERENT_CLUB_EXCEPTION)).when(postService).getPostsByClubId(anyLong(), anyLong(), any(Pageable.class));

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/clubs/{clubId}",clubId)
                .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isNotFound());
        verify(postService,times(1)).getPostsByClubId(anyLong(),anyLong(), any(Pageable.class));

    }

    @Test
    public void PostController_GetMyPostsByClubId_OK() throws Throwable {

        doAnswer(invocation -> new SliceResponse(List.of(), PageRequest.of(0,10), false))
                .when(postService).getMyPostsByClubId(anyLong(), anyLong(), any(Pageable.class));


        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/clubMembers/{clubMemberId}", clubMemberId)
        );

        resultActions.andExpect(status().isOk());

        verify(postService, times(1)).getMyPostsByClubId(anyLong(), anyLong(), any(Pageable.class));
    }

    @Test
    public void PostController_GetMyPostsByClubId_FORBIDDEN() throws Throwable {

        doThrow(new ServiceException(DIFFERENT_CLUB_EXCEPTION)).when(postService).getMyPostsByClubId(anyLong(), anyLong(), any(Pageable.class));

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/clubMembers/{clubMemberId}", clubMemberId)
        );

        resultActions.andExpect(status().isNotFound());
        verify(postService, times(1)).getMyPostsByClubId(anyLong(), anyLong(), any(Pageable.class));

    }


    @Test
    public void PostController_GetPost_OK() throws Throwable {

        Post post = Post.builder().clubMember(clubMember).club(club).build();
        ReflectionTestUtils.setField(post, "id", postId);

        doReturn(Response.of(post, 0L, 0L,false)).when(postService).getPostById(anyLong(), anyLong());

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/{postId}", postId)
        );

        resultActions.andExpect(status().isOk());
        Response response = getResponse(resultActions, Response.class);
        assertThat(response.getId()).isEqualTo(postId);
        verify(postService, times(1)).getPostById(clubMemberId, postId);
    }



    @Test
    public void PostController_GetPost_FORBIDDEN() throws Throwable {

        doThrow(new ServiceException(DIFFERENT_CLUB_EXCEPTION)).when(postService).getPostById(anyLong(), anyLong());

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/{postId}", postId)
        );

        resultActions.andExpect(status().isNotFound());
        verify(postService, times(1)).getPostById(clubMemberId, postId);

    }


    @Test
    public void PostController_DeletePost_NOCONTENT() throws Throwable {


        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/posts/{postId}", postId)
        );

        resultActions.andExpect(status().isNoContent());

        verify(postService, times(1)).deletePostById(clubMemberId, postId);
    }

    @Test
    public void PostController_DeletePost_FORBIDDEN() throws Throwable {

        doThrow(new ServiceException(DELETE_AUTHORIZATION_DENIED)).when(postService).deletePostById(anyLong(), anyLong());

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/posts/{postId}", postId)
        );

        resultActions.andExpect(status().isNotFound());

        verify(postService, times(1)).deletePostById(clubMemberId, postId);
    }


    @Test
    public void PostController_UpdatePost_OK() throws Throwable
    {

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.multipart(HttpMethod.PUT,"/posts/{postId}", postId)
                        .file(new MockMultipartFile("update", "", MediaType.APPLICATION_JSON_VALUE, "{\"id\" : 1, \"content\":\"hello\"}".getBytes()))
                        .file(new MockMultipartFile("files", "fileName" + "." + "jpg", "jpg", "image".getBytes()))
                        .file(new MockMultipartFile("files", "fileName2" + "." + "jpg", "jpg", "image2".getBytes()))
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE));

        resultActions.andExpect(status().isOk());
    }

    @Test
    public void PostController_updatePost_FORBIDDEN() throws Throwable {


        doThrow(new ServiceException(UPDATE_AUTHORIZATION_DENIED)).when(postService).updatePost(anyLong(), any(Update.class));

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.multipart(HttpMethod.PUT,"/posts/{postId}", postId)
                        .file(new MockMultipartFile("update", "", MediaType.APPLICATION_JSON_VALUE, "{\"id\" : 1, \"content\":\"hello\"}".getBytes()))
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE));

        resultActions.andExpect(status().isNotFound());

        verify(postService, times(1)).updatePost(anyLong(), any(Update.class));
    }

    @Test
    public void PostController_LikePost_OK() throws Throwable
    {

        final ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/posts/{postId}/like", postId)
                        .param("clubId",clubId.toString())
        );

        resultActions.andExpect(status().isOk());

        verify(postService, times(1)).likePost(clubMemberId, postId);
    }
    @Test
    public void CommentController_getMyComments() throws Throwable {
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/posts/clubMembers/{clubMemberId}/commented", clubMemberId)
        );

        resultActions.andExpect(status().isOk());
        Pageable pageable = PageRequest.of(0, 10);
        verify(postService, times(1)).getCommentedPosts(clubMemberId, clubId, pageable);
    }

}

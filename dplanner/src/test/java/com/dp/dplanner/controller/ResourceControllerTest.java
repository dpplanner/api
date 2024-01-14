package com.dp.dplanner.controller;

import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.exception.GlobalExceptionHandler;
import com.dp.dplanner.exception.ResourceException;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ResourceService;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;

import static com.dp.dplanner.dto.ResourceDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ResourceControllerTest {

    @InjectMocks
    private ResourceController target;

    @Mock
    private ResourceService resourceService;


    @Mock
    private MockMvc mockMvc;
    @Mock
    private Gson gson;
    
    Long clubMemberId ;
    Long clubId;
    

    @BeforeEach
    public void setUp() {

        target = new ResourceController(resourceService);

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();

        mockMvc = MockMvcBuilders
                .standaloneSetup(target)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver(), new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        clubMemberId = 123L;
        clubId = 12L;
               
    }


    @Test
    public void ResourceController_createResource_CREATED() throws Throwable {

        Create createDto = Create.builder()
                .clubId(clubId)
                .name("test")
                .info("test")
                .build();

        Club club = Club.builder().build();
        ReflectionTestUtils.setField(club,"id",clubId);

        Resource resource = Resource.builder()
                .club(club)
                .name("test")
                .info("test")
                .build();
        ReflectionTestUtils.setField(resource, "id", 1L);

        doReturn(Response.of(resource)).when(resourceService).createResource(anyLong(), any(Create.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/resources")
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isCreated());


        Response response = gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(), Response.class);
        assertThat(response.getClubId()).isEqualTo(clubId);
        assertThat(response.getId()).isEqualTo(1L);

        verify(resourceService, times(1)).createResource(anyLong(), any(Create.class));

    }

    @Test
    public void ResourceController_createResponse_FORBIDDEN() throws Throwable
    {
        Create createDto = Create.builder()
                .clubId(clubId)
                .name("test")
                .info("test")
                .build();


        doThrow(new ResourceException(DIFFERENT_CLUB_EXCEPTION)).when(resourceService).createResource(anyLong(), any(Create.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/resources")
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isForbidden());

        verify(resourceService, times(1)).createResource(anyLong(), any(Create.class));

    }

    @Test
    public void ResourceController_getResources_OK() throws Throwable {


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/resources")
        );

        resultActions.andExpect(status().isOk());

        verify(resourceService, times(1)).getResourceByClubId(clubMemberId, clubId);

    }

    @Test
    public void ResourceController_getResources_FORBIDDEN() throws Throwable {

        doThrow(new ResourceException(DIFFERENT_CLUB_EXCEPTION)).when(resourceService).getResourceByClubId(clubMemberId, clubId);

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/resources")
        );

        resultActions.andExpect(status().isForbidden());

        verify(resourceService, times(1)).getResourceByClubId(clubMemberId, clubId);

    }
    
    @Test
    public void ResourceController_getResource_OK() throws Throwable
    {
        Long resourceId = 1L;


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/resources/{resourceId}",resourceId)
        );

        resultActions.andExpect(status().isOk());

        verify(resourceService, times(1)).getResourceById(clubMemberId, resourceId);

    }

    @Test
    public void ResourceController_getResource_FORBIDDEN() throws Throwable
    {
        Long resourceId = 1L;

        doThrow(new ResourceException(DIFFERENT_CLUB_EXCEPTION)).when(resourceService).getResourceById(clubMemberId, resourceId);

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/resources/{resourceId}",resourceId)
        );

        resultActions.andExpect(status().isForbidden());

        verify(resourceService, times(1)).getResourceById(clubMemberId, resourceId);

    }

    @Test
    public void ResourceController_updateResource_OK() throws Throwable
    {
        Long resourceId = 1L;
        Update updateDto = Update.builder()
                .id(resourceId)
                .name("update")
                .info("update")
                .build();

        Club club = Club.builder().build();
        ReflectionTestUtils.setField(club,"id",clubId);

        Resource resource = Resource.builder()
                .club(club)
                .name("update")
                .info("update")
                .build();
        ReflectionTestUtils.setField(resource, "id", 1L);

        doReturn(Response.of(resource)).when(resourceService).updateResource(anyLong(), any(Update.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/resources/{resourceId}", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(updateDto))
        );

        resultActions.andExpect(status().isOk());
        Response response = gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(), Response.class);

        assertThat(response.getId()).isEqualTo(resourceId);
        assertThat(response.getClubId()).isEqualTo(clubId);
        assertThat(response.getName()).isEqualTo("update");
        assertThat(response.getInfo()).isEqualTo("update");

        verify(resourceService, times(1)).updateResource(anyLong(), any(Update.class));
    }

    @Test
    public void ResourceController_updateResource_FORBIDDEN() throws Throwable
    {
        Long resourceId = 1L;
        Update updateDto = Update.builder()
                .id(resourceId)
                .name("update")
                .info("update")
                .build();


        doThrow(new ResourceException(UPDATE_AUTHORIZATION_DENIED)).when(resourceService).updateResource(anyLong(), any(Update.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/resources/{resourceId}", resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(updateDto))
        );

        resultActions.andExpect(status().isForbidden());

        verify(resourceService, times(1)).updateResource(anyLong(), any(Update.class));
    }

    @Test
    public void ResourceController_deleteResource_NOCONTENT() throws Throwable
    {
        Long resourceId = 1L;


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/resources/{resourceId}",resourceId)
        );

        resultActions.andExpect(status().isNoContent());

        verify(resourceService, times(1)).deleteResource(clubMemberId, resourceId);

    }
    @Test
    public void ResourceController_deleteResource_FORBIDDEN() throws Throwable
    {
        Long resourceId = 1L;

        doThrow(new ResourceException(DELETE_AUTHORIZATION_DENIED)).when(resourceService).deleteResource(clubMemberId, resourceId);

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/resources/{resourceId}",resourceId)
        );

        resultActions.andExpect(status().isForbidden());

        verify(resourceService, times(1)).deleteResource(clubMemberId, resourceId);

    }
    class MockAuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().isAssignableFrom(PrincipalDetails.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
            return new PrincipalDetails(1L, clubId, clubMemberId, "email", null);
        }
    }
}

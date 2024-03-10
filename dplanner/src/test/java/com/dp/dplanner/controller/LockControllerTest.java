package com.dp.dplanner.controller;

import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.exception.GlobalExceptionHandler;
import com.dp.dplanner.exception.ServiceException;
import com.dp.dplanner.service.LockService;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.dp.dplanner.dto.LockDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class LockControllerTest {

    @InjectMocks
    private LockController target;
    @Mock
    private LockService lockService;
    @Mock
    private MockMvc mockMvc;
    @Mock
    private Gson gson;
    Long memberId;
    Long clubId;
    Long clubMemberId;
    Long resourceId;
    LocalDateTime start;
    LocalDateTime end ;


    @BeforeEach
    public void setUp() {


        target = new LockController(lockService);

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();

        memberId = 1L;
        clubId = 11L;
        clubMemberId = 123L;
        resourceId = 1234L;

        mockMvc = MockMvcBuilders
                .standaloneSetup(target)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver(memberId, clubId, clubMemberId), new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        start = LocalDateTime.of(2023,10,1,12,0,0);
        end = start.plusHours(1);


    }

    @Test
    public void LockController_createLock_CREATED() throws Throwable
    {
        Create createDto = Create.builder()
                .resourceId(resourceId)
                .startDateTime(start)
                .endDateTime(end)
                .build();

        Club club = Club.builder().build();
        ReflectionTestUtils.setField(club,"id",clubId);

        Resource resource = Resource.builder()
                .club(club)
                .build();

        ReflectionTestUtils.setField(resource, "id", resourceId);

        Long lockId = 123L;
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();
        ReflectionTestUtils.setField(lock, "id", lockId);

        doReturn(Response.of(lock)).when(lockService).createLock(anyLong(), any(Create.class));
        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/locks/resources/{resourceId}",resourceId)
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isCreated());

        Response response = getResponse(resultActions, Response.class);

        assertThat(response.getId()).isEqualTo(lockId);
        assertThat(response.getStartDateTime()).isEqualTo(start);
        assertThat(response.getEndDateTime()).isEqualTo(end);
        assertThat(response.getResourceId()).isEqualTo(resourceId);

        verify(lockService, times(1)).createLock(anyLong(), any(Create.class));


    }

    @Test
    public void LockController_createLock_BADREQUEST() throws Throwable
    {
        Create createDto = Create.builder()
                .resourceId(resourceId)
                .startDateTime(start)
                .endDateTime(end)
                .build();


        doThrow(new ServiceException(PERIOD_OVERLAPPED_EXCEPTION)).when(lockService).createLock(anyLong(), any(Create.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/locks/resources/{resourceId}",resourceId)
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isBadRequest());

        verify(lockService, times(1)).createLock(anyLong(), any(Create.class));

    }
    
    @Test
    public void LockController_updateLock_OK() throws Throwable
    {
        Long lockId = -1L;

        Update updateDto = Update.builder()
                .id(lockId)
                .resourceId(resourceId)
                .startDateTime(start)
                .endDateTime(start.plusHours(2))
                .build();

        Club club = Club.builder().build();
        ReflectionTestUtils.setField(club,"id",clubId);

        Resource resource = Resource.builder()
                .club(club)
                .build();

        ReflectionTestUtils.setField(resource, "id", resourceId);

        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(updateDto.getStartDateTime(),updateDto.getEndDateTime()))
                .build();

        ReflectionTestUtils.setField(lock, "id", lockId);

        doReturn(Response.of(lock)).when(lockService).updateLock(anyLong(), any(Update.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/locks/{lockId}", lockId)
                        .content(gson.toJson(updateDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isOk());

        Response response = getResponse(resultActions, Response.class);
        assertThat(response.getId()).isEqualTo(lockId);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getStartDateTime()).isEqualTo(updateDto.getStartDateTime());
        assertThat(response.getEndDateTime()).isEqualTo(updateDto.getEndDateTime());

        verify(lockService, times(1)).updateLock(anyLong(), any(Update.class));

    }

    @Test
    public void LockController_getLock_OK() throws Throwable
    {
        Long lockId = -1L;


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/locks/{lockId}", lockId)
        );

        resultActions.andExpect(status().isOk());

        verify(lockService, times(1)).getLock(clubMemberId, lockId);
    }

    @Test
    public void LockController_getLock_FORBIDDEN() throws Throwable
    {
        Long lockId = -1L;

        doThrow(new ServiceException(DIFFERENT_CLUB_EXCEPTION)).when(lockService).getLock(clubMemberId, lockId);

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/locks/{lockId}", lockId)
        );

        resultActions.andExpect(status().isNotFound());

        verify(lockService, times(1)).getLock(clubMemberId, lockId);
    }

    @Test
    public void LockController_updateLock_BADREQUEST() throws Throwable
    {
        Long lockId = -1L;

        Update updateDto = Update.builder()
                .id(lockId)
                .resourceId(resourceId)
                .startDateTime(start)
                .endDateTime(start.plusHours(2))
                .build();


        doThrow(new ServiceException(PERIOD_OVERLAPPED_EXCEPTION)).when(lockService).updateLock(anyLong(), any(Update.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/locks/{lockId}", lockId)
                        .content(gson.toJson(updateDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isBadRequest());

        verify(lockService, times(1)).updateLock(anyLong(), any(Update.class));

    }


    @Test
    public void LockController_deleteLock_NOCONTENT() throws Throwable
    {
        Long lockId = -1L;


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/locks/{lockId}",lockId)
        );

        resultActions.andExpect(status().isNoContent());

        verify(lockService,times(1)).deleteLock(clubMemberId,lockId);
    }

    @Test
    public void LockController_deleteLock_FORBIDDEN() throws Throwable
    {
        Long lockId = -1L;

        doThrow(new ServiceException(DIFFERENT_CLUB_EXCEPTION)).when(lockService).deleteLock(clubMemberId, lockId);


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/locks/{lockId}", lockId)
        );

        resultActions.andExpect(status().isNotFound());

        verify(lockService,times(1)).deleteLock(clubMemberId,lockId);
    }

    @Test
    public void LockController_getLocks_OK() throws Throwable
    {


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/locks/resources/{resourceId}", resourceId)
                        .param("startDateTime", "2023-08-25 12:00:00")
                        .param("endDateTime","2023-08-25 14:00:00")

        );

        resultActions.andExpect(status().isOk());

        verify(lockService, times(1)).getLocks(anyLong(), anyLong(), any(Period.class));

    }

    /**
     * utility methods
     */
    private <T> T getResponse(ResultActions resultActions, Class<T> responseType) throws UnsupportedEncodingException {
        Type type = TypeToken.getParameterized(CommonResponse.class, responseType).getType();
        return ((CommonResponse<T>) gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), type)).getData();
    }

}

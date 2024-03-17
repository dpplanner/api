package com.dp.dplanner.controller;

import com.dp.dplanner.adapter.controller.GlobalExceptionHandler;
import com.dp.dplanner.adapter.controller.ReservationController;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.adapter.dto.ReservationDto;
import com.dp.dplanner.adapter.dto.ReservationDto.Request;
import com.dp.dplanner.exception.*;
import com.dp.dplanner.service.ReservationService;
import com.dp.dplanner.service.exception.ServiceException;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.dp.dplanner.adapter.dto.ReservationDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ReservationControllerTest {

    @InjectMocks
    private ReservationController target;
    @Mock
    private ReservationService reservationService;
    @Mock
    private MockMvc mockMvc;
    @Mock
    private Gson gson;
    Long memberId;
    Long clubMemberId;
    Long resourceId;
    Long clubId;
    LocalDateTime start;
    LocalDateTime end;

    @BeforeEach
    public void setUp() throws Throwable {
        target = new ReservationController(reservationService);

        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();


        memberId = 1L;
        clubMemberId = 123L;
        resourceId = 12L;
        clubId = 23L;
        start = LocalDateTime.of(2023, 12, 28, 18, 0, 0);
        end = start.plusHours(2);

        mockMvc = MockMvcBuilders
                .standaloneSetup(target)
                .setCustomArgumentResolvers(new MockAuthenticationPrincipalArgumentResolver(memberId, clubId, clubMemberId))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @Disabled("메시지 큐로 API 이동")
    public void ReservationController_createReservation_CREATED() throws Throwable {

        Create createDto = Create.builder()
                .resourceId(resourceId)
                .title("title")
                .usage("usage")
                .sharing(true)
                .startDateTime(start)
                .endDateTime(end)
                .build();


        Reservation reservation = getReservation("title", "usage", resourceId, true, start, end);
        Long reservationId = -1L;
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        doReturn(Response.of(reservation)).when(reservationService).createReservation(anyLong(), any(Create.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/reservations")
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isCreated());
        Response response = gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(), Response.class);
        assertThat(response.getReservationId()).isEqualTo(reservationId);
        verify(reservationService, times(1)).createReservation(anyLong(), any(Create.class));

    }

    @ParameterizedTest
    @MethodSource("throwException")
    @Disabled("메시지 큐로 API 이동")
    public void ReservationController_createReservation_ThrowException(BaseException exception, ResultMatcher matcher) throws Throwable
    {
        Create createDto = Create.builder()
                .resourceId(resourceId)
                .title("title")
                .usage("usage")
                .sharing(true)
                .startDateTime(start)
                .endDateTime(end)
                .build();

        doThrow(exception).when(reservationService).createReservation(anyLong(), any(Create.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.post("/reservations")
                        .content(gson.toJson(createDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(matcher);
        verify(reservationService, times(1)).createReservation(anyLong(), any(Create.class));

    }
    @Test
    public void RestController_updateReservation_OK() throws Throwable
    {
        Long reservationId = -1L;
        Update updateDto = Update.builder()
                .reservationId(reservationId)
                .title("updateTitle")
                .usage("updateUsage")
                .resourceId(resourceId)
                .sharing(true)
                .startDateTime(start)
                .endDateTime(end)
                .build();

        Reservation reservation = getReservation("updateTitle","updateUsage",resourceId,true,start,end);
        ReflectionTestUtils.setField(reservation, "id", reservationId);

        doReturn(Response.of(reservation)).when(reservationService).updateReservation(anyLong(), any(Update.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/reservations/{reservationId}/update", reservationId)
                        .content(gson.toJson(updateDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isOk());
        verify(reservationService, times(1)).updateReservation(anyLong(), any(Update.class));

        Response response = getResponse(resultActions, Response.class);
        assertThat(response.getReservationId()).isEqualTo(reservationId);
        assertThat(response.getStartDateTime()).isEqualTo(start);
        assertThat(response.getEndDateTime()).isEqualTo(end);

    }

    @ParameterizedTest
    @MethodSource("throwException")
    public void ReservationController_updateReservation_ThrowException(BaseException exception, ResultMatcher matcher) throws Throwable
    {
        Long reservationId = -1L;
        Update updateDto = Update.builder()
                .reservationId(reservationId)
                .title("updateTitle")
                .usage("updateUsage")
                .resourceId(resourceId)
                .sharing(true)
                .startDateTime(start)
                .endDateTime(end)
                .build();

        doThrow(exception).when(reservationService).updateReservation(anyLong(), any(Update.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.put("/reservations/{reservationId}/update", reservationId)
                        .content(gson.toJson(updateDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(matcher);
        verify(reservationService, times(1)).updateReservation(anyLong(), any(Update.class));
    }

    @Test
    public void RestController_cancelReservation_NOCONTENT() throws Throwable
    {
        Long reservationId = -1L;
        Delete deleteDto = Delete.builder()
                .reservationId(reservationId)
                .build();


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.patch("/reservations/{reservationId}/cancel", reservationId)
                        .content(gson.toJson(deleteDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isNoContent());
        verify(reservationService, times(1)).cancelReservation(anyLong(), any(Delete.class));

    }


    @Test
    public void ReservationController_deleteReservation_NOCONTNET() throws Throwable
    {
        Long reservationId = -1L;
        Delete deleteDto = Delete.builder()
                .reservationId(reservationId)
                .build();


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/reservations")
                        .content(gson.toJson(deleteDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );

        resultActions.andExpect(status().isNoContent());

        verify(reservationService, times(1)).deleteReservation(anyLong(), any(Delete.class));
    }

    @ParameterizedTest
    @MethodSource("throwException")
    public void ReservationController_deleteReservation_ThrowException(BaseException exception, ResultMatcher matcher) throws Throwable
    {
        Long reservationId = -1L;
        Delete deleteDto = Delete.builder()
                .reservationId(reservationId)
                .build();

        doThrow(exception).when(reservationService).deleteReservation(anyLong(),any(Delete.class));

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.delete("/reservations")
                        .content(gson.toJson(deleteDto))
                        .contentType(MediaType.APPLICATION_JSON)
        );


        resultActions.andExpect(matcher);
        verify(reservationService, times(1)).deleteReservation(anyLong(), any(Delete.class));
    }
    
    @Test
    public void ReservationController_confirmAllReservations_NOCONTENT() throws Throwable
    {
        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));
        List<Request> requestDto = ReservationDto.Request.ofList(reservationIds);


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.patch("/reservations")
                        .param("confirm","true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(requestDto))
        );

        resultActions.andExpect(status().isNoContent());

        verify(reservationService, times(1)).confirmAllReservations(anyLong(), any(List.class));
    }

    @Test
    public void ReservationController_rejectAllReservations_NOCONTENT() throws Throwable
    {
        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));
        List<Request> requestDto = ReservationDto.Request.ofList(reservationIds);


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.patch("/reservations")
                        .param("confirm","false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(requestDto))
        );

        resultActions.andExpect(status().isNoContent());

        verify(reservationService, times(1)).rejectAllReservations(anyLong(), any(List.class));
    }

    @Test
    public void ReservationController_getReservation_OK() throws Throwable
    {
        Long reservationId = 1L;

        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/reservations/{reservationsId}", reservationId)
        );

        resultActions.andExpect(status().isOk());

        verify(reservationService, times(1)).findReservationById(anyLong(), any(Request.class));
    }

    @Test
    public void ReservationController_getAllReservationsByPeriod_OK() throws Throwable {



        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/reservations")
                        .param("resourceId",resourceId.toString())
                        .param("start","2023-08-25 12:00:00")
                        .param("end","2023-08-25 14:00:00")
                        .param("status","ALL")
        );

        resultActions.andExpect(status().isOk());

        verify(reservationService, times(1)).findAllReservationsByPeriod(anyLong(), any(Request.class));
    }


    @Test
    @Disabled("api 삭제")
    public void ReservationController_getAllReservationsNotConfirmed_OK() throws Throwable {

        Request requestDto = Request.builder()
                .resourceId(resourceId)
                .build();


        ResultActions resultActions = mockMvc.perform(
                MockMvcRequestBuilders.get("/reservations")
                        .param("status","NOT CONFIRMED")
                        .param("resourceId", resourceId.toString())
        );

        resultActions.andExpect(status().isOk());

        verify(reservationService, times(1)).findAllNotConfirmedReservations(anyLong(), any(Request.class));
    }


    /**
     * utility method
     */

    private Reservation getReservation(String title,String usage,Long resourceId,boolean sharing,LocalDateTime start ,LocalDateTime end) {
        Member member = Member.builder().build();
        Club club = Club.builder().build();
        ClubMember clubMember = ClubMember.createClubMember(member, club);
        Resource resource = Resource.builder().club(club).build();
        ReflectionTestUtils.setField(resource, "id", resourceId);

        return Reservation.builder()
                .resource(resource)
                .clubMember(clubMember)
                .title(title)
                .usage(usage)
                .sharing(sharing)
                .period(new Period(start, end))
                .build();
    }

    private static Stream<Arguments> throwException() {

        return Stream.of(
                Arguments.of(new ServiceException(RESERVATION_UNAVAILABLE), status().isBadRequest()),
                Arguments.of(new ServiceException(CLUBMEMBER_NOT_FOUND), status().isNotFound()),
                Arguments.of(new ServiceException(CLUBMEMBER_NOT_CONFIRMED), status().isNotFound()),
                Arguments.of(new ServiceException(RESOURCE_NOT_FOUND), status().isNotFound()),
                Arguments.of(new ServiceException(DIFFERENT_CLUB_EXCEPTION), status().isNotFound()),
                Arguments.of(new ServiceException(DIFFERENT_CLUB_EXCEPTION), status().isNotFound()),
                Arguments.of(new ServiceException(RESERVATION_NOT_FOUND),status().isNotFound()),
                Arguments.of(new ServiceException(UPDATE_AUTHORIZATION_DENIED),status().isNotFound())
        );
    }

    /**
     * utility methods
     */
    private <T> T getResponse(ResultActions resultActions, Class<T> responseType) throws UnsupportedEncodingException {
        Type type = TypeToken.getParameterized(CommonResponse.class, responseType).getType();
        return ((CommonResponse<T>) gson.fromJson(resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), type)).getData();
    }

}

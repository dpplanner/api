package com.dp.dplanner.service;

import com.dp.dplanner.domain.*;
import com.dp.dplanner.domain.club.*;
import com.dp.dplanner.adapter.dto.ReservationDto;
import com.dp.dplanner.exception.*;
import com.dp.dplanner.repository.*;
import com.dp.dplanner.service.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.domain.ReservationStatus.*;
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTests {

    @Mock
    Clock clock;
    @Mock
    ClubMemberRepository clubMemberRepository;
    @Mock
    ResourceRepository resourceRepository;
    @Mock
    LockRepository lockRepository;
    @Mock
    ReservationRepository reservationRepository;
    @Mock
    MessageService messageService;
    @Mock
    ReservationInviteeRepository reservationInviteeRepository;
    @Mock
    RedisReservationService redisReservationService;

    @InjectMocks
    ReservationService reservationService;


    ClubMember clubMember;
    ClubMember sameClubMember;
    Resource resource;
    ClubAuthority clubAuthority;

    ClubMember otherClubMember;
    Resource otherClubResource;
    LocalDateTime fixedNow;

    @BeforeEach
    void setUp() {
        Club club = Club.builder().build();
        ReflectionTestUtils.setField(club, "id", 56L);

        resource = createResource(club, 111L);
        clubMember = createClubMember(club, 11L);
        sameClubMember = createClubMember(club, 22L);

        Club otherClub = Club.builder().build();
        ReflectionTestUtils.setField(otherClub, "id", 98L);

        otherClubResource = createResource(otherClub, 33L);
        otherClubMember = createClubMember(otherClub, 44L);

        clubAuthority = ClubAuthority.builder()
                .club(clubMember.getClub())
                .clubAuthorityTypes(List.of(ClubAuthorityType.SCHEDULE_ALL))
                .build();

        fixedNow = LocalDateTime.of(2023, 8, 5, 0, 0);
    }


    /**
     * createReservation
     */
    @Test
    @DisplayName("일반 회원은 승인 대기 상태의 예약을 생성할 수 있다.")
    public void createReservationRequestByUser() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(redisReservationService.saveReservation(any(), any(), any())).willReturn(true);
        given(clock.instant()).willReturn(fixedNow.atZone(ZoneId.systemDefault()).toInstant());
        given(clock.getZone()).willReturn(ZoneId.systemDefault());

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),clubMember.getId());
        ReservationDto.Response responseDto = reservationService.createReservation(clubMember.getId(), createDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getClubMemberId()).as("예약자 정보가 일치해야 한다").isEqualTo(clubMember.getId());
        assertThat(responseDto.getResourceId()).as("예약한 리소스 정보가 일치해야 한다").isEqualTo(resource.getId());
        assertThat(responseDto.getTitle()).as("예약 제목이 일치해야 한다").isEqualTo(createDto.getTitle());
        assertThat(responseDto.getUsage()).as("예약 용도가 일치해야 한다").isEqualTo(createDto.getUsage());
        assertThat(responseDto.isSharing()).as("공유 여부가 일치해야 한다").isEqualTo(createDto.isSharing());
        assertThat(responseDto.getStartDateTime()).as("예약 시작 시간이 일치해야 한다").isEqualTo(createDto.getStartDateTime());
        assertThat(responseDto.getEndDateTime()).as("예약 종료 시간이 일치해야 한다").isEqualTo(createDto.getEndDateTime());
        assertThat(responseDto.getStatus()).as("예약은 REQUEST 상태여야 한다").isEqualTo(REQUEST.name());
    }

    @Test
    @DisplayName("일반 회원은 예약 주인이 본인이 아닌 예약을 신청할 경우에는 REQUEST_IS_INVALID.")
    public void createReservationRequestByUser_RequestOwnerIsDifferent() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(redisReservationService.saveReservation(any(), any(), any())).willReturn(true);

        //when
        Long otherReservationOwnerId = 1234L;
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),otherReservationOwnerId);

        //then
        ServiceException serviceException = assertThrows(ServiceException.class, () -> reservationService.createReservation(clubMember.getId(), createDto));
        assertThat(serviceException.getMessage()).isEqualTo(REQUEST_IS_INVALID.getMessage());
    }


    @Test
    @DisplayName("예약시 invitee를 초대할 수 있다.")
    public void createReservationRequestWithInvitee() throws Exception {

        Reservation reservation = Reservation.builder().clubMember(clubMember).resource(resource).period(new Period(getTime(20), getTime(21))).build();
        ClubMember invitee = sameClubMember;
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(clubMemberRepository.findById(invitee.getId())).willReturn(Optional.ofNullable(invitee));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willReturn(reservation);
        given(redisReservationService.saveReservation(any(), any(), any())).willReturn(true);
        given(clock.instant()).willReturn(fixedNow.atZone(ZoneId.systemDefault()).toInstant());
        given(clock.getZone()).willReturn(ZoneId.systemDefault());



        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),clubMember.getId());
        createDto.setReservationInvitees(List.of(invitee.getId()));

        ReservationDto.Response responseDto = reservationService.createReservation(clubMember.getId(), createDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getStartDateTime()).as("예약 시작 시간이 일치해야 한다").isEqualTo(createDto.getStartDateTime());
        assertThat(responseDto.getEndDateTime()).as("예약 종료 시간이 일치해야 한다").isEqualTo(createDto.getEndDateTime());
        assertThat(responseDto.getInvitees().size()).as("초대한 맴버가 존재해야 한다").isEqualTo(1);
        assertThat(responseDto.getInvitees().get(0).getClubMemberId()).as("초대한 맴버의 아이디가 일치해야 한다").isEqualTo(invitee.getId());
    }

    @Test
    @DisplayName("관리자는 승인된 예약을 생성할 수 있다")
    public void createReservationByAdmin() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(redisReservationService.saveReservation(any(), any(), any())).willReturn(true);

        clubMember.changeRole(ClubRole.ADMIN);

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),clubMember.getId());
        ReservationDto.Response responseDto = reservationService.createReservation(clubMember.getId(), createDto);

        //then
        assertThat(responseDto.getStatus()).as("예약은 승인된 상태여야 한다").isEqualTo(CONFIRMED.name());
    }

    @Test
    @DisplayName("권한이 있는 매니저는 승인된 예약을 생성할 수 있다")
    public void createReservationByManagerHasSCHEDULE_ALL() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(redisReservationService.saveReservation(any(), any(), any())).willReturn(true);

        clubMember.changeRole(ClubRole.MANAGER);
        clubMember.updateClubAuthority(clubAuthority);

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),clubMember.getId());
        ReservationDto.Response responseDto = reservationService.createReservation(clubMember.getId(), createDto);

        //then
        assertThat(responseDto.getStatus()).as("예약은 승인된 상태여야 한다").isEqualTo(CONFIRMED.name());
    }


    @Test
    @DisplayName("예약 권한이 있는 매니저나 관리자는 본인이 아닌 다른 클럽 멤버를 예약 주인으로 신청할 수 있다.")
    public void createReservationDifferentReservationOwner() throws Exception {
        //given
        Long diffrentReservaionOwnerId = sameClubMember.getId();
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(clubMemberRepository.findById(sameClubMember.getId())).willReturn(Optional.ofNullable(sameClubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(redisReservationService.saveReservation(any(), any(), any())).willReturn(true);

        clubMember.changeRole(ClubRole.MANAGER);
        clubMember.updateClubAuthority(clubAuthority);

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),diffrentReservaionOwnerId);
        ReservationDto.Response responseDto = reservationService.createReservation(clubMember.getId(), createDto);

        //then
        assertThat(responseDto.getStatus()).as("예약은 승인된 상태여야 한다").isEqualTo(CONFIRMED.name());
        assertThat(responseDto.getClubMemberId()).as("예약의 주인은 본인이 아닌 다른 클럽원이다.").isEqualTo(sameClubMember.getId());
        assertThat(responseDto.getClubMemberName()).as("예약의 주인은 본인이 아닌 다른 클럽원이다.").isEqualTo(sameClubMember.getName());
    }
    @Test
    @DisplayName("레디스에 이미 해당 예약에 대한 키가 있으면 REQUEST_IS_INVALID.")
    public void createReservationConcurrentRequestThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(false);
        given(redisReservationService.saveReservation(any(), any(), any())).willReturn(false);

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),clubMember.getId());
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> reservationService.createReservation(clubMember.getId(), createDto));

        //then
        assertThat(runtimeException.getMessage()).isEqualTo(RESERVATION_UNAVAILABLE.getMessage());

    }

    @Test
    @DisplayName("요청한 시간에 다른 예약이 있으면 RESERVATION_UNAVAILABLE")
    public void createReservationWhenPeriodOverlappedThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(true);

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),clubMember.getId());

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.createReservation(clubMember.getId(), createDto));
        assertThat(exception.getErrorResult()).as("예약이 불가능하면 RESERVATION_UNAVAILABLE 예외를 던진다.")
                .isEqualTo(RESERVATION_UNAVAILABLE);
    }

    @Test
    @DisplayName("요청한 시간에 락이 걸려 있으면 RESERVATION_UNAVAILABLE")
    public void createReservationWhenLockedThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(lockRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(true);
        given(reservationRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(false);
        given(redisReservationService.saveReservation(any(), any(), eq(resource.getId()))).willReturn(true);

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),clubMember.getId());

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.createReservation(clubMember.getId(), createDto));
        assertThat(exception.getErrorResult()).as("예약이 불가능하면 RESERVATION_UNAVAILABLE 예외를 던진다.")
                .isEqualTo(RESERVATION_UNAVAILABLE);
    }

    @Test
    @DisplayName("일반 사용자는 현재로부터 bookableSpan 이후 날짜는 예약할 수 없다. RESERVATION_UNAVAILABLE")
    public void createReservation7DaysAwayThenException() throws Exception {
        //given
        given(lockRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(false);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(false);
        given(redisReservationService.saveReservation(any(), any(), eq(resource.getId()))).willReturn(true);

        LocalDateTime now = LocalDateTime.of(2023, 8, 2, 20, 0); // now : 2023-08-02
        given(clock.instant()).willReturn(now.atZone(ZoneId.systemDefault()).toInstant());
        given(clock.getZone()).willReturn(ZoneId.systemDefault());
        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false,  LocalDateTime.of(2023, 8, 10, 20, 0), LocalDateTime.of(2023, 8, 10, 21, 0),clubMember.getId());

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.createReservation(clubMember.getId(), createDto));
        assertThat(exception.getMessage()).as("예약이 불가능하면 RESERVATION_UNAVAILABLE 예외를 던진다.")
                .isEqualTo("BookableSpan Validation Error");
    }

    @Test
    @DisplayName("일반 회원은 과거 시간의 예약 요청을 할 수 없다..")
    public void createPastReservationRequestByUserThrowException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(false);
        given(redisReservationService.saveReservation(any(), any(), any())).willReturn(true);
        fixedNow = LocalDateTime.of(2023, 8, 10, 20, 0);
        given(clock.instant()).willReturn(fixedNow.atZone(ZoneId.systemDefault()).toInstant());
        given(clock.getZone()).willReturn(ZoneId.systemDefault());

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(18), getTime(19),clubMember.getId());
        //then
        BaseException exception = assertThrows(ServiceException.class, () -> reservationService.createReservation(clubMember.getId(), createDto));
        assertThat(exception.getMessage()).isEqualTo(REQUEST_IS_INVALID.getMessage());

        //when
        ReservationDto.Create createDto2 = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(18), getTime(20),clubMember.getId());
        //then
        BaseException exception2 = assertThrows(ServiceException.class, () -> reservationService.createReservation(clubMember.getId(), createDto2));
        assertThat(exception2.getMessage()).isEqualTo(REQUEST_IS_INVALID.getMessage());
    }
    @Test
    @DisplayName("다른 클럽의 회원이 요청하면 DIFFERENT_CLUB_EXCEPTION")
    public void createReservationByOtherClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(otherClubMember.getId())).willReturn(Optional.ofNullable(otherClubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),otherClubMember.getId());

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.createReservation(otherClubMember.getId(), createDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 리소스를 조회하면 DIFFERENT_CLUB_EXCEPTION 예외를 던진다.")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("예약 요청시 회원 정보가 없으면 CLUBMEMBER_NOT_FOUND")
    public void createReservationByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),null);

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.createReservation(clubMember.getId(), createDto));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("승인되지 않은 회원이 예약 요청시 CLUBMEMBER_NOT_CONFIRMED")
    public void createReservationByUnconfirmedClubMemberThenException() throws Exception {
        //given
        Long unconfirmedId = 3L;
        ClubMember unconfirmed = ClubMember.createClubMember(Member.builder().build(), Club.builder().build());
        given(clubMemberRepository.findById(unconfirmedId)).willReturn(Optional.ofNullable(unconfirmed));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),unconfirmedId);

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.createReservation(unconfirmedId, createDto));
        assertThat(exception.getErrorResult()).as("승인되지 않은 클럽회원일 경우 CLUBMEMBER_NOT_CONFIRMED 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_CONFIRMED);
    }

    @Test
    @DisplayName("예약 요청시 리소스 정보가 없으면 RESOURCE_NOT_FOUND")
    public void createReservationByNoResourceThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21),clubMember.getId());

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.createReservation(clubMember.getId(), createDto));
        assertThat(exception.getErrorResult()).as("리소스가 없으면 RESOURCE_NOT_FOUND 에러를 던진다")
                .isEqualTo(RESOURCE_NOT_FOUND);
    }


    /**
     * updateReservation
     */
    @Test
    @DisplayName("사용자는 본인의 예약을 수정할 수 있다.")
    @Disabled("예약 시간 수정은 불가능해짐에 따라 test disabled")
    public void updateReservationByUser() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMember.getId(), updateDto);
        
        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getClubMemberId()).as("예약자 정보가 일치해야 한다").isEqualTo(clubMember.getId());
        assertThat(responseDto.getResourceId()).as("예약한 리소스 정보가 일치해야 한다").isEqualTo(resource.getId());
        assertThat(responseDto.getTitle()).as("예약 제목이 일치해야 한다").isEqualTo(updateDto.getTitle());
        assertThat(responseDto.getUsage()).as("예약 용도가 일치해야 한다").isEqualTo(updateDto.getUsage());
        assertThat(responseDto.isSharing()).as("공유 여부가 일치해야 한다").isEqualTo(updateDto.isSharing());
        assertThat(responseDto.getStartDateTime()).as("예약 시작 시간이 일치해야 한다").isEqualTo(updateDto.getStartDateTime());
        assertThat(responseDto.getEndDateTime()).as("예약 종료 시간이 일치해야 한다").isEqualTo(updateDto.getEndDateTime());
        assertThat(responseDto.getStatus()).as("예약은 REQUEST 상태여야 한다").isEqualTo(REQUEST.name());
    }

    @Test
    @DisplayName("사용자는 본인의 예약을 수정할 수 있다. 예약 시간은 수정 불가능 하다.")
    public void updateReservationExceptPeriod() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> reservationService.updateReservation(clubMember.getId(), updateDto));
        //then
        assertThat(runtimeException.getMessage()).isEqualTo(REQUEST_IS_INVALID.getMessage());

    }

    @Test
    @DisplayName("사용자는 invtiee 정보를 수정할 수 있다.")
    public void updateReservationWithInviteeByUser() throws Exception {

        //given
        ClubMember invitee = sameClubMember;
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.clearInvitee();
        reservation.getReservationInvitees().add(new ReservationInvitee(reservation, null)); // 기존 Reservation에 등록된 invitee 있다고 가정
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));
        given(clubMemberRepository.findById(invitee.getId())).willReturn(Optional.ofNullable(invitee));
        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(21)
        );
        updateDto.setReservationInvitees(List.of(invitee.getId()));

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMember.getId(), updateDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getStartDateTime()).as("예약 시작 시간이 일치해야 한다").isEqualTo(updateDto.getStartDateTime());
        assertThat(responseDto.getEndDateTime()).as("예약 종료 시간이 일치해야 한다").isEqualTo(updateDto.getEndDateTime());
        assertThat(responseDto.getInvitees().size()).as("예약 invitee가 update 요청과 일치해야 한다").isEqualTo(1);
        verify(reservationInviteeRepository, times(1)).deleteReservationInviteeByReservationId(reservationId);
    }

    @Test
    @DisplayName("사용자가 승인된 예약의 시간을 수정하면 승인 대기상태로 전환된다.")
    @Disabled("예약 시간 수정은 불가능해짐에 따라 test disabled")
    public void updateConfirmedReservationByUserThenNotConfirmed() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMember.getId(), updateDto);

        //then
        assertThat(responseDto.getStatus()).as("예약은 REQUEST 상태여야 한다").isEqualTo(REQUEST.name());
    }

    @Test
    @DisplayName("사용자가 승인된 예약을 수정 시 예약 시간 변경이 없으면 그대로 승인 상태가 유지된다..")
    public void updateConfirmedReservationByUserThenConfirmed() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDtoSameTime = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(21)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMember.getId(), updateDtoSameTime);

        //then
        assertThat(responseDto.getStatus()).as("예약은 Confirmed 상태여야 한다").isEqualTo(CONFIRMED.name());
    }


    @Test
    @DisplayName("관리자가 승인된 예약을 수정시 승인대기를 하지 않는다")
    @Disabled("예약 시간 수정은 불가능해짐에 따라 test disabled")
    public void updateReservationByAdminThenConfirmed() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMember.getId(), updateDto);

        //then
        assertThat(responseDto.getStatus()).as("예약은 승인 완료상태여야 한다").isEqualTo(CONFIRMED.name());
    }

    @Test
    @DisplayName("권한이 있는 매니저가 승인된 예약을 수정시 승인대기를 하지 않는다")
    @Disabled("예약 시간 수정은 불가능해짐에 따라 test disabled")
    public void updateReservationByManagerHasSCHEDULE_ALLThenConfirmed() throws Exception {
        //given
        clubMember.changeRole(ClubRole.MANAGER);
        clubMember.updateClubAuthority(clubAuthority);

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMember.getId(), updateDto);

        //then
        assertThat(responseDto.getStatus()).as("예약은 승인 완료상태여야 한다").isEqualTo(CONFIRMED.name());
    }

    @Test
    @DisplayName("예약 시간 수정시 다른 예약이 있으면 RESERVATION_UNAVAILABLE")
    @Disabled("예약 시간 수정은 불가능해짐에 따라 test disabled")
    public void updateReservationWhenOverlappedWithOtherReservationThenException() throws Exception {
        //given
        Long reservationId = 1L;
        given(reservationRepository.existsOthersBetween(any(), any(), eq(resource.getId()), eq(reservationId)))
                .willReturn(true);

        //when
        //then
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.updateReservation(clubMember.getId(), updateDto));
        assertThat(exception.getErrorResult()).as("예약이 불가능하면 RESERVATION_UNAVAILABLE 예외를 던진다")
                .isEqualTo(RESERVATION_UNAVAILABLE);
    }

    @Test
    @DisplayName("수정하려는 시간에 락이 걸려있으면 RESERVATION_UNAVAILABLE")
    @Disabled("예약 시간 수정은 불가능해짐에 따라 test disabled")
    public void updateReservationWhenLockedThenException() throws Exception {
        //given
        Long reservationId = 1L;
        given(lockRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(true);

        //when
        //then
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.updateReservation(clubMember.getId(), updateDto));
        assertThat(exception.getErrorResult()).as("예약이 불가능하면 RESERVATION_UNAVAILABLE 예외를 던진다")
                .isEqualTo(RESERVATION_UNAVAILABLE);
    }

    @Test
    @DisplayName("다른 사람의 예약을 수정하려 하면 AUTHORIZATION_DENIED")
    public void updateReservationByOtherClubMemberThenException() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        //then
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.updateReservation(otherClubMember.getId(), updateDto));
        assertThat(exception.getErrorResult()).as("수정 권한이 없으면 AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("예약 수정시 예약 정보가 없으면 RESERVATION_NOT_FOUND")
    public void updateReservationWhenNotExistsThenException() throws Exception {
        //given
        Long reservationId = 1L;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.updateReservation(clubMember.getId(), updateDto));
        assertThat(exception.getErrorResult()).as("예야 데이터가 없으면 RESERVATION_NOT_FOUND 예외를 던져야 한다")
                .isEqualTo(RESERVATION_NOT_FOUND);
    }


    /**
     * cancelReservation
     */
    @Test
    @DisplayName("승인되지 않은 예약을 취소하면 즉시 삭제할 수 있다.")
//    @DisplayName("일반 회원은 승인되지 않은 예약을 즉시 삭제할 수 있다.")
    public void cancelNotConfirmedReservationByUser() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        reservationService.cancelReservation(clubMember.getId(), deleteDto);

        //then
        Reservation deletedReservation = captureFromMockRepositoryWhenDelete();
        assertThat(deletedReservation).as("삭제된 예약은 실제 예약과 일치해야 한다").isEqualTo(reservation);
    }

    @Test
    @DisplayName("승인되지 않은 예약도 취소하면 즉시 삭제할 수 있다.")
//    @DisplayName("일반 회원은 승인되지 않은 예약을 즉시 삭제할 수 있다.")
    public void cancelConfirmedReservationByUser() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        reservationService.cancelReservation(clubMember.getId(), deleteDto);

        //then
        Reservation deletedReservation = captureFromMockRepositoryWhenDelete();
        assertThat(deletedReservation).as("삭제된 예약은 실제 예약과 일치해야 한다").isEqualTo(reservation);
    }

    @Test
    @DisplayName("일반 회원이 승인된 예약을 취소하는 경우 승인 대기상태가 된다.")
    @Disabled("CANCEL 상태 사용 안 함.")
    public void cancelConfirmedReservationByUserThenCANCEL() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        reservationService.cancelReservation(clubMember.getId(), deleteDto);

        //then
//        assertThat(reservation.getStatus()).as("예약은 CANCEL 상태여야 한다").isEqualTo(CANCEL);
    }
    
    @Test
    @DisplayName("일반 회원이 다른 회원의 예약을 취소하는 경우 AUTHORIZATION_DENIED")
    public void cancelOtherClubMemberReservationThenException() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, otherClubMember);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.cancelReservation(clubMember.getId(), deleteDto));
        assertThat(exception.getErrorResult()).as("삭제 권한이 없으면 AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(AUTHORIZATION_DENIED);
    }

    @Test
    @DisplayName("예약 취소시 예약 정보가 없으면 RESERVATION_NOT_FOUND")
    public void cancelNoReservationThenException() throws Exception {
        //given
        Long reservationId = 1L;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.cancelReservation(clubMember.getId(), deleteDto));
        assertThat(exception.getErrorResult()).as("예약 데이터가 없으면 RESERVATION_NOT_FOUND 예외를 던진다")
                .isEqualTo(RESERVATION_NOT_FOUND);
    }


    /**
     * deleteReservation
     */
    @Test
    @DisplayName("관리자는 예약을 삭제할 수 있다.")
    public void deleteReservationByAdmin() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();

        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        reservationService.deleteReservation(clubMember.getId(), deleteDto);

        //then
        Reservation deletedReservation = captureFromMockRepositoryWhenDelete();
        assertThat(deletedReservation).as("삭제된 예약은 실제 예약과 일치해야 한다").isEqualTo(reservation);
    }

    @Test
    @DisplayName("권한이 있는 매니저는 예약을 삭제할 수 있다.")
    public void deleteReservationByManagerHasSCHEDULE_ALL() throws Exception {
        //given
        clubMember.changeRole(ClubRole.MANAGER);
        clubMember.updateClubAuthority(clubAuthority);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        reservationService.deleteReservation(clubMember.getId(), deleteDto);

        //then
        Reservation deletedReservation = captureFromMockRepositoryWhenDelete();
        assertThat(deletedReservation).as("삭제된 예약은 실제 예약과 일치해야 한다").isEqualTo(reservation);
    }

    @Test
    @DisplayName("관리자는 같은 클럽의 다른 회원의 예약을 삭제할 수 있다.")
    public void deleteSameClubMemberReservationByAdmin() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, sameClubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        reservationService.deleteReservation(clubMember.getId(), deleteDto);

        //then
        Reservation deletedReservation = captureFromMockRepositoryWhenDelete();
        assertThat(deletedReservation).as("삭제된 예약은 실제 예약과 일치해야 한다").isEqualTo(reservation);
    }

    @Test
    @DisplayName("관리자가 다른 클럽의 예약을 삭제하려 하면 DIFFERENT_CLUB_EXCEPTION")
    public void deleteOtherClubMemberReservationByAdminThenException() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(otherClubResource, otherClubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.deleteReservation(clubMember.getId(), deleteDto));
        assertThat(exception.getErrorResult()).as("삭제 권한이 없으면 DELETE_AUTHORIZATION_DENIED 예외를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("예약 삭제시 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void deleteReservationByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));

        Long reservationId = 1L;

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.deleteReservation(clubMember.getId(), deleteDto));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다.")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("예약 삭제시 예약 데이터가 없으면 RESERVATION_NOT_FOUND")
    public void deleteNoReservationThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.deleteReservation(clubMember.getId(), deleteDto));
        assertThat(exception.getErrorResult()).as("예약 데이터가 없으면 RESERVATION_NOT_FOUND 예외를 던진다.")
                .isEqualTo(RESERVATION_NOT_FOUND);
    }


    /**
     * confirmAllReservations
     */
    @Test
    @DisplayName("관리자는 승인대기 상태의 예약을 승인할 수 있다.")
    public void confirmAllReservationsByAdmin() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));

        Reservation createReservation = createDefaultReservation(resource, sameClubMember);
        assert createReservation.getStatus() == REQUEST;

        Reservation updateReservation = createDefaultReservation(resource, sameClubMember);
        updateDefaultReservation(updateReservation);
        assert updateReservation.getStatus() == REQUEST;

        given(reservationRepository.findAllById(reservationIds))
                .willReturn(List.of(createReservation, updateReservation));

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(reservationIds);
        reservationService.confirmAllReservations(clubMember.getId(), requestDto);

        //then
        assertThat(createReservation.getStatus()).as("예약은 승인완료 상태여야 한다").isEqualTo(CONFIRMED);
        assertThat(updateReservation.getStatus()).as("예약은 승인완료 상태여야 한다").isEqualTo(CONFIRMED);
    }

    @Test
    @DisplayName("권한이 있는 매니저는 승인대기 상태의 예약을 승인할 수 있다.")
    public void confirmAllReservationsByManagerHasSCHEDULE_ALL() throws Exception {
        //given
        clubMember.changeRole(ClubRole.MANAGER);
        clubMember.updateClubAuthority(clubAuthority);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));

        Reservation createReservation = createDefaultReservation(resource, sameClubMember);
        assert createReservation.getStatus() == REQUEST;

        Reservation updateReservation = createDefaultReservation(resource, sameClubMember);
        updateDefaultReservation(updateReservation);
        assert updateReservation.getStatus() == REQUEST;

        given(reservationRepository.findAllById(reservationIds))
                .willReturn(List.of(createReservation, updateReservation));

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(reservationIds);
        reservationService.confirmAllReservations(clubMember.getId(), requestDto);

        //then
        assertThat(createReservation.getStatus()).as("예약은 승인완료 상태여야 한다").isEqualTo(CONFIRMED);
        assertThat(updateReservation.getStatus()).as("예약은 승인완료 상태여야 한다").isEqualTo(CONFIRMED);
    }

    @Test
    @DisplayName("예약 취소 요청을 승인하는 경우 예약이 삭제된다")
    @Disabled("CANCEL 상태 삭제")
    public void confirmAllCanceledReservationThenDelete() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long canceledReservationId = 1L;
        Reservation canceledReservation = createDefaultReservation(resource, sameClubMember);
//        canceledReservation.cancel();

        given(reservationRepository.findAllById(List.of(canceledReservationId)))
                .willReturn(List.of(canceledReservation));

//        assert canceledReservation.getStatus() == CANCEL;

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(canceledReservationId));
        reservationService.confirmAllReservations(clubMember.getId(), requestDto);

        //then
        List<Reservation> deletedReservations = captureFromMockRepositoryWhenDeleteAll();
        assertThat(deletedReservations).as("취소상태의 예약이 삭제되어야 한다").contains(canceledReservation);
    }

    @Test
    @DisplayName("다른 클럽의 예약을 승인하려 하면 DIFFERENT_CLUB_EXCEPTION")
    public void confirmAllOtherClubReservationThenException() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long otherClubReservationId = 1L;
        Reservation otherClubReservation = createReservation(
                otherClubResource, otherClubMember, getPeriod(17, 20), "title", "usage", false
        );

        given(reservationRepository.findAllById(List.of(otherClubReservationId)))
                .willReturn(List.of(otherClubReservation));

        //when
        //then
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(otherClubReservationId));
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.confirmAllReservations(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 예약에 접근하면 DIFFERENT_CLUB_EXCEPTION를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("예약 요청 승인시 본인의 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void confirmAllByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));
        Long reservationId = 1L;

        //when
        //then
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(reservationId));
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.confirmAllReservations(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * rejectAllReservations
     */
    @Test
    @DisplayName("관리자는 승인 대기중인 예약을 거절할 수 있다.")
    public void rejectAllByAdmin() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));

        Reservation createReservation = createDefaultReservation(resource, sameClubMember);
        assert createReservation.getStatus() == REQUEST;

        Reservation updateReservation = createDefaultReservation(resource, sameClubMember);
        updateDefaultReservation(updateReservation);
        assert updateReservation.getStatus() == REQUEST;

        given(reservationRepository.findAllById(reservationIds))
                .willReturn(List.of(createReservation, updateReservation));

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(reservationIds);
        reservationService.rejectAllReservations(clubMember.getId(), requestDto);

        //then
        assertThat(createReservation.getStatus()).isEqualTo(REJECTED).as("거절된 예약은 REJECTED 상태여야 한다");
        assertThat(updateReservation.getStatus()).isEqualTo(REJECTED).as("거절된 예약은 REJECTED 상태여야 한다");
//        List<Reservation> deletedReservations = captureFromMockRepositoryWhenDeleteAll();
//        assertThat(deletedReservations).as("거절된 예약 요청은 삭제되어야 한다")
//                .containsExactlyInAnyOrder(createReservation, updateReservation);
    }

    @Test
    @DisplayName("권한이 있는 매니저는 승인 대기중인 예약을 거절할 수 있다.")
    public void rejectAllByManagerHasSCHEDULE_ALL() throws Exception {
        //given
        clubMember.changeRole(ClubRole.MANAGER);
        clubMember.updateClubAuthority(clubAuthority);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));

        Reservation createReservation = createDefaultReservation(resource, sameClubMember);
        assert createReservation.getStatus() == REQUEST;

        Reservation updateReservation = createDefaultReservation(resource, sameClubMember);
        updateDefaultReservation(updateReservation);
        assert updateReservation.getStatus() == REQUEST;

        given(reservationRepository.findAllById(reservationIds))
                .willReturn(List.of(createReservation, updateReservation));

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(reservationIds);
        reservationService.rejectAllReservations(clubMember.getId(), requestDto);

        //then
        assertThat(createReservation.getStatus()).isEqualTo(REJECTED);
        assertThat(updateReservation.getStatus()).isEqualTo(REJECTED);
//        List<Reservation> deletedReservations = captureFromMockRepositoryWhenDeleteAll();
//        assertThat(deletedReservations).as("거절된 예약 요청은 삭제되어야 한다")
//                .containsExactlyInAnyOrder(createReservation, updateReservation);
    }

    @Test
    @DisplayName("예약 취소 요청을 거절하는 경우 예약이 CONFIRMED 상태로 남는다")
    @Disabled("CANCEL 상태 사용 안 함.")
    public void rejectAllCanceledReservationThenRemain() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long canceledReservationId = 1L;
        Reservation canceledReservation = createDefaultReservation(resource, sameClubMember);
//        canceledReservation.cancel();

        given(reservationRepository.findAllById(List.of(canceledReservationId)))
                .willReturn(List.of(canceledReservation));

//        assert canceledReservation.getStatus() == CANCEL;

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(canceledReservationId));
        reservationService.rejectAllReservations(clubMember.getId(), requestDto);

        //then
        assertThat(canceledReservation.getStatus()).as("취소요청을 거절하면 예약은 CONFIRM 상태로 남아야 한다")
                .isEqualTo(CONFIRMED);
    }

    @Test
    @DisplayName("다른 클럽의 예약을 거절하려 하면 DIFFERENT_CLUB_EXCEPTION")
    public void rejectAllOtherClubReservationThenException() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long otherClubReservationId = 1L;
        Reservation otherClubReservation = createReservation(
                otherClubResource, otherClubMember, getPeriod(17, 20), "title", "usage", false
        );

        given(reservationRepository.findAllById(List.of(otherClubReservationId)))
                .willReturn(List.of(otherClubReservation));

        //when
        //then
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(otherClubReservationId));
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.rejectAllReservations(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 예약에 접근하면 DIFFERENT_CLUB_EXCEPTION를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("예약 요청 거절시 본인의 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void rejectAllByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));
        Long reservationId = 1L;

        //when
        //then
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(reservationId));
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.rejectAllReservations(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * findReservationById
     */
    @Test
    @DisplayName("사용자는 예약 id로 예약 정보를 조회할 수 있다.")
    public void findReservationById() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, clubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Request requestDto = new ReservationDto.Request(reservationId);
        ReservationDto.Response responseDto = reservationService.findReservationById(clubMember.getId(), requestDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getClubMemberId()).as("예약자 정보가 일치해야 한다").isEqualTo(reservation.getClubMember().getId());
        assertThat(responseDto.getResourceId()).as("예약한 리소스 정보가 일치해야 한다").isEqualTo(reservation.getResource().getId());
        assertThat(responseDto.getTitle()).as("예약 제목이 일치해야 한다").isEqualTo(reservation.getTitle());
        assertThat(responseDto.getUsage()).as("예약 용도가 일치해야 한다").isEqualTo(reservation.getUsage());
        assertThat(responseDto.isSharing()).as("공유 여부가 일치해야 한다").isEqualTo(reservation.isSharing());
        assertThat(responseDto.getStartDateTime()).as("예약 시작 시간이 일치해야 한다").isEqualTo(reservation.getPeriod().getStartDateTime());
        assertThat(responseDto.getEndDateTime()).as("예약 종료 시간이 일치해야 한다").isEqualTo(reservation.getPeriod().getEndDateTime());
        assertThat(responseDto.getStatus()).as("예약의 상태가 일치해야 한다").isEqualTo(reservation.getStatus().name());
    }

    @Test
    @DisplayName("다른 클럽의 예약을 조회하려면 DIFFERENT_CLUB_EXCEPTION")
    public void findOtherClubReservationByIdThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(otherClubResource, otherClubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        //then
        ReservationDto.Request requestDto = new ReservationDto.Request(reservationId);
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.findReservationById(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 예약에 접근하면 DIFFERENT_CLUB_EXCEPTION 예외를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("예약 조회시 예약 데이터가 없으면 RESERVATION_NOT_FOUND")
    public void findNoReservationByIdThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Request requestDto = new ReservationDto.Request(reservationId);
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.findReservationById(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("예약 데이터가 없으면 RESERVATION_NOT_FOUND 에외를 던진다")
                .isEqualTo(RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("예약 조회시 본인의 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void findReservationByIdByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));
        Long reservationId = 1L;

        //when
        //then
        ReservationDto.Request requestDto = new ReservationDto.Request(reservationId);
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.findReservationById(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * findAllReservationsByPeriod
     */
    @Test
    @DisplayName("사용자는 주어진 기간 안에 있는 모든 예약을 조회할 수 있다.")
    public void findAllReservationsByPeriod() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Reservation confirmed = createReservation(
                resource, clubMember, getPeriod(20, 21), "title1", "usage1", false);
        confirmed.confirm();
        Reservation unconfirmed = createReservation(
                resource, sameClubMember, getPeriod(21, 22), "title2", "usage2", false);
        List<Reservation> reservations = List.of(confirmed, unconfirmed);

        given(reservationRepository.findAllBetween(any(), any(), eq(resource.getId()))).willReturn(reservations);

        //when
        ReservationDto.Request requestDto = ReservationDto.Request.builder()
                .resourceId(resource.getId())
                .startDateTime(getTime(20))
                .endDateTime(getTime(22))
                .build();

        List<ReservationDto.Response> responseDto = reservationService.findAllReservationsByPeriod(clubMember.getId(), requestDto);

        //then
        List<String> reservationNames = responseDto.stream().map(ReservationDto.Response::getTitle).toList();
        assertThat(reservationNames).as("기간 내의 예약을 모두 포함해야 한다.")
                .containsAll(reservations.stream().map(Reservation::getTitle).toList());
    }

    @Test
    @DisplayName("다른 클럽의 예약을 조회하려 하면 DIFFERENT_CLUB_EXCEPTION")
    public void findAllOtherClubReservationsByPeriodThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Reservation confirmed = createReservation(
                otherClubResource, otherClubMember, getPeriod(20, 21), "title1", "usage1", false);
        confirmed.confirm();
        Reservation unconfirmed = createReservation(
                otherClubResource, otherClubMember, getPeriod(21, 22), "title2", "usage2", false);
        List<Reservation> reservations = List.of(confirmed, unconfirmed);

        given(reservationRepository.findAllBetween(any(), any(), eq(otherClubResource.getId()))).willReturn(reservations);

        //when
        //then
        ReservationDto.Request requestDto = ReservationDto.Request.builder()
                .resourceId(otherClubResource.getId())
                .startDateTime(getTime(20))
                .endDateTime(getTime(22))
                .build();
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.findAllReservationsByPeriod(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 예약에 접근하면 DIFFERENT_CLUB_EXCEPTION를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("기간으로 조회시 본인의 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void findAllReservationByPeriodWhenNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Request requestDto = ReservationDto.Request.builder()
                .resourceId(resource.getId())
                .startDateTime(getTime(20))
                .endDateTime(getTime(22))
                .build();
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.findAllReservationsByPeriod(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * findAllNotConfirmedReservations
     */
    @Test
    @DisplayName("관리자는 승인 대기중인 예약들을 조회할 수 있다.")
    public void findAllNotConfirmedReservationsByAdmin() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Reservation unconfirmed1 = createReservation(
                resource, clubMember, getPeriod(20, 21), "title1", "usage1", false);
        Reservation unconfirmed2 = createReservation(
                resource, sameClubMember, getPeriod(21, 22), "title2", "usage2", false);
        List<Reservation> reservations = List.of(unconfirmed1, unconfirmed2);

        given(reservationRepository.findAllNotConfirmed(resource.getId())).willReturn(reservations);

        //when
        ReservationDto.Request requestDto = ReservationDto.Request.builder().resourceId(resource.getId()).build();
        List<ReservationDto.Response> responseDto =
                reservationService.findAllNotConfirmedReservations(clubMember.getId(), requestDto);

        //then
        List<String> reservationNames = responseDto.stream().map(ReservationDto.Response::getTitle).toList();
        assertThat(reservationNames).as("해당 리소스의 승인되지 않은 예약들을 모두 포함해야 한다.")
                .containsAll(reservations.stream().map(Reservation::getTitle).toList());
    }

    @Test
    @DisplayName("권한이 있는 매니저는 승인 대기중인 예약들을 조회할 수 있다.")
    public void findAllNotConfirmedReservationsByManagerHasSCHEDULE_ALL() throws Exception {
        //given
        clubMember.changeRole(ClubRole.MANAGER);
        clubMember.updateClubAuthority(clubAuthority);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Reservation unconfirmed1 = createReservation(
                resource, clubMember, getPeriod(20, 21), "title1", "usage1", false);
        Reservation unconfirmed2 = createReservation(
                resource, sameClubMember, getPeriod(21, 22), "title2", "usage2", false);
        List<Reservation> reservations = List.of(unconfirmed1, unconfirmed2);

        given(reservationRepository.findAllNotConfirmed(resource.getId())).willReturn(reservations);

        //when
        ReservationDto.Request requestDto = ReservationDto.Request.builder().resourceId(resource.getId()).build();
        List<ReservationDto.Response> responseDto =
                reservationService.findAllNotConfirmedReservations(clubMember.getId(), requestDto);

        //then
        List<String> reservationNames = responseDto.stream().map(ReservationDto.Response::getTitle).toList();
        assertThat(reservationNames).as("해당 리소스의 승인되지 않은 예약들을 모두 포함해야 한다.")
                .containsAll(reservations.stream().map(Reservation::getTitle).toList());
    }

    @Test
    @DisplayName("다른 클럽의 승인 대기중인 예약을 조회하려 하면 DIFFERENT_CLUB_EXCEPTION")
    public void findAllNotConfirmedReservationWhenOtherClubThenException() throws Exception {
        //given
        clubMember.changeRole(ClubRole.ADMIN);
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Reservation unconfirmed1 = createReservation(
                otherClubResource, otherClubMember, getPeriod(20, 21), "title1", "usage1", false);
        Reservation unconfirmed2 = createReservation(
                otherClubResource, otherClubMember, getPeriod(21, 22), "title2", "usage2", false);
        List<Reservation> reservations = List.of(unconfirmed1, unconfirmed2);

        given(reservationRepository.findAllNotConfirmed(otherClubResource.getId())).willReturn(reservations);

        //when
        //then
        ReservationDto.Request requestDto = ReservationDto.Request.builder().resourceId(otherClubResource.getId()).build();
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.findAllNotConfirmedReservations(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 예약에 접근하면 DIFFERENT_CLUB_EXCEPTION를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    @DisplayName("승인 대기중인 예약 조회시 본인의 데이터가 없으면 CLUBMEMBER_NOT_FOUND")
    public void findAllNotConfirmedReservationsWhenNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Request requestDto = ReservationDto.Request.builder().resourceId(resource.getId()).build();
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.findAllNotConfirmedReservations(clubMember.getId(), requestDto));
        assertThat(exception.getErrorResult()).as("클럽 회원 데이터가 없으면 CLUBMEMBER_NOT_FOUND 예외를 던진다")
                .isEqualTo(CLUBMEMBER_NOT_FOUND);
    }


    /**
     * returnReservation
     */
    @Test
    @DisplayName("예약을 반환하면 예약의 반환 상태가 true가 된다.")
    public void testReturnReservation_Success() {
        // given
        Reservation reservation = createReservation(
                resource,clubMember, getPeriod(20, 21), "title1", "usage1", false);
        when(reservationRepository.findById(any())).thenReturn(Optional.ofNullable(reservation));
        when(clubMemberRepository.findById(clubMember.getId())).thenReturn(Optional.ofNullable(clubMember));
        ReservationDto.Return returnDto = ReservationDto.Return.builder().reservationId(reservation.getId()).build();

        // 실행
        reservationService.returnReservation(clubMember.getId(), returnDto);

        // 검증
        assertThat(reservation.isReturned()).isTrue(); // 예약이 반환 상태로 변경되었는지 확인
    }

    @Test
    @DisplayName("반환 요청자와 예약 정보의 클럽이 다르면 DIFFERENT_CLUB_EXCEPTION")
    public void testReturnReservation_DIFFERENT_CLUB_EXCEPTION() {
        // given
        Reservation reservation = createReservation(
                otherClubResource,otherClubMember, getPeriod(20, 21), "title1", "usage1", false);
        when(reservationRepository.findById(any())).thenReturn(Optional.ofNullable(reservation));
        when(clubMemberRepository.findById(clubMember.getId())).thenReturn(Optional.ofNullable(clubMember));
        ReservationDto.Return returnDto = ReservationDto.Return.builder().reservationId(reservation.getId()).build();


        // 실행
        // 검증
        BaseException exception = assertThrows(ServiceException.class,
                () -> reservationService.returnReservation(clubMember.getId(), returnDto));
        assertThat(exception.getErrorResult()).as("다른 클럽의 예약에 접근하면 DIFFERENT_CLUB_EXCEPTION를 던진다")
                .isEqualTo(DIFFERENT_CLUB_EXCEPTION);
    }

    @Test
    public void testBookableSpan() throws Exception
    {

        Long bookableSpan = 7L;
        System.out.println(LocalDate.now());
        LocalDate nowDate = LocalDate.of(2024, 5, 5);
        LocalDate endDate = LocalDate.of(2024, 5, 12);
        LocalDate limit = nowDate.plusDays(bookableSpan);

        assert !endDate.isAfter(limit);

        LocalDate nowDate2 = LocalDate.of(2024, 12, 31);
        LocalDate endDate2 = LocalDate.of(2025, 1, 7);
        LocalDate limit2 = nowDate2.plusDays(bookableSpan);

        assert !endDate2.isAfter(limit2);
    }


    /**
     * Argument capture method
     */
    private Reservation captureFromMockRepositoryWhenDelete() {
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        then(reservationRepository).should(atLeastOnce()).delete(captor.capture());
        return captor.getValue();
    }

    private List<Reservation> captureFromMockRepositoryWhenDeleteAll() {
        ArgumentCaptor<List<Reservation>> captor = ArgumentCaptor.forClass(List.class);
        then(reservationRepository).should(atLeastOnce()).deleteAll(captor.capture());
        return captor.getValue();
    }


    /**
     * ClubMember util method
     */
    private static ClubMember createClubMember(Club club, long value) {
        Member member = Member.builder().build();
        ClubMember clubMember = ClubMember.builder().club(club).member(member).build();
        ReflectionTestUtils.setField(clubMember, "id", value);
        clubMember.confirm();
        return clubMember;
    }

    /**
     * Resource util method
     */
    private static Resource createResource(Club club, long value) {
        Resource resource = Resource.builder().club(club).build();
        ReflectionTestUtils.setField(resource, "id", value);
        ReflectionTestUtils.setField(resource, "bookableSpan", 7L);
        return resource;
    }

    /**
     * Reservation util method
     */

    private Reservation createReservation(Resource resource, ClubMember clubMember, Period period,
                                          String title, String usage, boolean sharing) {
        return Reservation.builder()
                .resource(resource)
                .clubMember(clubMember)
                .period(period)
                .title(title)
                .usage(usage)
                .sharing(sharing)
                .build();
    }

    private static void updateDefaultReservation(Reservation updateReservation) {
        updateReservation.update("title", "usage",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);
    }

    private Reservation createDefaultReservation(Resource resource, ClubMember clubMember) {
        return createReservation(
                resource, clubMember, getPeriod(20, 21), "title", "usage", false);
    }


    /**
     * Time util method
     */
    private static LocalDateTime getTime(int hour) {
        return LocalDateTime.of(2023, 8, 10, hour, 0);
    }

    private static Period getPeriod(int start, int end) {
        return new Period(getTime(start), getTime(end));
    }


    /**
     * Dto util method
     */
    private ReservationDto.Create getCreateDto(
            Long resourceId, String title, String usage, boolean sharing, LocalDateTime start, LocalDateTime end,Long reservationOwnerId) {

        return ReservationDto.Create.builder()
                .reservationOwnerId(reservationOwnerId)
                .resourceId(resourceId)
                .title(title)
                .usage(usage)
                .sharing(sharing)
                .startDateTime(start)
                .endDateTime(end)
                .build();
    }

    private static ReservationDto.Update getUpdateDto(
            Long reservationId, Long resourceId, String title, String usage,
            boolean sharing, LocalDateTime start, LocalDateTime end) {

        return ReservationDto.Update.builder()
                .reservationId(reservationId)
                .resourceId(resourceId)
                .title(title)
                .usage(usage)
                .sharing(sharing)
                .startDateTime(start)
                .endDateTime(end)
                .build();
    }
}

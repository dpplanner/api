package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubAuthorityType;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.LockRepository;
import com.dp.dplanner.repository.ReservationRepository;
import com.dp.dplanner.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTests {

    @Mock
    ClubMemberRepository clubMemberRepository;
    @Mock
    ResourceRepository resourceRepository;
    @Mock
    LockRepository lockRepository;
    @Mock
    ReservationRepository reservationRepository;

    @InjectMocks
    ReservationService reservationService;

    Long clubMemberId;
    ClubMember clubMember;
    Long otherClubMemberId;
    ClubMember otherClubMember;
    Long resourceId;
    Resource resource;

    @BeforeEach
    void setUp() {
        Member member = Member.builder().build();
        Club club = Club.builder().build();
        Club otherClub = Club.builder().build();

        clubMemberId = 1L;
        clubMember = ClubMember.builder().club(club).member(member).build();
        ReflectionTestUtils.setField(clubMember, "id", clubMemberId);

        otherClubMemberId = 2L;
        otherClubMember = ClubMember.builder().club(otherClub).member(member).build();
        ReflectionTestUtils.setField(otherClubMember, "id", otherClubMemberId);

        resourceId = 1L;
        resource = Resource.builder().club(club).build();
        ReflectionTestUtils.setField(resource, "id", resourceId);
    }

    @Test
    @DisplayName("일반 회원은 승인 대기 상태의 예약을 생성할 수 있다.")
    public void createReservationRequestByUser() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resourceId)).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

        //when
        ReservationDto.Create createDto = getCreateDto(
                resourceId, "reservation", "usage", false, getTime(20), getTime(21));
        ReservationDto.Response responseDto = reservationService.createReservation(clubMemberId, createDto);

        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getClubMemberId()).as("예약자 정보가 일치해야 한다").isEqualTo(clubMemberId);
        assertThat(responseDto.getResourceId()).as("예약한 리소스 정보가 일치해야 한다").isEqualTo(resourceId);
        assertThat(responseDto.getTitle()).as("예약 제목이 일치해야 한다").isEqualTo(createDto.getTitle());
        assertThat(responseDto.getUsage()).as("예약 용도가 일치해야 한다").isEqualTo(createDto.getUsage());
        assertThat(responseDto.isSharing()).as("공유 여부가 일치해야 한다").isEqualTo(createDto.isSharing());
        assertThat(responseDto.getStartDateTime()).as("예약 시작 시간이 일치해야 한다").isEqualTo(createDto.getStartDateTime());
        assertThat(responseDto.getEndDateTime()).as("예약 종료 시간이 일치해야 한다").isEqualTo(createDto.getEndDateTime());
        assertThat(responseDto.isConfirmed()).as("예약은 승인 대기상태여야 한다").isFalse();
    }

    @Test
    @DisplayName("관리자는 승인된 예약을 생성할 수 있다")
    public void createReservationByAdmin() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resourceId)).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

        clubMember.setAdmin();

        //when
        ReservationDto.Create createDto = getCreateDto(
                resourceId, "reservation", "usage", false, getTime(20), getTime(21));
        ReservationDto.Response responseDto = reservationService.createReservation(clubMemberId, createDto);

        //then
        assertThat(responseDto.isConfirmed()).as("예약은 승인된 상태여야 한다").isTrue();
    }

    @Test
    @DisplayName("권한이 있는 매니저는 승인된 예약을 생성할 수 있다")
    public void createReservationByManagerHasSCHEDULE_ALL() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resourceId)).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

        ClubAuthority.createAuthorities(clubMember.getClub(), List.of(ClubAuthorityType.SCHEDULE_ALL));
        clubMember.setManager();

        //when
        ReservationDto.Create createDto = getCreateDto(
                resourceId, "reservation", "usage", false, getTime(20), getTime(21));
        ReservationDto.Response responseDto = reservationService.createReservation(clubMemberId, createDto);

        //then
        assertThat(responseDto.isConfirmed()).as("예약은 승인된 상태여야 한다").isTrue();
    }
    
    @Test
    @DisplayName("요청한 시간에 다른 예약이 있으면 IllegalStateException")
    public void requestReservationWhenPeriodOverlappedThenException() throws Exception {
        //given
        given(reservationRepository.existsBetween(any(), any(), eq(resourceId))).willReturn(true);

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resourceId, "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(clubMemberId, createDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("요청한 시간에 락이 걸려 있으면 IllegalStateException")
    public void requestReservationWhenLockedThenException() throws Exception {
        //given
        given(lockRepository.existsBetween(any(), any(), eq(resourceId))).willReturn(true);

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resourceId, "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(clubMemberId, createDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("다른 클럽의 회원이 요청하면 IllegalStateException")
    public void requestReservationByOtherClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(otherClubMemberId)).willReturn(Optional.ofNullable(otherClubMember));
        given(resourceRepository.findById(resourceId)).willReturn(Optional.ofNullable(resource));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resourceId, "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(otherClubMemberId, createDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("예약 요청시 회원 정보가 없으면 NoSuchElementException")
    public void requestReservationByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resourceId, "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(clubMemberId, createDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("예약 요청시 리소스 정보가 없으면 NoSuchElementException")
    public void requestReservationByNoResourceThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMemberId)).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resourceId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resourceId, "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(clubMemberId, createDto))
                .isInstanceOf(NoSuchElementException.class);
    }
    
    @Test
    @DisplayName("사용자는 본인의 예약을 수정할 수 있다.")
    public void updateReservationByUser() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createReservation(
                resource, clubMember, getPeriod(20, 21), "title", "usage", false);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resourceId, "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMemberId, updateDto);
        
        //then
        assertThat(responseDto).as("결과가 존재해야 한다").isNotNull();
        assertThat(responseDto.getClubMemberId()).as("예약자 정보가 일치해야 한다").isEqualTo(clubMemberId);
        assertThat(responseDto.getResourceId()).as("예약한 리소스 정보가 일치해야 한다").isEqualTo(resourceId);
        assertThat(responseDto.getTitle()).as("예약 제목이 일치해야 한다").isEqualTo(updateDto.getTitle());
        assertThat(responseDto.getUsage()).as("예약 용도가 일치해야 한다").isEqualTo(updateDto.getUsage());
        assertThat(responseDto.isSharing()).as("공유 여부가 일치해야 한다").isEqualTo(updateDto.isSharing());
        assertThat(responseDto.getStartDateTime()).as("예약 시작 시간이 일치해야 한다").isEqualTo(updateDto.getStartDateTime());
        assertThat(responseDto.getEndDateTime()).as("예약 종료 시간이 일치해야 한다").isEqualTo(updateDto.getEndDateTime());
        assertThat(responseDto.isConfirmed()).as("예약은 승인 대기상태여야 한다").isFalse();
    }

    @Test
    @DisplayName("사용자가 승인된 예약을 수정하면 승인 대기상태로 전환된다.")
    public void updateConfirmedReservationByUserThenNotConfirmed() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createReservation(
                resource, clubMember, getPeriod(20, 21), "title", "usage", false);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resourceId, "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMemberId, updateDto);

        //then
        assertThat(responseDto.isConfirmed()).as("예약은 승인 대기상태여야 한다").isFalse();
    }

    @Test
    @DisplayName("관리자가 승인된 예약을 수정시 승인대기를 하지 않는다")
    public void updateReservationByAdminThenConfirmed() throws Exception {
        //given
        clubMember.setAdmin();

        Long reservationId = 1L;
        Reservation reservation = createReservation(
                resource, clubMember, getPeriod(20, 21), "title", "usage", false);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resourceId, "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMemberId, updateDto);

        //then
        assertThat(responseDto.isConfirmed()).as("예약은 승인 완료상태여야 한다").isTrue();
    }

    @Test
    @DisplayName("권한이 있는 매니저가 승인된 예약을 수정시 승인대기를 하지 않는다")
    public void updateReservationByManagerHasSCHEDULE_ALLThenConfirmed() throws Exception {
        //given
        ClubAuthority.createAuthorities(clubMember.getClub(), List.of(ClubAuthorityType.SCHEDULE_ALL));
        clubMember.setManager();

        Long reservationId = 1L;
        Reservation reservation = createReservation(
                resource, clubMember, getPeriod(20, 21), "title", "usage", false);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resourceId, "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        ReservationDto.Response responseDto = reservationService.updateReservation(clubMemberId, updateDto);

        //then
        assertThat(responseDto.isConfirmed()).as("예약은 승인 완료상태여야 한다").isTrue();
    }

    @Test
    @DisplayName("예약 시간 수정시 다른 예약이 있으면 IllegalStateException")
    public void updateReservationWhenOverlappedWithOtherReservationThenException() throws Exception {
        //given
        Long reservationId = 1L;
        given(reservationRepository.existsOthersBetween(any(), any(), eq(resourceId), eq(reservationId)))
                .willReturn(true);

        //when
        //then
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resourceId, "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        assertThatThrownBy(() -> reservationService.updateReservation(clubMemberId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("수정하려는 시간에 락이 걸려있으면 IllegalStateException")
    public void updateReservationWhenLockedThenException() throws Exception {
        //given
        Long reservationId = 1L;
        given(lockRepository.existsBetween(any(), any(), eq(resourceId))).willReturn(true);

        //when
        //then
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resourceId, "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        assertThatThrownBy(() -> reservationService.updateReservation(clubMemberId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("다른 사람의 예약을 수정하려 하면 IllegalStateException")
    public void updateReservationByOtherClubMemberThenException() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createReservation(
                resource, clubMember, getPeriod(20, 21), "title", "usage", false);

        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        //then
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resourceId, "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        assertThatThrownBy(() -> reservationService.updateReservation(otherClubMemberId, updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("예약 수정시 예약 정보가 없으면 NoSuchElementException")
    public void updateReservationWhenNotExistsThenException() throws Exception {
        //given
        Long reservationId = 1L;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Update updateDto = getUpdateDto(
                reservationId, resourceId, "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        assertThatThrownBy(() -> reservationService.updateReservation(clubMemberId, updateDto))
                .isInstanceOf(NoSuchElementException.class);
    }




    private static LocalDateTime getTime(int hour) {
        return LocalDateTime.of(2023, 8, 10, hour, 0);
    }

    private static Period getPeriod(int start, int end) {
        return new Period(getTime(start), getTime(end));
    }
    private Reservation createReservation(Resource resource, ClubMember clubMember, Period period, String title, String usage, boolean sharing) {
        return Reservation.builder()
                .resource(resource)
                .clubMember(clubMember)
                .period(period)
                .title(title)
                .usage(usage)
                .sharing(sharing)
                .build();
    }

    private ReservationDto.Create getCreateDto(
            Long resourceId, String title, String usage, boolean sharing, LocalDateTime start, LocalDateTime end) {

        return ReservationDto.Create.builder()
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

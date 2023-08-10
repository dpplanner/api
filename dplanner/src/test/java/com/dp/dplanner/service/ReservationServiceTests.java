package com.dp.dplanner.service;

import com.dp.dplanner.domain.Member;
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
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(resourceId)
                .title("reservation")
                .usage("usage")
                .sharing(false)
                .startDateTime(LocalDateTime.of(2023, 8, 10, 20, 0))
                .endDateTime(LocalDateTime.of(2023, 8, 10, 21, 0))
                .build();

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
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(resourceId)
                .startDateTime(LocalDateTime.of(2023, 8, 10, 20, 0))
                .endDateTime(LocalDateTime.of(2023, 8, 10, 21, 0))
                .build();

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
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(resourceId)
                .startDateTime(LocalDateTime.of(2023, 8, 10, 20, 0))
                .endDateTime(LocalDateTime.of(2023, 8, 10, 21, 0))
                .build();

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
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(resourceId)
                .startDateTime(LocalDateTime.of(2023, 8, 10, 20, 0))
                .endDateTime(LocalDateTime.of(2023, 8, 10, 21, 0))
                .build();

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
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(resourceId)
                .startDateTime(LocalDateTime.of(2023, 8, 10, 20, 0))
                .endDateTime(LocalDateTime.of(2023, 8, 10, 21, 0))
                .build();

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
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(resourceId)
                .startDateTime(LocalDateTime.of(2023, 8, 10, 20, 0))
                .endDateTime(LocalDateTime.of(2023, 8, 10, 21, 0))
                .build();

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
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(resourceId)
                .startDateTime(LocalDateTime.of(2023, 8, 10, 20, 0))
                .endDateTime(LocalDateTime.of(2023, 8, 10, 21, 0))
                .build();

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
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(resourceId)
                .startDateTime(LocalDateTime.of(2023, 8, 10, 20, 0))
                .endDateTime(LocalDateTime.of(2023, 8, 10, 21, 0))
                .build();

        assertThatThrownBy(() -> reservationService.createReservation(clubMemberId, createDto))
                .isInstanceOf(NoSuchElementException.class);
    }

}

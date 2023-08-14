package com.dp.dplanner.service;

import com.dp.dplanner.domain.*;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.dp.dplanner.domain.ReservationStatus.*;
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


    ClubMember clubMember;
    ClubMember sameClubMember;
    Resource resource;

    ClubMember otherClubMember;
    Resource otherClubResource;

    @BeforeEach
    void setUp() {
        Club club = Club.builder().build();
        resource = createResource(club, 1L);
        clubMember = createClubMember(club, 1L);
        sameClubMember = createClubMember(club, 2L);

        Club otherClub = Club.builder().build();
        otherClubResource = createResource(otherClub, 2L);
        otherClubMember = createClubMember(otherClub, 3L);
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

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21));
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
        assertThat(responseDto.getStatus()).as("예약은 CREATE 상태여야 한다").isEqualTo(CREATE.name());
    }

    @Test
    @DisplayName("관리자는 승인된 예약을 생성할 수 있다")
    public void createReservationByAdmin() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));
        given(reservationRepository.save(any(Reservation.class))).willAnswer(invocation -> invocation.getArgument(0));

        clubMember.setAdmin();

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21));
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

        ClubAuthority.createAuthorities(clubMember.getClub(), List.of(ClubAuthorityType.SCHEDULE_ALL));
        clubMember.setManager();

        //when
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21));
        ReservationDto.Response responseDto = reservationService.createReservation(clubMember.getId(), createDto);

        //then
        assertThat(responseDto.getStatus()).as("예약은 승인된 상태여야 한다").isEqualTo(CONFIRMED.name());
    }
    
    @Test
    @DisplayName("요청한 시간에 다른 예약이 있으면 IllegalStateException")
    public void createReservationWhenPeriodOverlappedThenException() throws Exception {
        //given
        given(reservationRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(true);

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(clubMember.getId(), createDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("요청한 시간에 락이 걸려 있으면 IllegalStateException")
    public void createReservationWhenLockedThenException() throws Exception {
        //given
        given(lockRepository.existsBetween(any(), any(), eq(resource.getId()))).willReturn(true);

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(clubMember.getId(), createDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("다른 클럽의 회원이 요청하면 IllegalStateException")
    public void createReservationByOtherClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(otherClubMember.getId())).willReturn(Optional.ofNullable(otherClubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(resource));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(otherClubMember.getId(), createDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("예약 요청시 회원 정보가 없으면 NoSuchElementException")
    public void createReservationByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(clubMember.getId(), createDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("예약 요청시 리소스 정보가 없으면 NoSuchElementException")
    public void createReservationByNoResourceThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));
        given(resourceRepository.findById(resource.getId())).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Create createDto = getCreateDto(
                resource.getId(), "reservation", "usage", false, getTime(20), getTime(21));

        assertThatThrownBy(() -> reservationService.createReservation(clubMember.getId(), createDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * updateReservation
     */
    @Test
    @DisplayName("사용자는 본인의 예약을 수정할 수 있다.")
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
        assertThat(responseDto.getStatus()).as("예약은 UPDATE 상태여야 한다").isEqualTo(UPDATE.name());
    }

    @Test
    @DisplayName("사용자가 승인된 예약을 수정하면 승인 대기상태로 전환된다.")
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
        assertThat(responseDto.getStatus()).as("예약은 UPDATE 상태여야 한다").isEqualTo(UPDATE.name());
    }

    @Test
    @DisplayName("관리자가 승인된 예약을 수정시 승인대기를 하지 않는다")
    public void updateReservationByAdminThenConfirmed() throws Exception {
        //given
        clubMember.setAdmin();

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
    public void updateReservationByManagerHasSCHEDULE_ALLThenConfirmed() throws Exception {
        //given
        ClubAuthority.createAuthorities(clubMember.getClub(), List.of(ClubAuthorityType.SCHEDULE_ALL));
        clubMember.setManager();

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
    @DisplayName("예약 시간 수정시 다른 예약이 있으면 IllegalStateException")
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

        assertThatThrownBy(() -> reservationService.updateReservation(clubMember.getId(), updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("수정하려는 시간에 락이 걸려있으면 IllegalStateException")
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

        assertThatThrownBy(() -> reservationService.updateReservation(clubMember.getId(), updateDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("다른 사람의 예약을 수정하려 하면 IllegalStateException")
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

        assertThatThrownBy(() -> reservationService.updateReservation(otherClubMember.getId(), updateDto))
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
                reservationId, resource.getId(), "newTitle", "newUsage",
                false, getTime(20), getTime(22)
        );

        assertThatThrownBy(() -> reservationService.updateReservation(clubMember.getId(), updateDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * cancelReservation
     */
    @Test
    @DisplayName("일반 회원은 승인되지 않은 예약을 즉시 삭제할 수 있다.")
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
    @DisplayName("일반 회원이 승인된 예약을 취소하는 경우 승인 대기상태가 된다.")
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
        assertThat(reservation.getStatus()).as("예약은 CANCEL 상태여야 한다").isEqualTo(CANCEL);
    }
    
    @Test
    @DisplayName("일반 회원이 다른 회원의 예약을 취소하는 경우 IllegalStateException")
    public void cancelOtherClubMemberReservationThenException() throws Exception {
        //given
        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(resource, otherClubMember);
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        assertThatThrownBy(() -> reservationService.cancelReservation(clubMember.getId(), deleteDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("예약 취소시 예약 정보가 없으면 NoSuchElementException")
    public void cancelNoReservationThenException() throws Exception {
        //given
        Long reservationId = 1L;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        assertThatThrownBy(() -> reservationService.cancelReservation(clubMember.getId(), deleteDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * deleteReservation
     */
    @Test
    @DisplayName("관리자는 예약을 삭제할 수 있다.")
    public void deleteReservationByAdmin() throws Exception {
        //given
        clubMember.setAdmin();
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
        clubMember.setManager();
        ClubAuthority.createAuthorities(clubMember.getClub(), List.of(ClubAuthorityType.SCHEDULE_ALL));
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
        clubMember.setAdmin();
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
    @DisplayName("관리자가 다른 클럽의 예약을 삭제하려 하면 IllegalStateException")
    public void deleteOtherClubMemberReservationByAdminThenException() throws Exception {
        //given
        clubMember.setAdmin();
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        Reservation reservation = createDefaultReservation(otherClubResource, otherClubMember);
        reservation.confirm();
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(reservation));

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        assertThatThrownBy(() -> reservationService.deleteReservation(clubMember.getId(), deleteDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("예약 삭제시 회원 데이터가 없으면 NoSuchElementException")
    public void deleteReservationByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));

        Long reservationId = 1L;

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        assertThatThrownBy(() -> reservationService.deleteReservation(clubMember.getId(), deleteDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("예약 삭제시 예약 데이터가 없으면 NoSuchElementException")
    public void deleteNoReservationThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Delete deleteDto = new ReservationDto.Delete(reservationId);
        assertThatThrownBy(() -> reservationService.deleteReservation(clubMember.getId(), deleteDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * confirmAllReservations
     */
    @Test
    @DisplayName("관리자는 승인대기 상태의 예약을 승인할 수 있다.")
    public void confirmAllReservationsByAdmin() throws Exception {
        //given
        clubMember.setAdmin();
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));

        Reservation createReservation = createDefaultReservation(resource, sameClubMember);
        assert createReservation.getStatus() == CREATE;

        Reservation updateReservation = createDefaultReservation(resource, sameClubMember);
        updateDefaultReservation(updateReservation);
        assert updateReservation.getStatus() == UPDATE;

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
        clubMember.setManager();
        ClubAuthority.createAuthorities(clubMember.getClub(), List.of(ClubAuthorityType.SCHEDULE_ALL));
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));

        Reservation createReservation = createDefaultReservation(resource, sameClubMember);
        assert createReservation.getStatus() == CREATE;

        Reservation updateReservation = createDefaultReservation(resource, sameClubMember);
        updateDefaultReservation(updateReservation);
        assert updateReservation.getStatus() == UPDATE;

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
    public void confirmAllCanceledReservationThenDelete() throws Exception {
        //given
        clubMember.setAdmin();
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long canceledReservationId = 1L;
        Reservation canceledReservation = createDefaultReservation(resource, sameClubMember);
        canceledReservation.cancel();

        given(reservationRepository.findAllById(List.of(canceledReservationId)))
                .willReturn(List.of(canceledReservation));

        assert canceledReservation.getStatus() == CANCEL;

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(canceledReservationId));
        reservationService.confirmAllReservations(clubMember.getId(), requestDto);

        //then
        List<Reservation> deletedReservations = captureFromMockRepositoryWhenDeleteAll();
        assertThat(deletedReservations).as("취소상태의 예약이 삭제되어야 한다").contains(canceledReservation);
    }

    @Test
    @DisplayName("다른 클럽의 예약을 승인하려 하면 IllegalStateException")
    public void confirmAllOtherClubReservationThenException() throws Exception {
        //given
        clubMember.setAdmin();
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
        assertThatThrownBy(() -> reservationService.confirmAllReservations(clubMember.getId(), requestDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("예약 요청 승인시 본인의 데이터가 없으면 NoSuchElementException")
    public void confirmAllByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));
        Long reservationId = 1L;

        //when
        //then
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(reservationId));
        assertThatThrownBy(() -> reservationService.confirmAllReservations(clubMember.getId(), requestDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * rejectAllReservations
     */
    @Test
    @DisplayName("관리자는 승인 대기중인 예약을 거절할 수 있다.")
    public void rejectAllByAdmin() throws Exception {
        //given
        clubMember.setAdmin();
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));

        Reservation createReservation = createDefaultReservation(resource, sameClubMember);
        assert createReservation.getStatus() == CREATE;

        Reservation updateReservation = createDefaultReservation(resource, sameClubMember);
        updateDefaultReservation(updateReservation);
        assert updateReservation.getStatus() == UPDATE;

        given(reservationRepository.findAllById(reservationIds))
                .willReturn(List.of(createReservation, updateReservation));

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(reservationIds);
        reservationService.rejectAllReservations(clubMember.getId(), requestDto);

        //then
        List<Reservation> deletedReservations = captureFromMockRepositoryWhenDeleteAll();
        assertThat(deletedReservations).as("거절된 예약 요청은 삭제되어야 한다")
                .containsExactlyInAnyOrder(createReservation, updateReservation);
    }

    @Test
    @DisplayName("권한이 있는 매니저는 승인 대기중인 예약을 거절할 수 있다.")
    public void rejectAllByManagerHasSCHEDULE_ALL() throws Exception {
        //given
        clubMember.setManager();
        ClubAuthority.createAuthorities(clubMember.getClub(), List.of(ClubAuthorityType.SCHEDULE_ALL));
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        List<Long> reservationIds = new ArrayList<>(List.of(1L, 2L));

        Reservation createReservation = createDefaultReservation(resource, sameClubMember);
        assert createReservation.getStatus() == CREATE;

        Reservation updateReservation = createDefaultReservation(resource, sameClubMember);
        updateDefaultReservation(updateReservation);
        assert updateReservation.getStatus() == UPDATE;

        given(reservationRepository.findAllById(reservationIds))
                .willReturn(List.of(createReservation, updateReservation));

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(reservationIds);
        reservationService.rejectAllReservations(clubMember.getId(), requestDto);

        //then
        List<Reservation> deletedReservations = captureFromMockRepositoryWhenDeleteAll();
        assertThat(deletedReservations).as("거절된 예약 요청은 삭제되어야 한다")
                .containsExactlyInAnyOrder(createReservation, updateReservation);
    }

    @Test
    @DisplayName("예약 취소 요청을 거절하는 경우 예약이 CONFIRMED 상태로 남는다")
    public void rejectAllCanceledReservationThenRemain() throws Exception {
        //given
        clubMember.setAdmin();
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long canceledReservationId = 1L;
        Reservation canceledReservation = createDefaultReservation(resource, sameClubMember);
        canceledReservation.cancel();

        given(reservationRepository.findAllById(List.of(canceledReservationId)))
                .willReturn(List.of(canceledReservation));

        assert canceledReservation.getStatus() == CANCEL;

        //when
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(canceledReservationId));
        reservationService.rejectAllReservations(clubMember.getId(), requestDto);

        //then
        assertThat(canceledReservation.getStatus()).as("취소요청을 거절하면 예약은 CONFIRM 상태로 남아야 한다")
                .isEqualTo(CONFIRMED);
    }

    @Test
    @DisplayName("다른 클럽의 예약을 거절하려 하면 IllegalStateException")
    public void rejectAllOtherClubReservationThenException() throws Exception {
        //given
        clubMember.setAdmin();
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
        assertThatThrownBy(() -> reservationService.rejectAllReservations(clubMember.getId(), requestDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("예약 요청 거절시 본인의 데이터가 없으면 NoSuchElementException")
    public void rejectAllByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));
        Long reservationId = 1L;

        //when
        //then
        List<ReservationDto.Request> requestDto = ReservationDto.Request.ofList(List.of(reservationId));
        assertThatThrownBy(() -> reservationService.rejectAllReservations(clubMember.getId(), requestDto))
                .isInstanceOf(NoSuchElementException.class);
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
    @DisplayName("다른 클럽의 예약을 조회하려면 IllegalStateException")
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
        assertThatThrownBy(() -> reservationService.findReservationById(clubMember.getId(), requestDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("예약 조회시 예약 데이터가 없으면 NoSuchElementException")
    public void findNoReservationByIdThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(clubMember));

        Long reservationId = 1L;
        given(reservationRepository.findById(reservationId)).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Request requestDto = new ReservationDto.Request(reservationId);
        assertThatThrownBy(() -> reservationService.findReservationById(clubMember.getId(), requestDto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("예약 조회시 본인의 데이터가 없으면 NoSuchElementException")
    public void findReservationByIdByNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));
        Long reservationId = 1L;

        //when
        //then
        ReservationDto.Request requestDto = new ReservationDto.Request(reservationId);
        assertThatThrownBy(() -> reservationService.findReservationById(clubMember.getId(), requestDto))
                .isInstanceOf(NoSuchElementException.class);
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
    @DisplayName("다른 클럽의 예약을 조회하려 하면 IllegalStateException")
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
        assertThatThrownBy(() -> reservationService.findAllReservationsByPeriod(clubMember.getId(), requestDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("기간으로 조회시 본인의 데이터가 없으면 NoSuchElementException")
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
        assertThatThrownBy(() -> reservationService.findAllReservationsByPeriod(clubMember.getId(), requestDto))
                .isInstanceOf(NoSuchElementException.class);
    }


    /**
     * findAllNotConfirmedReservations
     */
    @Test
    @DisplayName("관리자는 승인 대기중인 예약들을 조회할 수 있다.")
    public void findAllNotConfirmedReservationsByAdmin() throws Exception {
        //given
        clubMember.setAdmin();
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
        clubMember.setManager();
        ClubAuthority.createAuthorities(clubMember.getClub(), List.of(ClubAuthorityType.SCHEDULE_ALL));
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
    @DisplayName("다른 클럽의 승인 대기중인 예약을 조회하려 하면 IllegalStateException")
    public void findAllNotConfirmedReservationWhenOtherClubThenException() throws Exception {
        //given
        clubMember.setAdmin();
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
        assertThatThrownBy(() -> reservationService.findAllNotConfirmedReservations(clubMember.getId(), requestDto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("승인 대기중인 예약 조회시 본인의 데이터가 없으면 NoSuchElementException")
    public void findAllNotConfirmedReservationsWhenNoClubMemberThenException() throws Exception {
        //given
        given(clubMemberRepository.findById(clubMember.getId())).willReturn(Optional.ofNullable(null));

        //when
        //then
        ReservationDto.Request requestDto = ReservationDto.Request.builder().resourceId(resource.getId()).build();
        assertThatThrownBy(() -> reservationService.findAllNotConfirmedReservations(clubMember.getId(), requestDto))
                .isInstanceOf(NoSuchElementException.class);
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
        return clubMember;
    }

    /**
     * Resource util method
     */
    private static Resource createResource(Club club, long value) {
        Resource resource = Resource.builder().club(club).build();
        ReflectionTestUtils.setField(resource, "id", value);
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

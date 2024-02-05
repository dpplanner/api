package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.domain.message.MessageConst;
import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.ReservationException;
import com.dp.dplanner.exception.ResourceException;
import com.dp.dplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.dp.dplanner.domain.ReservationStatus.*;
import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReservationService {

    private final MessageService messageService;
    private final ClubMemberRepository clubMemberRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final LockRepository lockRepository;
    private final ReservationInviteeRepository reservationInviteeRepository;


    @Transactional
    public ReservationDto.Response createReservation(Long clubMemberId, ReservationDto.Create createDto) {

        Long resourceId = createDto.getResourceId();
        LocalDateTime startDateTime = createDto.getStartDateTime();
        LocalDateTime endDateTime = createDto.getEndDateTime();

        if (!isReservable(resourceId, startDateTime, endDateTime)) {
            throw new ReservationException(RESERVATION_UNAVAILABLE);
        }

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isConfirmed()) {
            throw new ClubMemberException(CLUBMEMBER_NOT_CONFIRMED);
        }

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceException(RESOURCE_NOT_FOUND));

        if (!clubMember.isSameClub(resource)) {
            throw new ResourceException(DIFFERENT_CLUB_EXCEPTION);
        }

        Reservation reservation = reservationRepository.save(createDto.toEntity(clubMember, resource));

        confirmIfAuthorized(clubMember, reservation);

        // todo Message 보내기
        //  -> ClubMember 중 Schedule 권한 가지고 있는 멤버에게, 인앱 메시지
        if (!reservation.getStatus().equals(CONFIRMED)){
            List<Long> clubMemberIds = clubMemberRepository.findClubMemberByClubIdAndClubAuthorityTypesContaining(resource.getClub().getId(), SCHEDULE_ALL)
                    .stream().map(ClubMember::getId).toList();

            messageService.createPrivateMessage(clubMemberIds, new Message(MessageConst.RESERVATION_REQUEST, MessageConst.RESERVATION_REQUEST, "redirect_url"));
        }

        return ReservationDto.Response.of(reservation);
    }

    @Transactional
    public ReservationDto.Response updateReservation(Long clubMemberId, ReservationDto.Update updateDto) {

        Long reservationId = updateDto.getReservationId();
        Long resourceId = updateDto.getResourceId();
        LocalDateTime start = updateDto.getStartDateTime();
        LocalDateTime end = updateDto.getEndDateTime();

        if (!isUpdatable(reservationId, resourceId, start, end)) {
            throw new ReservationException(RESERVATION_UNAVAILABLE);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(RESERVATION_NOT_FOUND));

        if (!isReservationOwner(clubMemberId, reservation)) {
            throw new ReservationException(UPDATE_AUTHORIZATION_DENIED);
        }
        // 이미 확정된 상태에서, 예약 시간이 동일하다면 확정 상태 유지
        if (reservation.getStatus().equals(CONFIRMED) && reservation.getPeriod().equals(new Period(start, end))) {
                reservation.updateNotChangeStatus(
                        updateDto.getTitle(),
                        updateDto.getUsage(),
                        updateDto.getStartDateTime(),
                        updateDto.getEndDateTime(),
                        updateDto.isSharing()
                );
        }else{
            reservation.update(
                    updateDto.getTitle(),
                    updateDto.getUsage(),
                    updateDto.getStartDateTime(),
                    updateDto.getEndDateTime(),
                    updateDto.isSharing()
            );
        }

        confirmIfAuthorized(reservation.getClubMember(), reservation);

        return ReservationDto.Response.of(reservation);
    }

    @Transactional
    public void cancelReservation(Long clubMemberId, ReservationDto.Delete deleteDto) {

        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(() -> new ReservationException(RESERVATION_NOT_FOUND));

        if (!isReservationOwner(clubMemberId, reservation)) {
            throw new ReservationException(DELETE_AUTHORIZATION_DENIED);
        }


    // todo cancel 상태에서 한번 더 요청 보냉면 삭제됨. -> CANCEL 상태에도 그냥 그대로 CANCEL 추가
        if (reservation.isConfirmed() || reservation.getStatus().equals(CANCEL)) {
            reservation.cancel();
        }
        else {
            reservationRepository.delete(reservation);
        }
    }

    @Transactional
    @RequiredAuthority(authority = SCHEDULE_ALL)
    public void deleteReservation(Long managerId, ReservationDto.Delete deleteDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(() -> new ReservationException(RESERVATION_NOT_FOUND));

        if (!manager.isSameClub(reservation)) {
            throw new ReservationException(DIFFERENT_CLUB_EXCEPTION);
        }

        reservationRepository.delete(reservation);

        messageService.createPrivateMessage(List.of(reservation.getClubMember().getId()), new Message(MessageConst.RESERVATION_DISCARD, MessageConst.RESERVATION_DISCARD, "redirectUrl"));

    }

    @Transactional
    @RequiredAuthority(authority = SCHEDULE_ALL)
    public void confirmAllReservations(Long managerId, List<ReservationDto.Request> requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        List<Long> reservationIds = requestDto.stream()
                .map(ReservationDto.Request::getReservationId)
                .toList();

        List<Reservation> reservations = reservationRepository.findAllById(reservationIds);

        reservations.forEach(reservation -> {
            if (!manager.isSameClub(reservation)) {
                throw new ReservationException(DIFFERENT_CLUB_EXCEPTION);
            }
        });

        confirmReservations(reservations, true);

        messageService.createPrivateMessage(reservations.stream().map(reservation -> reservation.getClubMember().getId()).collect(Collectors.toList()),
                new Message(MessageConst.RESERVATION_REQUEST_APPROVED, MessageConst.RESERVATION_REQUEST_APPROVED, "redirectUrl"));

        // TODO Reservation Invitee에게 메시지 보내기
    }

    @Transactional
    @RequiredAuthority(authority = SCHEDULE_ALL)
    public void rejectAllReservations(Long managerId, List<ReservationDto.Request> requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        List<Long> reservationIds = requestDto.stream()
                .map(ReservationDto.Request::getReservationId)
                .toList();

        List<Reservation> reservations = reservationRepository.findAllById(reservationIds);

        reservations.forEach(reservation -> {
            if (!manager.isSameClub(reservation)) {
                throw new ReservationException(DIFFERENT_CLUB_EXCEPTION);
            }
        });

        confirmReservations(reservations, false);
        messageService.createPrivateMessage(reservations.stream().map(reservation -> reservation.getClubMember().getId()).collect(Collectors.toList()),
                new Message(MessageConst.RESERVATION_DISCARD, MessageConst.RESERVATION_DISCARD, "redirectUrl"));
        // todo 취소 사유 보내기
    }

    public ReservationDto.Response findReservationById(Long clubMemberId, ReservationDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(requestDto.getReservationId())
                .orElseThrow(() -> new ReservationException(RESERVATION_NOT_FOUND));

        if (!clubMember.isSameClub(reservation)) {
            throw new ReservationException(DIFFERENT_CLUB_EXCEPTION);
        }

        return ReservationDto.Response.of(reservation);
    }

    public List<ReservationDto.Response> findAllReservationsByPeriod(Long clubMemberId, ReservationDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));

        LocalDateTime start = requestDto.getStartDateTime();
        LocalDateTime end = requestDto.getEndDateTime();
        List<Reservation> reservations = reservationRepository.findAllBetween(start, end, requestDto.getResourceId());

        reservations.forEach(reservation -> {
            if (!clubMember.isSameClub(reservation)) {
                    throw new ReservationException(DIFFERENT_CLUB_EXCEPTION);
                }
        });

        return ReservationDto.Response.ofList(reservations);
    }

    @RequiredAuthority(authority = SCHEDULE_ALL)
    public List<ReservationDto.Response> findAllNotConfirmedReservations(Long managerId, ReservationDto.Request requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ClubMemberException(CLUBMEMBER_NOT_FOUND));
        List<Reservation> reservations = reservationRepository.findAllNotConfirmed(requestDto.getResourceId());

        reservations.forEach(reservation -> {
            if (!manager.isSameClub(reservation)) {
                throw new ReservationException(DIFFERENT_CLUB_EXCEPTION);
            }
        });

        return ReservationDto.Response.ofList(reservations);
    }


    /**
     * utility methods
     */
    private void confirmReservations(List<Reservation> reservations, boolean isConfirm) {

        Map<Boolean, List<Reservation>> partitioned = partitionCanceledReservations(reservations);
        List<Reservation> canceled = partitioned.get(true);
        List<Reservation> notCanceled = partitioned.get(false);

        if (isConfirm) {
            reservationRepository.deleteAll(canceled);
            notCanceled.forEach(Reservation::confirm);
        } else {
            reservationRepository.deleteAll(notCanceled);
            canceled.forEach(Reservation::confirm);
        }
    }

    private Map<Boolean, List<Reservation>> partitionCanceledReservations(List<Reservation> reservations) {

        return reservations.stream()
                .collect(Collectors.partitioningBy(
                        reservation -> reservation.getStatus().equals(CANCEL)
                ));
    }


    private static void confirmIfAuthorized(ClubMember clubMember, Reservation reservation) {
        if (clubMember.hasAuthority(SCHEDULE_ALL)) {
            reservation.confirm();
        }
    }

    //ToDO refactor
    private static boolean isReservationOwner(Long clubMemberId, Reservation reservation) {
        return reservation.getClubMember().getId().equals(clubMemberId);
    }


    private boolean isReservable(Long resourceId, LocalDateTime start, LocalDateTime end) {
        return !(reservationRepository.existsBetween(start, end, resourceId)
                || isLocked(resourceId, start, end));
    }

    private boolean isUpdatable(Long reservationId, Long resourceId, LocalDateTime start, LocalDateTime end) {
        return !(reservationRepository.existsOthersBetween(start, end, resourceId, reservationId)
        || isLocked(resourceId, start, end));
    }

    private boolean isLocked(Long resourceId, LocalDateTime start, LocalDateTime end) {
        return lockRepository.existsBetween(start, end, resourceId);
    }
}

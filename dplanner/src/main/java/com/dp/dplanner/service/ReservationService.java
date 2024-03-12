package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.*;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.dto.AttachmentDto;
import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.exception.ServiceException;
import com.dp.dplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.dp.dplanner.domain.ReservationStatus.*;
import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final MessageService messageService;
    private final ClubMemberRepository clubMemberRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final LockRepository lockRepository;
    private final AttachmentService attachmentService;
    private final ReservationInviteeRepository reservationInviteeRepository;
    private final Clock clock;

    public synchronized ReservationDto.Response createReservation(Long clubMemberId, ReservationDto.Create createDto) {
        Long resourceId = createDto.getResourceId();
        LocalDateTime startDateTime = createDto.getStartDateTime();
        LocalDateTime endDateTime = createDto.getEndDateTime();
        // 이미 예약이 있는지 검사
        if (reservationRepository.existsBetween(startDateTime, endDateTime, resourceId)) {
            throw new ServiceException(RESERVATION_UNAVAILABLE);
        }

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        if (!clubMember.isConfirmed()) {
            throw new ServiceException(CLUBMEMBER_NOT_CONFIRMED);
        }
        Reservation reservation;

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ServiceException(RESOURCE_NOT_FOUND));

        //일반 사용자 요청 처리
        if (!clubMember.hasAuthority(SCHEDULE_ALL)) {

            if (!clubMember.isSameClub(resource)) {
                throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
            }

            // 락 여부 조사
            if (isLocked(resourceId, startDateTime, endDateTime)) {
                throw new ServiceException(RESERVATION_UNAVAILABLE);
            }

            // 현재 시간과 예약 시작 시간 사이의 차이를 계산합니다.
            LocalDateTime now = LocalDateTime.now(clock);
            long secondsDifference = ChronoUnit.SECONDS.between(now, startDateTime);
            if (secondsDifference > 604800) { // 7 * 24 * 60 * 60 초
                throw new ServiceException(RESERVATION_UNAVAILABLE);
            }

            // 예약을 생성합니다.
            reservation = reservationRepository.saveAndFlush(createDto.toEntity(clubMember, resource));
            // 관리자에게 메시지를 전송합니다.
            List<Long> adminClubMemberIds = clubMemberRepository.findClubMemberByClubIdAndClubAuthorityTypesContaining(resource.getClub().getId(), SCHEDULE_ALL)
                    .stream().map(ClubMember::getId).toList();

            messageService.createPrivateMessage(adminClubMemberIds, Message.requestMessage());
            createReservationInvitee(createDto.getReservationInvitees(), clubMember, reservation);
        } else {
            // 사용자가 예약 관리 권한을 가진 경우
            if (!clubMember.isSameClub(resource)) {
                throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
            }
            // 예약을 생성하고 승인합니다.
            reservation = createDto.toEntity(clubMember, resource);
            reservation.confirm();
            reservationRepository.saveAndFlush(reservation);
            createReservationInvitee(createDto.getReservationInvitees(), clubMember, reservation);
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
            throw new ServiceException(RESERVATION_UNAVAILABLE);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        if (!isReservationOwner(clubMemberId, reservation)) {
            throw new ServiceException(UPDATE_AUTHORIZATION_DENIED);
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
            // 양방향 관계 reservation.invitees clear 필요
            reservation.clearInvitee();
            reservationInviteeRepository.deleteReservationInviteeByReservationId(reservationId);
            createReservationInvitee(updateDto.getReservationInvitees(), reservation.getClubMember(), reservation);
        }else{
            reservation.update(
                    updateDto.getTitle(),
                    updateDto.getUsage(),
                    updateDto.getStartDateTime(),
                    updateDto.getEndDateTime(),
                    updateDto.isSharing()
            );
            reservation.clearInvitee();
            reservationInviteeRepository.deleteReservationInviteeByReservationId(reservationId);
            createReservationInvitee(updateDto.getReservationInvitees(), reservation.getClubMember(), reservation);
        }

        confirmIfAuthorized(reservation.getClubMember(), reservation);

        return ReservationDto.Response.of(reservation);
    }

    /**
     * 예약 취소 요청 보내면 바로 삭제 (원래 기획 의도랑 달라짐, CANCELD-> 관리자 확인 후 삭제 였는데 바로 삭제로)
     * @param clubMemberId
     * @param deleteDto
     */
    @Transactional
    public void cancelReservation(Long clubMemberId, ReservationDto.Delete deleteDto) {

        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        if (!isReservationOwner(clubMemberId, reservation)) {
            throw new ServiceException(DELETE_AUTHORIZATION_DENIED);
        }

        reservationRepository.delete(reservation);

//        if (reservation.isConfirmed() || reservation.getStatus().equals(CANCELED)) {
//            reservation.cancel();
//        }
//        else {
//            reservationRepository.delete(reservation);
//        }
    }

    /**
     * 관리자 혹은 매니저가 직접 예약 삭제
     * @param managerId
     * @param deleteDto
     */
    @Transactional
    @RequiredAuthority(authority = SCHEDULE_ALL)
    public void deleteReservation(Long managerId, ReservationDto.Delete deleteDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        if (!manager.isSameClub(reservation)) {
            throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
        }

        messageService.createPrivateMessage(List.of(reservation.getClubMember().getId()),
                Message.discardMessage());
        reservationRepository.delete(reservation);

    }

    @Transactional
    @RequiredAuthority(authority = SCHEDULE_ALL)
    public void confirmAllReservations(Long managerId, List<ReservationDto.Request> requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        List<Long> reservationIds = requestDto.stream()
                .map(ReservationDto.Request::getReservationId)
                .toList();

        List<Reservation> reservations = reservationRepository.findAllById(reservationIds);

        reservations.forEach(reservation -> {
            if (!manager.isSameClub(reservation)) {
                throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
            }
            reservation.confirm();
        });

//        confirmReservations(reservations, true);

        messageService.createPrivateMessage(reservations.stream().map(reservation -> reservation.getClubMember().getId()).collect(Collectors.toList()),
               Message.confirmMessage());

        reservations.forEach(reservation -> {
            List<Long> inviteeIds = reservation.getReservationInvitees().stream()
                    .map(invitee -> invitee.getClubMember().getId())
                    .collect(Collectors.toList());
            messageService.createPrivateMessage(inviteeIds, Message.invitedMessage());
        });
    }

    @Transactional
    @RequiredAuthority(authority = SCHEDULE_ALL)
    public void rejectAllReservations(Long managerId, List<ReservationDto.Request> requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        List<Long> reservationIds = requestDto.stream()
                .map(ReservationDto.Request::getReservationId)
                .toList();

        List<Reservation> reservations = reservationRepository.findAllById(reservationIds);

        reservations.forEach(reservation -> {
            if (!manager.isSameClub(reservation)) {
                throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
            }
            reservation.reject();
        });

//        confirmReservations(reservations, false);
        messageService.createPrivateMessage(reservations.stream().map(reservation -> reservation.getClubMember().getId()).collect(Collectors.toList()),
                Message.discardMessage());
        // todo 거절 사유 보내기
    }
    @Transactional(readOnly = true)
    public ReservationDto.Response findReservationById(Long clubMemberId, ReservationDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(requestDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        if (!clubMember.isSameClub(reservation)) {
            throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
        }

        return ReservationDto.Response.of(reservation);
    }
    @Transactional(readOnly = true)
    public List<ReservationDto.Response> findAllReservationsByPeriod(Long clubMemberId, ReservationDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        LocalDateTime start = requestDto.getStartDateTime();
        LocalDateTime end = requestDto.getEndDateTime();
        List<Reservation> reservations = reservationRepository.findAllBetween(start, end, requestDto.getResourceId());

        reservations.forEach(reservation -> {
            System.out.println(reservation.getResource().getClub().getId() + " " + clubMember.getClub().getId());
            if (!clubMember.isSameClub(reservation)) {
                    throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
                }
        });

        return ReservationDto.Response.ofList(reservations);
    }
    @Transactional(readOnly = true)
    public List<ReservationDto.Response> findAllReservationsByPeriodAndStatus(Long clubMemberId, ReservationDto.Request requestDto, ReservationStatus status) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        LocalDateTime start = requestDto.getStartDateTime();
        LocalDateTime end = requestDto.getEndDateTime();
        List<Reservation> reservations = reservationRepository.findAllBetweenAndStatus(start, end, requestDto.getResourceId(),status);

        reservations.forEach(reservation -> {
            if (!clubMember.isSameClub(reservation)) {
                throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
            }
        });

        return ReservationDto.Response.ofList(reservations);
    }
    @Transactional(readOnly = true)
    public List<ReservationDto.Response> findAllReservationsByPeriodForScheduler(Long clubMemberId, ReservationDto.Request requestDto) {

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        LocalDateTime start = requestDto.getStartDateTime();
        LocalDateTime end = requestDto.getEndDateTime();
        List<Reservation> reservations = reservationRepository.findAllBetweenForScheduler(start, end, requestDto.getResourceId());

        reservations.forEach(reservation -> {
            if (!clubMember.isSameClub(reservation)) {
                throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
            }
        });

        return ReservationDto.Response.ofList(reservations);
    }

    @RequiredAuthority(authority = SCHEDULE_ALL)
    @Transactional(readOnly = true)
    public List<ReservationDto.Response> findAllNotConfirmedReservations(Long managerId, ReservationDto.Request requestDto) {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        List<Reservation> reservations = reservationRepository.findAllNotConfirmed(requestDto.getResourceId());

        reservations.forEach(reservation -> {
            if (!manager.isSameClub(reservation)) {
                throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
            }
        });

        return ReservationDto.Response.ofList(reservations);
    }

    @Transactional
    public ReservationDto.Response returnReservation(Long clubMemberId, ReservationDto.Return returnDto) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(returnDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        if (!reservation.isReturned()) {
            if (!clubMember.isSameClub(reservation)) {
                throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
            }

            if (returnDto.getFiles() != null && returnDto.getFiles().size() != 0) {
                attachmentService.createAttachmentReservation(
                        AttachmentDto.Create.builder()
                                .reservationId(reservation.getId())
                                .files(returnDto.getFiles())
                                .build());
            }
            reservation.returned(returnDto.getReturnMessage());

            List<Long> managerIds = clubMemberRepository.findClubMemberByClubIdAndClubAuthorityTypesContaining(clubMember.getClub().getId(), SCHEDULE_ALL).stream().map(ClubMember::getId).collect(Collectors.toList());
            messageService.createPrivateMessage(managerIds, Message.checkReturnMessage());

        }

        return ReservationDto.Response.of(reservation);

    }


    /**
     * utility methods
     */
//    private void confirmReservations(List<Reservation> reservations, boolean isConfirm) {
//
//        Map<Boolean, List<Reservation>> partitioned = partitionCanceledReservations(reservations);
//        List<Reservation> canceled = partitioned.get(true);
//        List<Reservation> notCanceled = partitioned.get(false);
//
//        if (isConfirm) {
//            reservationRepository.deleteAll(canceled);
//            notCanceled.forEach(Reservation::confirm);
//        } else {
//            reservationRepository.deleteAll(notCanceled);
//            canceled.forEach(Reservation::confirm);
//        }
//    }

//    private Map<Boolean, List<Reservation>> partitionCanceledReservations(List<Reservation> reservations) {
//
//        return reservations.stream()
//                .collect(Collectors.partitioningBy(
//                        reservation -> reservation.getStatus().equals(CANCELED)
//                ));
//    }


    private static void confirmIfAuthorized(ClubMember clubMember, Reservation reservation) {
        if (clubMember.hasAuthority(SCHEDULE_ALL)) {
            reservation.confirm();
        }
    }

    //ToDO refactor

    private static boolean isReservationOwner(Long clubMemberId, Reservation reservation) {
        return reservation.getClubMember().getId().equals(clubMemberId);
    }

//    private boolean isReservable(Long resourceId, LocalDateTime start, LocalDateTime end) {
//        return !(reservationRepository.existsBetween(start, end, resourceId)
//                || isLocked(resourceId, start, end));
//    }

    private boolean isUpdatable(Long reservationId, Long resourceId, LocalDateTime start, LocalDateTime end) {
        return !(reservationRepository.existsOthersBetween(start, end, resourceId, reservationId)
        || isLocked(resourceId, start, end));
    }

    private boolean isLocked(Long resourceId, LocalDateTime start, LocalDateTime end) {
        return lockRepository.existsBetween(start, end, resourceId);
    }

    /**
     *
     * @param clubMemberIds : invitee ids
     * @param inviter : inviter
     * @param reservation : reservation
     */
    private void createReservationInvitee(List<Long> clubMemberIds, ClubMember inviter, Reservation reservation) {
        List<ReservationInvitee> reservationInvitees = new ArrayList<>();
        clubMemberIds
                .forEach(inviteeId -> {
                    Optional<ClubMember> inviteeOptional = clubMemberRepository.findById(inviteeId);
                    if(inviteeOptional.isPresent()){
                        ClubMember invitee = inviteeOptional.get();
                        if(invitee.isSameClub(inviter)){
                            ReservationInvitee reservationInvitee = ReservationInvitee.builder()
                                    .clubMember(invitee)
                                    .reservation(reservation)
                                    .build();
                            reservationInvitees.add(reservationInvitee);
                        }
                    }
                });
        reservationInviteeRepository.saveAllAndFlush(reservationInvitees);
    }
}

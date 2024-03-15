package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.*;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.dto.AttachmentDto;
import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.exception.ServiceException;
import com.dp.dplanner.redis.RedisReservationService;
import com.dp.dplanner.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    private final RedisReservationService redisReservationService;
    private final MessageService messageService;
    private final ClubMemberRepository clubMemberRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final LockRepository lockRepository;
    private final AttachmentService attachmentService;
    private final ReservationInviteeRepository reservationInviteeRepository;
    private final Clock clock;

    @Transactional
    public ReservationDto.Response createReservation(Long clubMemberId, ReservationDto.Create createDto) {

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
            if (secondsDifference > 604800) { // 7 * 24 * 60 * 60 초, 일주일
                throw new ServiceException(RESERVATION_UNAVAILABLE);
            }
            // 레디스 확인
            Boolean cache = redisReservationService.saveReservation(startDateTime, endDateTime, resourceId);

            if(!cache){
                throw new ServiceException(RESERVATION_UNAVAILABLE);
            }
            // 예약을 생성합니다.
            reservation = reservationRepository.save(createDto.toEntity(clubMember, resource));
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
            // 레디스 확인

            Boolean cache = redisReservationService.saveReservation(startDateTime, endDateTime, resourceId);
            if(!cache){
                throw new ServiceException(RESERVATION_UNAVAILABLE);
            }
            // 예약을 생성하고 승인합니다.
            reservation = createDto.toEntity(clubMember, resource);
            reservation.confirm();
            reservationRepository.save(reservation);
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
        
// 예약 시간 수정 불가능하기 때문에 시간 및 락 검사할 필요 없음
//        if (!isUpdatable(reservationId, resourceId, start, end)) {
//            throw new ServiceException(RESERVATION_UNAVAILABLE);
//        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        if (!isReservationOwner(clubMemberId, reservation)) {
            throw new ServiceException(UPDATE_AUTHORIZATION_DENIED);
        }

        if (reservation.getPeriod().equals(new Period(start, end))) {
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
            // 예약 시간 수정 불가능
            throw new ServiceException(REQUEST_IS_INVALID);
        }

        confirmIfAuthorized(reservation.getClubMember(), reservation);

        return ReservationDto.Response.of(reservation);
    }

    @Transactional
    public void cancelReservation(Long clubMemberId, ReservationDto.Delete deleteDto) {

        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        if (!isReservationOwner(clubMemberId, reservation)) {
            throw new ServiceException(DELETE_AUTHORIZATION_DENIED);
        }
        redisReservationService.deleteReservation(reservation.getPeriod().getStartDateTime(), reservation.getPeriod().getEndDateTime(), reservation.getResource().getId());
        reservationRepository.delete(reservation);

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
        redisReservationService.deleteReservation(reservation.getPeriod().getStartDateTime(), reservation.getPeriod().getEndDateTime(), reservation.getResource().getId());
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
            redisReservationService.deleteReservation(reservation.getPeriod().getStartDateTime(), reservation.getPeriod().getEndDateTime(), reservation.getResource().getId());
        });

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
        reservationInviteeRepository.saveAll(reservationInvitees);
    }
}

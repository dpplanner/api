package com.dp.dplanner.service;

import com.dp.dplanner.service.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.*;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.adapter.dto.AttachmentDto;
import com.dp.dplanner.adapter.dto.ReservationDto;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ServiceException(RESOURCE_NOT_FOUND));
        Reservation reservation;

        checkIsConfirmed(clubMember);
        checkIsSameClub(clubMember, resource.getClub().getId());
        checkIsReserved(resourceId, startDateTime, endDateTime);
        checkIsReservedCache(resourceId, startDateTime, endDateTime);

        if (!clubMember.hasAuthority(SCHEDULE_ALL)) {
            //일반 사용자 요청 처리
            checkIsSameClubMember(clubMemberId, createDto.getReservationOwnerId());
            checkIsLocked(resourceId, startDateTime, endDateTime);
            checkIsInBookableSpan(resource, endDateTime);
            checkIsPastReservation(startDateTime, endDateTime);
            // 예약을 생성합니다.
            reservation = reservationRepository.save(createDto.toEntity(clubMember, resource));
            // 관리자에게 메시지를 전송합니다.
            List<ClubMember> adminClubMembers = clubMemberRepository.findClubMemberByClubIdAndClubAuthorityTypesContaining(resource.getClub().getId(), SCHEDULE_ALL);
            messageService.createPrivateMessage(adminClubMembers,
                    Message.requestMessage(
                            Message.MessageContentBuildDto.builder().
                                    clubMemberName(reservation.getClubMember().getName()).
                                    start(reservation.getPeriod().getStartDateTime()).
                                    end(reservation.getPeriod().getEndDateTime()).
                                    resourceName(reservation.getResource().getName()).
                                    info(String.valueOf(reservation.getId())).
                                    build()));

            createReservationInvitee(createDto.getReservationInvitees(), clubMember, reservation);
        } else if(clubMember.hasAuthority(SCHEDULE_ALL)) {
            // 예약 관리 권한을 가진 매니저 및 관리자 요청 처리
            // 예약을 생성하고 승인합니다.
            ClubMember reservationOwner = clubMemberRepository.findById(createDto.getReservationOwnerId())
                    .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
            checkIsSameClub(clubMember,reservationOwner.getClub().getId());

            reservation = createDto.toEntity(reservationOwner, resource);
            reservation.confirm();
            reservationRepository.save(reservation);
            List<ReservationInvitee> invitees = createReservationInvitee(createDto.getReservationInvitees(), reservationOwner, reservation);
            messageService.createPrivateMessage(invitees.stream().map(ReservationInvitee::getClubMember).toList(),
                    Message.invitedMessage(
                            Message.MessageContentBuildDto.builder().
                                    clubMemberName(reservation.getClubMember().getName()).
                                    start(reservation.getPeriod().getStartDateTime()).
                                    end(reservation.getPeriod().getEndDateTime()).
                                    resourceName(reservation.getResource().getName()).
                                    info(String.valueOf(reservation.getId())).
                                    build()));
        }else{
            throw new ServiceException(REQUEST_IS_INVALID);
        }
        return ReservationDto.Response.of(reservation);
    }

    @Transactional
    public ReservationDto.Response updateReservation(Long clubMemberId, ReservationDto.Update updateDto) {
        Long reservationId = updateDto.getReservationId();
        LocalDateTime start = updateDto.getStartDateTime();
        LocalDateTime end = updateDto.getEndDateTime();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        checkIsReservationOwner(clubMemberId, reservation);
        checkUpdateDTONullValidation(reservation, updateDto);
        if (reservation.getPeriod().equals(new Period(start, end))) {
                reservation.updateNotChangeStatus(
                        updateDto.getTitle(),
                        updateDto.getUsage(),
                        updateDto.isSharing(),
                        updateDto.getColor()
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
    @RequiredAuthority(authority = SCHEDULE_ALL)
    public ReservationDto.Response updateReservationOwner(Long clubMemberId, ReservationDto.UpdateOwner updateDto) {
        Long reservationId = updateDto.getReservationId();
        Long newReservationOwnerId = updateDto.getReservationOwnerId();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        ClubMember mananger = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        ClubMember newReservationOwner = clubMemberRepository.findById(newReservationOwnerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        checkIsSameClub(mananger,newReservationOwner.getClub().getId());

        reservation.updateOwner(newReservationOwner);

        return ReservationDto.Response.of(reservation);


    }

    @Transactional
    public void cancelReservation(Long clubMemberId, ReservationDto.Delete deleteDto) {
        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));
        checkIsReservationOwner(clubMemberId, reservation);
        redisReservationService.deleteReservation(reservation.getPeriod().getStartDateTime(), reservation.getPeriod().getEndDateTime(), reservation.getResource().getId());
        reservationRepository.delete(reservation);

    }

    @Transactional
    @RequiredAuthority(authority = SCHEDULE_ALL)
    public void deleteReservation(Long managerId, ReservationDto.Delete deleteDto) {
        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        checkIsSameClub(manager, reservation.getResource().getClub().getId());

        messageService.createPrivateMessage(List.of(reservation.getClubMember()),
                Message.cancelMessage(
                        Message.MessageContentBuildDto.builder().
                                clubMemberName(reservation.getClubMember().getName()).
                                start(reservation.getPeriod().getStartDateTime()).
                                end(reservation.getPeriod().getEndDateTime()).
                                resourceName(reservation.getResource().getName()).
                                build()));

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
            checkIsSameClub(manager,reservation.getResource().getClub().getId());
            reservation.confirm();
        });

        for (Reservation reservation : reservations) {
            messageService.createPrivateMessage(List.of(reservation.getClubMember()),
                    Message.confirmMessage(
                            Message.MessageContentBuildDto.builder().
                                    clubMemberName(reservation.getClubMember().getName()).
                                    start(reservation.getPeriod().getStartDateTime()).
                                    end(reservation.getPeriod().getEndDateTime()).
                                    resourceName(reservation.getResource().getName()).
                                    info(String.valueOf(reservation.getId())).
                                    build()));

            List<ClubMember> invitees = reservation.getReservationInvitees().stream()
                    .map(ReservationInvitee::getClubMember)
                    .collect(Collectors.toList());
            messageService.createPrivateMessage(invitees,
                    Message.invitedMessage(
                            Message.MessageContentBuildDto.builder().
                                    clubMemberName(reservation.getClubMember().getName()).
                                    start(reservation.getPeriod().getStartDateTime()).
                                    end(reservation.getPeriod().getEndDateTime()).
                                    resourceName(reservation.getResource().getName()).
                                    info(String.valueOf(reservation.getId())).
                                    build()));
        }
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
            checkIsSameClub(manager,reservation.getResource().getClub().getId());
            reservation.reject(requestDto.get(0).getRejectMessage());
            redisReservationService.deleteReservation(reservation.getPeriod().getStartDateTime(), reservation.getPeriod().getEndDateTime(), reservation.getResource().getId());
        });

        for (Reservation reservation : reservations) {
            messageService.createPrivateMessage(List.of(reservation.getClubMember()),
                    Message.rejectMessage(
                            Message.MessageContentBuildDto.builder().
                                    clubMemberName(reservation.getClubMember().getName()).
                                    start(reservation.getPeriod().getStartDateTime()).
                                    end(reservation.getPeriod().getEndDateTime()).
                                    resourceName(reservation.getResource().getName()).
                                    info(String.valueOf(reservation.getId())).
                                    build()));
        }
    }

    public ReservationDto.SliceResponse findMyReservationsUpComing(Long clubMemberId, Pageable pageable) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUB_NOT_FOUND));
        Pageable upComing = PageRequest.of(pageable.getPageNumber(), 100, Sort.by(Sort.Direction.ASC, "period.startDateTime"));
        LocalDateTime now = LocalDateTime.now();

        Slice<Reservation> reservations = reservationRepository.findMyReservationsAfter(clubMember.getId(), now, upComing);

        return new ReservationDto.SliceResponse(ReservationDto.Response.ofList(reservations.getContent()), upComing, reservations.hasNext());
    }

    public ReservationDto.SliceResponse findMyReservationsPrevious(Long clubMemberId, Pageable pageable) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUB_NOT_FOUND));
        Pageable previous= PageRequest.of(pageable.getPageNumber(),100, Sort.by(Sort.Direction.DESC, "period.startDateTime"));
        LocalDateTime now = LocalDateTime.now();

        Slice<Reservation> reservations = reservationRepository.findMyReservationsBefore(clubMember.getId(), now, previous);

        return new ReservationDto.SliceResponse(ReservationDto.Response.ofList(reservations.getContent()), previous, reservations.hasNext());

    }

    public ReservationDto.SliceResponse findMyReservationsReject(Long clubMemberId, Pageable pageable) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUB_NOT_FOUND));
        Pageable pageRequest= PageRequest.of(pageable.getPageNumber(),100, Sort.by(Sort.Direction.DESC, "period.startDateTime"));

        Slice<Reservation> reservations = reservationRepository.findMyReservationsStatus(clubMember.getId(), ReservationStatus.REJECTED, pageRequest);

        return new ReservationDto.SliceResponse(ReservationDto.Response.ofList(reservations.getContent()), pageRequest, reservations.hasNext());

    }


    @Transactional(readOnly = true)
    public ReservationDto.Response findReservationById(Long clubMemberId, ReservationDto.Request requestDto) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(requestDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        checkIsSameClub(clubMember,reservation.getResource().getClub().getId());

        return ReservationDto.Response.of(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationDto.Response> findAllReservationsByPeriod(Long clubMemberId, ReservationDto.Request requestDto) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        LocalDateTime start = requestDto.getStartDateTime();
        LocalDateTime end = requestDto.getEndDateTime();
        List<Reservation> reservations = reservationRepository.findAllBetween(start, end, requestDto.getResourceId());

        reservations.forEach(reservation -> checkIsSameClub(clubMember,reservation.getResource().getClub().getId()));

        return ReservationDto.Response.ofList(reservations);
    }

    @Transactional(readOnly = true)
    public List<ReservationDto.Response> findAllReservationsForScheduler(Long clubMemberId, ReservationDto.Request requestDto) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        Resource resource = resourceRepository.findById(requestDto.getResourceId())
                .orElseThrow(() -> new ServiceException(RESOURCE_NOT_FOUND));

        checkIsSameClub(clubMember, resource.getClub().getId());

        LocalDateTime start = requestDto.getStartDateTime();
        LocalDateTime end = requestDto.getEndDateTime();
        List<Reservation> reservations = reservationRepository.findAllBetweenForScheduler(start, end, requestDto.getResourceId());

        return ReservationDto.Response.ofList(reservations);
    }

    @RequiredAuthority(authority = SCHEDULE_ALL)
    @Transactional(readOnly = true)
    public ReservationDto.SliceResponse findAllReservationsRequest(Long managerId, ReservationDto.Request requestDto, String status, Pageable pageable) {
        Pageable pageableAmin = PageRequest.of(pageable.getPageNumber(),100, Sort.by(Sort.Direction.DESC, "period.startDateTime"));

        Slice<Reservation> reservations = reservationRepository.findReservationsAdmin(requestDto.getClubId(), ReservationStatus.REQUEST, pageableAmin);

        return new ReservationDto.SliceResponse(ReservationDto.Response.ofList(reservations.getContent()), pageableAmin, reservations.hasNext());
    }

    @RequiredAuthority(authority = SCHEDULE_ALL)
    @Transactional(readOnly = true)
    public ReservationDto.SliceResponse findAllReservationsRejected(Long managerId, ReservationDto.Request requestDto, String status, Pageable pageable) {
        Pageable pageableAmin = PageRequest.of(pageable.getPageNumber(),100, Sort.by(Sort.Direction.DESC, "period.startDateTime"));

        Slice<Reservation> reservations = reservationRepository.findReservationsAdmin(requestDto.getClubId(), ReservationStatus.REJECTED, pageableAmin);

        return new ReservationDto.SliceResponse(ReservationDto.Response.ofList(reservations.getContent()), pageableAmin, reservations.hasNext());
    }

    @RequiredAuthority(authority = RETURN_MSG_READ)
    @Transactional(readOnly = true)
    public ReservationDto.SliceResponse findAllReservationsConfirmed(Long managerId, ReservationDto.Request requestDto, String status, Pageable pageable) {
        Pageable pageableAmin = PageRequest.of(pageable.getPageNumber(),100, Sort.by(Sort.Direction.DESC, "period.startDateTime"));

        Slice<Reservation> reservations = reservationRepository.findReservationsAdmin(requestDto.getClubId(), ReservationStatus.CONFIRMED, pageableAmin);
        System.out.println(reservations.getContent().size());
        return new ReservationDto.SliceResponse(ReservationDto.Response.ofList(reservations.getContent()), pageableAmin, reservations.hasNext());
    }

    @Transactional
    public ReservationDto.Response returnReservation(Long clubMemberId, ReservationDto.Return returnDto) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(returnDto.getReservationId())
                .orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        if (!reservation.isReturned()) {
            checkIsSameClub(clubMember, reservation.getResource().getClub().getId());

            if (returnDto.getFiles() != null && returnDto.getFiles().size() != 0) {
                attachmentService.createAttachmentReservation(
                        AttachmentDto.Create.builder()
                                .reservationId(reservation.getId())
                                .files(returnDto.getFiles())
                                .build());
            }
            reservation.returned(returnDto.getReturnMessage());

            List<ClubMember> managers = clubMemberRepository.findClubMemberByClubIdAndClubAuthorityTypesContaining(clubMember.getClub().getId(), RETURN_MSG_READ);
            messageService.createPrivateMessage(managers,
                    Message.checkReturnMessage(
                            Message.MessageContentBuildDto.builder().
                                    clubMemberName(reservation.getClubMember().getName()).
                                    start(reservation.getPeriod().getStartDateTime()).
                                    end(reservation.getPeriod().getEndDateTime()).
                                    resourceName(reservation.getResource().getName()).
                                    info(String.valueOf(reservation.getId())).
                                    build()));

        }
        return ReservationDto.Response.of(reservation);
    }



    /**
     * 같은 클럽인지 검사
     */
    private static void checkIsSameClub(ClubMember clubMember, Long targetClubId) {
        if (!clubMember.isSameClub(targetClubId)) {
            throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
        }
    }

    /**
     * 클럽 가입 상태 검사
     */
    private static void checkIsConfirmed(ClubMember clubMember) {
        if (!clubMember.getIsConfirmed()) {
            throw new ServiceException(CLUBMEMBER_NOT_CONFIRMED);
        }
    }
    private static void confirmIfAuthorized(ClubMember clubMember, Reservation reservation) {
        if (clubMember.hasAuthority(SCHEDULE_ALL)) {
            reservation.confirm();
        }
    }
    /**
     * 리소스의 bookableSpan 유효성 검사
     */
    private void checkIsInBookableSpan(Resource resource, LocalDateTime endDateTime) {
        Long bookableSpan = resource.getBookableSpan();
        LocalDate nowDate = LocalDate.now(clock);
        LocalDate endDate = endDateTime.toLocalDate();
        LocalDate limit = nowDate.plusDays(bookableSpan);
        if ((endDate.isAfter(limit))) {
            throw new ServiceException("BookableSpan Validation Error",400);
        }
    }
    /**
     *  락 여부 검사
     */
    private void checkIsLocked(Long resourceId, LocalDateTime start, LocalDateTime end) {
        if(lockRepository.existsBetween(start, end, resourceId)){
            throw new ServiceException(RESERVATION_UNAVAILABLE);
        }
    }
    /**
     * 데이터베이스에 이미 예약이 있는지 검사
     */
    private void checkIsReserved(Long resourceId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (reservationRepository.existsBetween(startDateTime, endDateTime, resourceId)) {
            throw new ServiceException(RESERVATION_UNAVAILABLE);
        }
    }
    /**
     * 캐시에 이미 예약이 있는지 검사
     */
    private void checkIsReservedCache(Long resourceId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if(!redisReservationService.saveReservation(startDateTime, endDateTime, resourceId)){
            throw new ServiceException(RESERVATION_UNAVAILABLE);
        }
    }
    /**
     * 예약 주인인지 검사
     */
    private static void checkIsReservationOwner(Long clubMemberId, Reservation reservation) {
        if (!reservation.getClubMember().getId().equals(clubMemberId)) {
            throw new ServiceException(AUTHORIZATION_DENIED);
        }
    }

    /**
     * 요청한 예약 시간이 과거 시간인지 검사
     */
    private void checkIsPastReservation(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (endDateTime.isBefore(now) || endDateTime.isEqual(now)) {
            throw new ServiceException(REQUEST_IS_INVALID);
        }
    }

    /**
     * 일반 사용자는 예약시 본인이 예약 주인이 되어야 함.
     */
    private void checkIsSameClubMember(Long clubMemberId, Long reservationOwnerId) {
        if (!clubMemberId.equals(reservationOwnerId)) {
            throw new ServiceException(REQUEST_IS_INVALID);
        }
    }

    private void checkUpdateDTONullValidation(Reservation reservation, ReservationDto.Update updateDto) {

        if (ObjectUtils.isEmpty(updateDto.isSharing())) {
            updateDto.setSharing(reservation.isSharing());
        }

        if (ObjectUtils.isEmpty(updateDto.getTitle())) {
            updateDto.setTitle(reservation.getTitle());
        }

        if (ObjectUtils.isEmpty(updateDto.getUsage())) {
            updateDto.setUsage(reservation.getUsage());
        }

        if (ObjectUtils.isEmpty(updateDto.getColor())) {
            updateDto.setColor(reservation.getColor());
        }
    }

    /**
     * @param clubMemberIds : invitee ids
     * @param inviter       : inviter
     * @param reservation   : reservation
     */
    private List<ReservationInvitee> createReservationInvitee(List<Long> clubMemberIds, ClubMember inviter, Reservation reservation) {
        List<ReservationInvitee> reservationInvitees = new ArrayList<>();
        clubMemberIds
                .forEach(inviteeId -> {
                    Optional<ClubMember> inviteeOptional = clubMemberRepository.findById(inviteeId);
                    if(inviteeOptional.isPresent()){
                        ClubMember invitee = inviteeOptional.get();
                        if(invitee.isSameClub(inviter.getClub().getId())){
                            ReservationInvitee reservationInvitee = ReservationInvitee.builder()
                                    .clubMember(invitee)
                                    .reservation(reservation)
                                    .build();
                            reservationInvitees.add(reservationInvitee);
                        }
                    }
                });
        return reservationInviteeRepository.saveAll(reservationInvitees);
    }
}

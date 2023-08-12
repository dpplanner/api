package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.ReservationStatus;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.LockRepository;
import com.dp.dplanner.repository.ReservationRepository;
import com.dp.dplanner.repository.ResourceRepository;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.dp.dplanner.domain.ReservationStatus.*;
import static com.dp.dplanner.domain.club.ClubAuthorityType.*;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ClubMemberRepository clubMemberRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final LockRepository lockRepository;


    public ReservationDto.Response createReservation(Long clubMemberId, ReservationDto.Create createDto)
        throws IllegalStateException, NoSuchElementException{

        Long resourceId = createDto.getResourceId();
        LocalDateTime startDateTime = createDto.getStartDateTime();
        LocalDateTime endDateTime = createDto.getEndDateTime();
        
        if (!isReservable(resourceId, startDateTime, endDateTime)) {
            throw new IllegalStateException();
        }

        ClubMember clubMember = clubMemberRepository.findById(clubMemberId)
                .orElseThrow(NoSuchElementException::new);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(NoSuchElementException::new);

        if (!clubMember.isSameClub(resource)) {
            throw new IllegalStateException();
        }

        Reservation reservation = reservationRepository.save(createDto.toEntity(clubMember, resource));

        confirmIfAuthorized(clubMember, reservation);

        return ReservationDto.Response.of(reservation);
    }

    public ReservationDto.Response updateReservation(Long clubMemberId, ReservationDto.Update updateDto)
        throws IllegalStateException, NoSuchElementException {

        Long reservationId = updateDto.getReservationId();
        Long resourceId = updateDto.getResourceId();
        LocalDateTime start = updateDto.getStartDateTime();
        LocalDateTime end = updateDto.getEndDateTime();

        if (!isUpdatable(reservationId, resourceId, start, end)) {
            throw new IllegalStateException();
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(NoSuchElementException::new);

        if (!isReservationOwner(clubMemberId, reservation)) {
            throw new IllegalStateException();
        }

        reservation.update(
                updateDto.getTitle(),
                updateDto.getUsage(),
                updateDto.getStartDateTime(),
                updateDto.getEndDateTime(),
                updateDto.isSharing()
        );

        confirmIfAuthorized(reservation.getClubMember(), reservation);

        return ReservationDto.Response.of(reservation);
    }

    public void cancelReservation(Long clubMemberId, ReservationDto.Delete deleteDto)
        throws IllegalStateException, NoSuchElementException {

        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(NoSuchElementException::new);

        if (!isReservationOwner(clubMemberId, reservation)) {
            throw new IllegalStateException();
        }

        if (reservation.isConfirmed()) {
            reservation.cancel();
        } else {
            reservationRepository.delete(reservation);
        }
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public void deleteReservation(Long managerId, ReservationDto.Delete deleteDto)
        throws IllegalStateException, NoSuchElementException {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        Reservation reservation = reservationRepository.findById(deleteDto.getReservationId())
                .orElseThrow(NoSuchElementException::new);

        throwIfOtherClubReservation(manager, reservation, new IllegalStateException());

        reservationRepository.delete(reservation);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public void confirmAllReservations(Long managerId, List<ReservationDto.Request> requestDto)
        throws IllegalStateException, NoSuchElementException {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        List<Long> reservationIds = requestDto.stream()
                .map(ReservationDto.Request::getReservationId)
                .toList();

        List<Reservation> reservations = reservationRepository.findAllById(reservationIds);

        reservations.forEach(reservation ->
                throwIfOtherClubReservation(manager, reservation, new IllegalStateException()));

        confirmReservations(reservations, true);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public void rejectAllReservations(Long managerId, List<ReservationDto.Request> requestDto)
        throws IllegalStateException, NoSuchElementException {

        ClubMember manager = clubMemberRepository.findById(managerId)
                .orElseThrow(NoSuchElementException::new);

        List<Long> reservationIds = requestDto.stream()
                .map(ReservationDto.Request::getReservationId)
                .toList();

        List<Reservation> reservations = reservationRepository.findAllById(reservationIds);

        reservations.forEach(
                reservation -> throwIfOtherClubReservation(manager, reservation, new IllegalStateException()));

        confirmReservations(reservations, false);
    }






    private void confirmReservations(List<Reservation> reservations, boolean isConfirm) {
        List<Reservation> canceled = extractCanceledReservations(reservations);
        List<Reservation> notCanceled = extractNotCanceledReservations(reservations);

        if (isConfirm) {
            reservationRepository.deleteAll(canceled);
            notCanceled.forEach(Reservation::confirm);
        } else {
            reservationRepository.deleteAll(notCanceled);
            canceled.forEach(Reservation::confirm);
        }
    }

    private static List<Reservation> extractNotCanceledReservations(List<Reservation> reservations) {
        return reservations.stream()
                .filter(reservation -> !reservation.getStatus().equals(CANCEL))
                .toList();
    }

    private static List<Reservation> extractCanceledReservations(List<Reservation> reservations) {
        return reservations.stream()
                .filter(reservation -> reservation.getStatus().equals(CANCEL))
                .toList();
    }

    private static boolean isReservationOwner(Long clubMemberId, Reservation reservation) {
        return reservation.getClubMember().getId().equals(clubMemberId);
    }

    private static void confirmIfAuthorized(ClubMember clubMember, Reservation reservation) {
        if (clubMember.hasAuthority(SCHEDULE_ALL)) {
            reservation.confirm();
        }
    }

    private static void throwIfOtherClubReservation(ClubMember manager, Reservation reservation, IllegalStateException e) {
        if (!manager.isSameClub(reservation.getResource())) {
            throw e;
        }
    }

    private boolean isReservable(Long resourceId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return !(reservationRepository.existsBetween(startDateTime, endDateTime, resourceId)
                || isLocked(resourceId, startDateTime, endDateTime));
    }

    private boolean isUpdatable(Long reservationId, Long resourceId, LocalDateTime start, LocalDateTime end) {
        return !(reservationRepository.existsOthersBetween(start, end, resourceId, reservationId)
        || isLocked(resourceId, start, end));
    }

    private boolean isLocked(Long resourceId, LocalDateTime start, LocalDateTime end) {
        return lockRepository.existsBetween(start, end, resourceId);
    }
}

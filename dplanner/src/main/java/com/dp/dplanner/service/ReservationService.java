package com.dp.dplanner.service;

import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.LockRepository;
import com.dp.dplanner.repository.ReservationRepository;
import com.dp.dplanner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

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

        Reservation reservation = createDto.toEntity(clubMember, resource);
        Reservation savedReservation = reservationRepository.save(reservation);

        if (clubMember.hasAuthority(SCHEDULE_ALL)) {
            savedReservation.confirm();
        }

        return ReservationDto.Response.of(savedReservation);
    }

    private boolean isReservable(Long resourceId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return !(reservationRepository.existsBetween(startDateTime, endDateTime, resourceId)
                || lockRepository.existsBetween(startDateTime, endDateTime, resourceId));
    }
}

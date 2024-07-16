package com.dp.dplanner.service;

import com.dp.dplanner.repository.ReservationRepository;
import com.dp.dplanner.service.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.LockRepository;
import com.dp.dplanner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.adapter.dto.LockDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LockService {

    private final ReservationRepository reservationRepository;
    private final LockRepository lockRepository;
    private final ResourceRepository resourceRepository;
    private final ClubMemberRepository clubMemberRepository;


    @RequiredAuthority(authority = SCHEDULE_ALL)
    @Transactional
    public Response createLock(Long clubMemberId, Create createDto) {
        Resource resource = getResource(createDto.getResourceId());
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, resource.getClub().getId());

        checkIfThereExistsLocksDuringPeriod(createDto.getStartDateTime(), createDto.getEndDateTime(), createDto.getResourceId(), null);
        checkIsReserved(createDto.getResourceId(), createDto.getStartDateTime(), createDto.getEndDateTime());


        Lock lock = lockRepository.save(createDto.toEntity(resource));

        return Response.of(lock);
    }

    public Response getLock(Long clubMemberId, Long lockId) {
        Lock lock = getLock(lockId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, lock.getResource().getClub().getId());

        return Response.of(lock);

    }
    @RequiredAuthority(authority = SCHEDULE_ALL)
    @Transactional
    public void deleteLock(Long clubMemberId, Long lockId) {
        Lock lock = getLock(lockId);
        Resource resource = lock.getResource();
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, resource.getClub().getId());

        lockRepository.delete(lock);
    }

    @RequiredAuthority(authority = SCHEDULE_ALL)
    @Transactional
    public Response updateLock(Long clubMemberId, Update updateDto) {
        Lock lock = getLock(updateDto.getId());
        checkIfThereExistsLocksDuringPeriod(updateDto.getStartDateTime(), updateDto.getEndDateTime(), updateDto.getResourceId(), lock);

        Resource resource = lock.getResource();
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, resource.getClub().getId());

        lock.update(new Period(updateDto.getStartDateTime(), updateDto.getEndDateTime()), updateDto.getMessage());

        return Response.of(lock);
    }

    public List<Response> getLocks(Long clubMemberId, Long resourceId, Period period) {
        Resource resource = getResource(resourceId);
        ClubMember clubMember = getClubMember(clubMemberId);
        checkIsSameClub(clubMember, resource.getClub().getId());

        List<Lock> locks = lockRepository.findBetween(period.getStartDateTime(), period.getEndDateTime(), resourceId);

        //todo 임시방편 코드 추후 락 관련  논의 필요
        LocalDateTime cutoffDate = LocalDateTime.now().plusDays(resource.getBookableSpan()).toLocalDate().atStartOfDay();
        List<Lock> filteredLock = locks.stream()
                .filter(lock -> !(lock.getPeriod().getStartDateTime().isAfter(cutoffDate) || lock.getPeriod().getEndDateTime().isAfter(cutoffDate)))
                .collect(Collectors.toList());

        return Response.ofList(filteredLock);
    }



    /**
     * utility methods
     */
    private void checkIsSameClub(ClubMember clubMember, Long targetClubId) {
        if (!clubMember.isSameClub(targetClubId)) {
            throw new ServiceException(DIFFERENT_CLUB_EXCEPTION);
        }
    }
    /**
     * @param target : 제외시킬 Lock
     */
    private void checkIfThereExistsLocksDuringPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime, Long resourceId, Lock target) {
        List<Lock> locksBetween = lockRepository.findBetween(startDateTime, endDateTime, resourceId);

        if (locksBetween.stream().filter(lock -> !lock.equals(target)).count() != 0) {
            throw new ServiceException(PERIOD_OVERLAPPED_EXCEPTION);
        }
    }

    /**
     * 데이터베이스에 이미 예약이 있는지 검사
     */
    private void checkIsReserved(Long resourceId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (reservationRepository.existsBetween(startDateTime, endDateTime, resourceId)) {
            throw new ServiceException("reservation is already reserved. Can not lock that request time.",400);
        }
    }

    private Lock getLock(Long lockId) {
        return lockRepository.findById(lockId).orElseThrow(()->new ServiceException(LOCK_NOT_FOUND));
    }
    private Resource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId).orElseThrow(() -> new ServiceException(RESOURCE_NOT_FOUND));
    }
    private ClubMember getClubMember(Long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ServiceException(CLUBMEMBER_NOT_FOUND));
    }
}

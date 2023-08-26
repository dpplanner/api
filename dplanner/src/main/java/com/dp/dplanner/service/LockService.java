package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.exception.LockException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.LockRepository;
import com.dp.dplanner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.dto.LockDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LockService {

    private final LockRepository lockRepository;
    private final ResourceRepository resourceRepository;
    private final ClubMemberRepository clubMemberRepository;



    @RequiredAuthority(SCHEDULE_ALL)
    @Transactional
    public Response createLock(Long clubMemberId, Create createDto) {

        checkIfThereExistsLocksDuringPeriod(createDto.getStartDateTime(), createDto.getEndDateTime(), createDto.getResourceId(), null);

        Resource resource = getResource(createDto.getResourceId());
        checkIsSameClub(clubMemberId, resource);

        Lock lock = lockRepository.save(createDto.toEntity(resource));

        return Response.of(lock);
    }

    public Response getLock(Long clubMemberId, Long lockId) {

        Lock lock = getLock(lockId);
        checkIsSameClub(clubMemberId, lock.getResource());

        return Response.of(lock);

    }
    @RequiredAuthority(SCHEDULE_ALL)
    @Transactional
    public void deleteLock(Long clubMemberId, Long lockId) {
        Lock lock = getLock(lockId);

        Resource resource = lock.getResource();
        checkIsSameClub(clubMemberId, resource);


        lockRepository.delete(lock);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    @Transactional
    public Response updateLock(Long clubMemberId, Update updateDto) {

        Lock lock = getLock(updateDto.getId());

        checkIfThereExistsLocksDuringPeriod(updateDto.getStartDateTime(), updateDto.getEndDateTime(), updateDto.getResourceId(), lock);
        Resource resource = lock.getResource();
        checkIsSameClub(clubMemberId, resource);

        lock.update(new Period(updateDto.getStartDateTime(), updateDto.getEndDateTime()));


        return Response.of(lock);
    }

    public List<Response> getLocks(Long clubMemberId, Long resourceId, Period period) {

        Resource resource = getResource(resourceId);
        checkIsSameClub(clubMemberId, resource);

        List<Lock> locks = lockRepository.findBetween(period.getStartDateTime(), period.getEndDateTime(), resourceId);

        return Response.ofList(locks);
    }

    private void checkIsSameClub(Long clubMemberId, Resource resource) {
        ClubMember clubMember = getClubMember(clubMemberId);
        if (!clubMember.isSameClub(resource)) {
            throw new LockException(DIFFERENT_CLUB_EXCEPTION);
        }

    }

    /**
     * @param target : 제외시킬 Lock
     */
    private void checkIfThereExistsLocksDuringPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime, Long resourceId, Lock target) {
        List<Lock> locksBetween = lockRepository.findBetween(startDateTime, endDateTime, resourceId);

        if (locksBetween.stream().filter(lock -> !lock.equals(target)).count() != 0) {
            throw new LockException(PERIOD_OVERLAPPED_EXCEPTION);
        }
    }

    private Lock getLock(Long lockId) {
        return lockRepository.findById(lockId).orElseThrow(()->new LockException(LOCK_NOT_FOUND));
    }

    private Resource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId).orElseThrow(() -> new LockException(RESOURCE_NOT_FOUND));
    }

    private ClubMember getClubMember(Long clubMemberId) {
        return clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new LockException(CLUBMEMBER_NOT_FOUND));
    }
}

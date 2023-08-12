package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.LockRepository;
import com.dp.dplanner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.dto.LockDto.*;

@Service
@RequiredArgsConstructor
public class LockService {

    private final LockRepository lockRepository;
    private final ResourceRepository resourceRepository;
    private final ClubMemberRepository clubMemberRepository;



    @RequiredAuthority(SCHEDULE_ALL)
    public Response createLock(Long clubMemberId, Create createDto) {

        checkIfThereExistsLocksDuringPeriod(createDto.getStartDateTime(), createDto.getEndDateTime(), createDto.getResourceId(), null);

        Resource resource = resourceRepository.findById(createDto.getResourceId()).orElseThrow(RuntimeException::new);
        checkIfClubMemberAndResourceOfLockIsSameClub(clubMemberId, resource);

        Lock lock = lockRepository.save(createDto.toEntity(resource));

        return Response.of(lock);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public void deleteLock(Long clubMemberId, Long lockId) {
        Lock lock = lockRepository.findById(lockId).orElseThrow(RuntimeException::new);

        Resource resource = lock.getResource();
        checkIfClubMemberAndResourceOfLockIsSameClub(clubMemberId, resource);


        lockRepository.delete(lock);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public Response updateLock(Long clubMemberId, Update updateDto) {

        Lock lock = lockRepository.findById(updateDto.getId()).orElseThrow(RuntimeException::new);

        checkIfThereExistsLocksDuringPeriod(updateDto.getStartDateTime(), updateDto.getEndDateTime(), updateDto.getResourceId(), lock);
        Resource resource = lock.getResource();
        checkIfClubMemberAndResourceOfLockIsSameClub(clubMemberId, resource);

        lock.update(new Period(updateDto.getStartDateTime(), updateDto.getEndDateTime()));


        return Response.of(lock);
    }

    public List<Response> getLocks(Long clubMemberId, Long resourceId, Period period) {

        Resource resource = resourceRepository.findById(resourceId).orElseThrow(RuntimeException::new);
        checkIfClubMemberAndResourceOfLockIsSameClub(clubMemberId, resource);

        List<Lock> locks = lockRepository.findBetween(period.getStartDateTime(), period.getEndDateTime(), resourceId);

        return Response.ofList(locks);
    }

    private void checkIfClubMemberAndResourceOfLockIsSameClub(Long clubMemberId, Resource resource) {
        ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(RuntimeException::new);
        if (!clubMember.isSameClub(resource)) {
            throw new RuntimeException();
        }

    }

    /**
     * @param target : 제외시킬 Lock
     */
    private void checkIfThereExistsLocksDuringPeriod(LocalDateTime startDateTime, LocalDateTime endDateTime, Long resourceId, Lock target) {
        List<Lock> locksBetween = lockRepository.findBetween(startDateTime, endDateTime, resourceId);

        if (locksBetween.stream().filter(lock -> !lock.equals(target)).count() != 0) {
            throw new RuntimeException();
        }
    }
}

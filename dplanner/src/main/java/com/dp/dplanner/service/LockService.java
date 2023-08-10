package com.dp.dplanner.service;

import com.dp.dplanner.aop.annotation.RequiredAuthority;
import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.repository.LockRepository;
import com.dp.dplanner.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.dto.LockDto.*;

@Service
@RequiredArgsConstructor
public class LockService {

    private final LockRepository lockRepository;
    private final ResourceRepository resourceRepository;



    @RequiredAuthority(SCHEDULE_ALL)
    public Response createLock(Long clubMemberId, Create createDto) {

        checkLockDuringPeriod(createDto.getStartDateTime(),createDto.getEndDateTime(), createDto.getResourceId(),null);

        Resource resource = resourceRepository.findById(createDto.getResourceId()).orElseThrow(RuntimeException::new);
        Lock lock = lockRepository.save(createDto.toEntity(resource));

        return Response.of(lock);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public void deleteLock(Long clubMemberId, Long lockId) {

        Lock lock = lockRepository.findById(lockId).orElseThrow(RuntimeException::new);
        lockRepository.delete(lock);
    }

    @RequiredAuthority(SCHEDULE_ALL)
    public Response updateLock(Long clubMemberId, Update updateDto) {

        Lock lock = lockRepository.findById(updateDto.getId()).orElseThrow(RuntimeException::new);
        checkLockDuringPeriod(updateDto.getStartDateTime(),updateDto.getEndDateTime(), updateDto.getResourceId(),lock);
        lock.update(new Period(updateDto.getStartDateTime(), updateDto.getEndDateTime()));

        return Response.of(lock);
    }
    public List<Response> getLocks(Long resourceId, Period period) {
        List<Lock> locks = lockRepository.findLocksBetween(period.getStartDateTime(), period.getEndDateTime(), resourceId);

        return Response.ofList(locks);
    }

    /**
     *
     * @param startDateTime
     * @param endDateTime
     * @param resourceId
     * @param target : 제외시킬 Lock
     */
    private void checkLockDuringPeriod(LocalDateTime startDateTime,LocalDateTime endDateTime,Long resourceId,Lock target) {
        List<Lock> locksBetween = lockRepository.findLocksBetween(startDateTime, endDateTime, resourceId);

        if (!locksBetween.stream().filter(lock -> !lock.equals(target)).collect(Collectors.toList()).isEmpty()) {
            throw new RuntimeException();
        }
    }
}

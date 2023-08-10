package com.dp.dplanner.service;

import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.repository.LockRepository;
import com.dp.dplanner.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.dto.LockDto.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LockServiceTest {

    @Mock
    LockRepository lockRepository;
    @Mock
    ResourceRepository resourceRepository;
    @InjectMocks
    LockService lockService;

    Long memberId;
    Long clubId;
    Long clubMemberId;
    Long resourceId;
    Club club;
    Member member;
    ClubMember clubMember;
    Resource resource;

    @BeforeEach
    public void setUp() {

        memberId = 10L;
        member = Member.builder().build();
        ReflectionTestUtils.setField(member,"id",clubId);


        clubId = 10L;
        club = Club.builder().build();
        ReflectionTestUtils.setField(club,"id",clubId);

        clubMemberId = 20L;
        clubMember = ClubMember.builder()
                .member(member)
                .club(club)
                .build();

        resourceId = 20L;
        resource = Resource.builder()
                .club(club)
                .build();
        ReflectionTestUtils.setField(resource,"id",resourceId);

    }

    @Test
    public void LockService_CreateLock_ReturnResponse() {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);

        Create createDto = Create.builder()
                .startDateTime(start)
                .endDateTime(end)
                .resourceId(resourceId)
                .build();

        when(resourceRepository.findById(resourceId)).thenReturn(Optional.ofNullable(resource));
        when(lockRepository.save(any(Lock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Response response = lockService.createLock(clubMemberId, createDto);

        assertThat(response).isNotNull();
        assertThat(response.getStartDateTime()).isEqualTo(start);
        assertThat(response.getEndDateTime()).isEqualTo(end);
        assertThat(response.getResourceId()).isEqualTo(resourceId);

    }
    
    @Test
    public void LockService_CreateLock_ThrowException_PeriodOverlap(){

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);

        Create createDto = Create.builder()
                .startDateTime(start)
                .endDateTime(end)
                .resourceId(resourceId)
                .build();

        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();

        when(lockRepository.findLocksBetween(start, end, resourceId)).thenReturn(Arrays.asList(lock));

        assertThatThrownBy(()->lockService.createLock(clubMemberId, createDto)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void LockService_DeleteLock_ReturnVoid(){

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);

        Long lockId = 1L;
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();
        ReflectionTestUtils.setField(lock, "id", lockId);

        when(lockRepository.findById(lockId)).thenReturn(Optional.ofNullable(lock));

        assertAll(()->lockService.deleteLock(clubMemberId,lockId));
    }


    @Test
    public void LockService_UpdateLock_ReturnResponse(){

        Long lockId = 1L;
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);

        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();
        ReflectionTestUtils.setField(lock, "id", lockId);


        Update updateDto = Update.builder()
                .id(lockId)
                .resourceId(resourceId)
                .startDateTime(start.plusDays(1))
                .endDateTime(end.plusDays(1))
                .build();

        when(lockRepository.findById(lockId)).thenReturn(Optional.ofNullable(lock));

        Response response = lockService.updateLock(clubMemberId, updateDto);

        assertThat(response.getId()).isEqualTo(lockId);
        assertThat(response.getStartDateTime()).isEqualTo(start.plusDays(1));
        assertThat(response.getEndDateTime()).isEqualTo(end.plusDays(1));
        assertThat(response.getResourceId()).isEqualTo(resourceId);

    }

    @Test
    public void LockService_UpdateLock_ThrowException_PeriodOverlap(){

        Long lockId = 1L;
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();
        ReflectionTestUtils.setField(lock, "id", lockId);

        Long overlappedLockId = 2L;
        Lock overlappedLock = Lock.builder()
                .resource(resource)
                .period(new Period(start.plusDays(8), start.plusDays(9)))
                .build();
        ReflectionTestUtils.setField(overlappedLock, "id", overlappedLockId);

        LocalDateTime updateStart = start.plusDays(1);
        LocalDateTime updateEnd = start.plusDays(9);

        Update updateDto = Update.builder()
                .id(lockId)
                .resourceId(resourceId)
                .startDateTime(updateStart)
                .endDateTime(updateEnd)
                .build();

        when(lockRepository.findLocksBetween(updateStart, updateEnd, resourceId)).thenReturn(Arrays.asList(lock,overlappedLock));
        when(lockRepository.findById(lockId)).thenReturn(Optional.ofNullable(lock));

        assertThatThrownBy(() -> lockService.updateLock(clubMemberId, updateDto));

    }


    @Test
    public void LockService_getLocks_ReturnResponseList() {


        LocalDateTime start = LocalDateTime.now();
        Period period = new Period(start, start.plusDays(7));

        Lock lock1 = Lock.builder()
                .resource(resource)
                .period(new Period(start.minusDays(1), start.plusDays(6)))
                .build();
        Lock lock2 = Lock.builder()
                .resource(resource)
                .period(new Period(start, start.plusDays(7)))
                .build();


        Lock lock3 = Lock.builder()
                .resource(resource)
                .period(new Period(start.plusDays(1), start.plusDays(6)))
                .build();

        Lock lock4 = Lock.builder()
                .resource(resource)
                .period(new Period(start.plusDays(1), start.plusDays(8)))
                .build();


        when(lockRepository.findLocksBetween(any(LocalDateTime.class), any(LocalDateTime.class), anyLong())).thenReturn(Arrays.asList(lock1, lock2, lock3, lock4));
        List<Response> responseList = lockService.getLocks(resourceId, period);

        assertThat(responseList.size()).isEqualTo(4);
        assertThat(responseList).extracting(Response::getResourceId).containsOnly(resourceId);


    }


}



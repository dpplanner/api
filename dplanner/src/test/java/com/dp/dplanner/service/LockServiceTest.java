package com.dp.dplanner.service;

import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.exception.BaseException;
import com.dp.dplanner.exception.LockException;
import com.dp.dplanner.repository.ClubMemberRepository;
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
import static com.dp.dplanner.exception.ErrorResult.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LockServiceTest {

    @Mock
    LockRepository lockRepository;
    @Mock
    ResourceRepository resourceRepository;
    @Mock
    ClubMemberRepository clubMemberRepository;
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

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Create createDto = Create.builder()
                .startDateTime(start)
                .endDateTime(end)
                .resourceId(resourceId)
                .build();
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
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

        LocalDateTime start = LocalDateTime.of(2023,8,11,12,0,0);
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

        when(lockRepository.findBetween(start, end, resourceId)).thenReturn(Arrays.asList(lock));

        BaseException lockException = assertThrows(LockException.class, () -> lockService.createLock(clubMemberId, createDto));
        assertThat(lockException.getErrorResult()).isEqualTo(PERIOD_OVERLAPPED_EXCEPTION);
    }

    @Test
    public void LockService_createLock_checkIfClubMemberAndResourceOfLockIsSameClub_ThrowException() {


        Club otherClub = Club.builder().build();
        ReflectionTestUtils.setField(otherClub, "id", clubId + 1);
        Resource otherClubResource = Resource.builder()
                .club(otherClub)
                .build();
        ReflectionTestUtils.setField(otherClubResource, "id", resourceId+1);


        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Create createDto = Create.builder()
                .startDateTime(start)
                .endDateTime(end)
                .resourceId(otherClubResource.getId())
                .build();

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(resourceRepository.findById(createDto.getResourceId())).thenReturn(Optional.ofNullable(otherClubResource));

        assertThatThrownBy(() -> lockService.createLock(clubMemberId, createDto)).isInstanceOf(RuntimeException.class);

        BaseException clubMemberException = assertThrows(LockException.class, () -> lockService.createLock(clubMemberId, createDto));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);

    }

    @Test
    public void LockService_getLock_ReturnResponse() {

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Long lockId = 1L;
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();
        ReflectionTestUtils.setField(lock, "id", lockId);

        when(lockRepository.findById(lockId)).thenReturn(Optional.ofNullable(lock));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));

        Response response = lockService.getLock(clubMemberId, lockId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(lockId);
        assertThat(response.getResourceId()).isEqualTo(resourceId);
        assertThat(response.getStartDateTime()).isEqualTo(start);
        assertThat(response.getEndDateTime()).isEqualTo(end);
    }
    @Test
    public void LockService_DeleteLock_ReturnVoid(){

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Long lockId = 1L;
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();
        ReflectionTestUtils.setField(lock, "id", lockId);

        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(lockRepository.findById(lockId)).thenReturn(Optional.ofNullable(lock));

        assertAll(()->lockService.deleteLock(clubMemberId,lockId));
    }

    @Test
    public void LockService_deleteLock_checkIfClubMemberAndResourceOfLockIsSameClub_ThrowException() {


        Club otherClub = Club.builder().build();
        ReflectionTestUtils.setField(otherClub, "id", clubId + 1);
        Resource otherClubResource = Resource.builder()
                .club(otherClub)
                .build();
        ReflectionTestUtils.setField(otherClubResource, "id", resourceId+1);

        Long lockId = 1L;
        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Lock lock = Lock.builder()
                .resource(otherClubResource)
                .period(new Period(start, end))
                .build();
        ReflectionTestUtils.setField(lock, "id", lockId);


        when(lockRepository.findById(lockId)).thenReturn(Optional.ofNullable(lock));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));

        BaseException clubMemberException = assertThrows(LockException.class,() -> lockService.deleteLock(clubMemberId, lockId));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);

    }


    @Test
    public void LockService_UpdateLock_ReturnResponse(){

        Long lockId = 1L;
        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
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
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(lockRepository.findById(lockId)).thenReturn(Optional.ofNullable(lock));

        Response response = lockService.updateLock(clubMemberId, updateDto);

        assertThat(response.getId()).isEqualTo(lockId);
        assertThat(response.getStartDateTime()).isEqualTo(start.plusDays(1));
        assertThat(response.getEndDateTime()).isEqualTo(end.plusDays(1));
        assertThat(response.getResourceId()).isEqualTo(resourceId);

    }

    @Test
    public void LockService_updateLock_checkIfClubMemberAndResourceOfLockIsSameClub_ThrowException() {


        Club otherClub = Club.builder().build();
        ReflectionTestUtils.setField(otherClub, "id", clubId + 1);
        Resource otherClubResource = Resource.builder()
                .club(otherClub)
                .build();
        ReflectionTestUtils.setField(otherClubResource, "id", resourceId+1);

        Long lockId = 1L;
        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Lock lock = Lock.builder()
                .resource(otherClubResource)
                .period(new Period(start, end))
                .build();
        ReflectionTestUtils.setField(lock, "id", lockId);


        Update updateDto = Update.builder()
                .id(lockId)
                .resourceId(otherClubResource.getId())
                .startDateTime(start.plusDays(1))
                .endDateTime(end.plusDays(1))
                .build();

        when(lockRepository.findById(updateDto.getId())).thenReturn(Optional.ofNullable(lock));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));


        BaseException clubMemberException = assertThrows(LockException.class,() -> lockService.updateLock(clubMemberId, updateDto));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);


    }


    @Test
    public void LockService_UpdateLock_ThrowException_PeriodOverlap(){

        Long lockId = 1L;
        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
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

        when(lockRepository.findBetween(updateStart, updateEnd, resourceId)).thenReturn(Arrays.asList(lock,overlappedLock));
        when(lockRepository.findById(lockId)).thenReturn(Optional.ofNullable(lock));


        BaseException lockException = assertThrows(LockException.class, () -> lockService.updateLock(clubMemberId, updateDto));
        assertThat(lockException.getErrorResult()).isEqualTo(PERIOD_OVERLAPPED_EXCEPTION);
    }


    @Test
    public void LockService_getLocks_ReturnResponseList() {


        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
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
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.ofNullable(resource));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));
        when(lockRepository.findBetween(any(LocalDateTime.class), any(LocalDateTime.class), anyLong())).thenReturn(Arrays.asList(lock1, lock2, lock3, lock4));
        List<Response> responseList = lockService.getLocks(clubMemberId,resourceId, period);

        assertThat(responseList.size()).isEqualTo(4);
        assertThat(responseList).extracting(Response::getResourceId).containsOnly(resourceId);


    }

    @Test
    public void LockService_getLocks_checkIfClubMemberAndResourceOfLockIsSameClub_ThrowException() {

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        Period period = new Period(start, start.plusDays(7));

        Club otherClub = Club.builder().build();
        ReflectionTestUtils.setField(otherClub, "id", clubId + 1);
        Resource otherClubResource = Resource.builder()
                .club(otherClub)
                .build();
        ReflectionTestUtils.setField(otherClubResource, "id", resourceId+1);

        when(resourceRepository.findById(resourceId+1)).thenReturn(Optional.ofNullable(otherClubResource));
        when(clubMemberRepository.findById(clubMemberId)).thenReturn(Optional.ofNullable(clubMember));

        BaseException clubMemberException = assertThrows(LockException.class, () -> lockService.getLocks(clubMemberId, resourceId + 1, period));
        assertThat(clubMemberException.getErrorResult()).isEqualTo(DIFFERENT_CLUB_EXCEPTION);

    }


}



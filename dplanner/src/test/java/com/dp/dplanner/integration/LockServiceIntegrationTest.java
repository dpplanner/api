package com.dp.dplanner.integration;


import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubAuthority;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.service.LockService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.dp.dplanner.domain.club.ClubAuthorityType.*;
import static com.dp.dplanner.dto.LockDto.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
public class LockServiceIntegrationTest {


    @Autowired
    LockService lockService;

    @Autowired
    EntityManager entityManager;

    Member member;
    Club club;
    ClubMember clubMember;
    Resource resource;
    @BeforeEach
    public void setUp() {

        member = Member.builder().build();

        club = Club.builder().build();

        clubMember = ClubMember.builder()
                .member(member)
                .club(club)
                .build();

        resource = Resource.builder()
                .club(club)
                .build();

        entityManager.persist(member);
        entityManager.persist(club);
        entityManager.persist(clubMember);
        entityManager.persist(resource);

    }

    @Test
    public void LockService_CreateLock_ReturnResponse_Admin() {

        clubMember.setAdmin();

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Create createDto = Create.builder()
                .startDateTime(start)
                .endDateTime(end)
                .resourceId(resource.getId())
                .build();

        Response response = lockService.createLock(clubMember.getId(), createDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isGreaterThan(0);
        assertThat(response.getStartDateTime()).isEqualTo(start);
        assertThat(response.getEndDateTime()).isEqualTo(end);
        assertThat(response.getResourceId()).isEqualTo(resource.getId());

    }

    @Test
    public void LockService_CreateLock_ReturnResponse_ManagerHasAuthority() {
        clubMember.setManager();
        ClubAuthority.createAuthorities(club, Arrays.asList(SCHEDULE_ALL));

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Create createDto = Create.builder()
                .startDateTime(start)
                .endDateTime(end)
                .resourceId(resource.getId())
                .build();

        Response response = lockService.createLock(clubMember.getId(), createDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isGreaterThan(0);
        assertThat(response.getStartDateTime()).isEqualTo(start);
        assertThat(response.getEndDateTime()).isEqualTo(end);
        assertThat(response.getResourceId()).isEqualTo(resource.getId());

    }

    @Test
    public void LockService_CreateLock_ThrowException_NotAdmin_NoAuthority() {
        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Create createDto = Create.builder()
                .startDateTime(start)
                .endDateTime(end)
                .resourceId(resource.getId())
                .build();

        assertThatThrownBy(() -> lockService.createLock(clubMember.getId(), createDto)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void LockService_CreateLock_ThrowException_ManagerHasNoAuthority() {
        clubMember.setManager();

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);

        Create createDto = Create.builder()
                .startDateTime(start)
                .endDateTime(end)
                .resourceId(resource.getId())
                .build();

        assertThatThrownBy(() -> lockService.createLock(clubMember.getId(), createDto)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void LockService_DeleteLock_ReturnVoid_Admin() {

        clubMember.setAdmin();
        LocalDateTime start = LocalDateTime.of(2023, 8, 10, 12, 0, 0);
        Lock lock = Lock.builder().resource(resource)
                .period(new Period(start, start.plusDays(7)))
                .build();
        entityManager.persist(lock);

        lockService.deleteLock(clubMember.getId(),lock.getId());
    }

    @Test
    public void LockService_DeleteLock_ReturnVoid_ManagerHasAuthority() {


        clubMember.setManager();
        ClubAuthority.createAuthorities(club, Arrays.asList(SCHEDULE_ALL));

        LocalDateTime start = LocalDateTime.of(2023, 8, 10, 12, 0, 0);
        Lock lock = Lock.builder().resource(resource)
                .period(new Period(start, start.plusDays(7)))
                .build();
        entityManager.persist(lock);

        lockService.deleteLock(clubMember.getId(),lock.getId());
    }

    @Test
    public void LockService_DeleteLock_ThrowException_NotAdmin_NoAuthority() {

        LocalDateTime start = LocalDateTime.of(2023, 8, 10, 12, 0, 0);
        Lock lock = Lock.builder().resource(resource)
                .period(new Period(start, start.plusDays(7)))
                .build();
        entityManager.persist(lock);

        assertThatThrownBy(() -> lockService.deleteLock(clubMember.getId(), lock.getId())).isInstanceOf(RuntimeException.class);
    }


    @Test
    public void LockService_DeleteLock_ThrowException_ManagerHasNoAuthority() {

        clubMember.setManager();

        LocalDateTime start = LocalDateTime.of(2023, 8, 10, 12, 0, 0);
        Lock lock = Lock.builder().resource(resource)
                .period(new Period(start, start.plusDays(7)))
                .build();
        entityManager.persist(lock);

        assertThatThrownBy(() -> lockService.deleteLock(clubMember.getId(), lock.getId())).isInstanceOf(RuntimeException.class);

    }

    @Test
    public void LockService_UpdateLock_ReturnResponse_Admin() {
        clubMember.setAdmin();

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();

        entityManager.persist(lock);

        Update updateDto = Update.builder()
                .id(lock.getId())
                .resourceId(resource.getId())
                .startDateTime(start.plusDays(1))
                .endDateTime(end.plusDays(1))
                .build();

        Response response = lockService.updateLock(clubMember.getId(), updateDto);

        assertThat(response.getId()).isEqualTo(lock.getId());
        assertThat(response.getStartDateTime()).isEqualTo(start.plusDays(1));
        assertThat(response.getEndDateTime()).isEqualTo(end.plusDays(1));
        assertThat(response.getResourceId()).isEqualTo(resource.getId());

    }

    @Test
    public void LockService_UpdateLock_ReturnResponse_ManagerHasAuthority() {
        clubMember.setManager();
        ClubAuthority.createAuthorities(club, Arrays.asList(SCHEDULE_ALL));

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();

        entityManager.persist(lock);

        Update updateDto = Update.builder()
                .id(lock.getId())
                .resourceId(resource.getId())
                .startDateTime(start.plusDays(1))
                .endDateTime(end.plusDays(1))
                .build();

        Response response = lockService.updateLock(clubMember.getId(), updateDto);

        assertThat(response.getId()).isEqualTo(lock.getId());
        assertThat(response.getStartDateTime()).isEqualTo(start.plusDays(1));
        assertThat(response.getEndDateTime()).isEqualTo(end.plusDays(1));
        assertThat(response.getResourceId()).isEqualTo(resource.getId());
    }

    @Test
    public void LockService_UpdateLock_ThrowException_ManagerHasNoAuthority() {
        clubMember.setManager();

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();

        entityManager.persist(lock);

        Update updateDto = Update.builder()
                .id(lock.getId())
                .resourceId(resource.getId())
                .startDateTime(start.plusDays(1))
                .endDateTime(end.plusDays(1))
                .build();

        assertThatThrownBy(()-> lockService.updateLock(clubMember.getId(), updateDto)).isInstanceOf(RuntimeException.class);

    }
    @Test
    public void LockService_UpdateLock_ThrowException_NotAdmin_NoAuthority() {

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();

        entityManager.persist(lock);

        Update updateDto = Update.builder()
                .id(lock.getId())
                .resourceId(resource.getId())
                .startDateTime(start.plusDays(1))
                .endDateTime(end.plusDays(1))
                .build();

        assertThatThrownBy(()-> lockService.updateLock(clubMember.getId(), updateDto)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void LockService_UpdateLock_ThrowException_PeriodOverlap() {

        clubMember.setAdmin();

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);
        Lock lock = Lock.builder()
                .resource(resource)
                .period(new Period(start, end))
                .build();

        entityManager.persist(lock);

        Lock overlappedLock = Lock.builder()
                .resource(resource)
                .period(new Period(end, end.plusDays(2)))
                .build();

        entityManager.persist(overlappedLock);

        Update updateDto = Update.builder()
                .id(lock.getId())
                .resourceId(resource.getId())
                .startDateTime(start.plusDays(1))
                .endDateTime(end.plusDays(1))
                .build();

        assertThatThrownBy(()-> lockService.updateLock(clubMember.getId(), updateDto)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void LockService_findLocks_ReturnResponseList() {

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        Period period = new Period(start, start.plusDays(7));


        Lock lock0 = Lock.builder()
                .resource(resource)
                .period(new Period(start.minusDays(1), start))
                .build();

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

        Lock lock5 = Lock.builder()
                .resource(resource)
                .period(new Period(start.plusDays(7), start.plusDays(8)))
                .build();
        entityManager.persist(lock0);
        entityManager.persist(lock1);
        entityManager.persist(lock2);
        entityManager.persist(lock3);
        entityManager.persist(lock4);
        entityManager.persist(lock5);

        List<Response> responseList = lockService.getLocks(resource.getId(), period);

        assertThat(responseList.size()).isEqualTo(6);
        assertThat(responseList).extracting(Response::getResourceId).containsOnly(resource.getId());



    }

}

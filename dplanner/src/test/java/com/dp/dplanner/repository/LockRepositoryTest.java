package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Lock;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class LockRepositoryTest {

    @Autowired
    private LockRepository lockRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    Club club;
    Resource resource;

    LocalDateTime start = LocalDateTime.of(2023, 8, 11, 17, 0, 0);
    LocalDateTime end = LocalDateTime.of(2023, 8, 11, 20, 0, 0);

    @BeforeEach
    public void setUp() {
        club = Club.builder().build();

        resource = Resource.builder()
                .club(club)
                .build();

        testEntityManager.persist(club);
        testEntityManager.persist(resource);
    }


    @Test
    public void LockRepository_save_ReturnLock() {
        Period period = new Period(LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        Lock lock = Lock.builder()
                .resource(resource)
                .period(period)
                .build();

        lockRepository.save(lock);

        assertThat(lock).isNotNull();
        assertThat(lock.getId()).isGreaterThan(0);
    }

    @Test
    public void LockRepository_save_ThrowException() {
        assertThatThrownBy(() -> new Period(LocalDateTime.now().plusDays(7), LocalDateTime.now())).isInstanceOf(RuntimeException.class);
    }


    @Test
    public void LockRepository_findLocksBetweenByResourceId_ReturnLockList() {

        LocalDateTime start = LocalDateTime.of(2023,8,10,12,0,0);
        LocalDateTime end = start.plusDays(7);
        createLock(new Period(start.minusDays(2), start.minusDays(1))); // Not Included

        createLock(new Period(start,end)); // Exactly Same
        createLock(new Period(start.plusDays(1), end.minusDays(1))); // Start , End Included
        createLock(new Period(start.plusDays(1), end.plusDays(1))); // Start Included
        createLock(new Period(start.minusDays(1), end.minusDays(5))); // End Included
        createLock(new Period(start.minusDays(1), end.plusDays(1))); // Start , End Outer Included


        List<Lock> locks = lockRepository.findBetween(start, end, resource.getId());

        System.out.println(start + " " + end);
        for (Lock lock : locks) {
            System.out.println(lock.getPeriod().getStartDateTime() + " " + lock.getPeriod().getEndDateTime());
        }

        assertThat(locks.size()).isEqualTo(5);

    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean1() {

        createLock(17,20);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean2() {

        createLock(18,19);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }
    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean3() {

        createLock(19,21);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean4() {

        createLock(16,18);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean5() {

        createLock(17,18);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean6() {

        createLock(19,20);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean7() {

        createLock(16,21);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean8() {

        createLock(17,21);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean9() {

        createLock(16,20);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isTrue();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean10() {

        createLock(16,17);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isFalse();
    }

    @Test
    public void LockRepository_ExistLockBetween_ReturnBoolean11() {

        createLock(20,21);
        boolean result = lockRepository.existsBetween(start, end, resource.getId());
        assertThat(result).isFalse();
    }







    private void createLock(int startHour,int endHour) {
        LocalDateTime s = LocalDateTime.of(2023, 8, 11, startHour, 0, 0);
        LocalDateTime e = LocalDateTime.of(2023, 8, 11, endHour, 0, 0);
        createLock(new Period(s,e));
    }


    private Lock createLock(Period period) {
        Lock lock = Lock.builder()
                .resource(resource)
                .period(period)
                .build();

        return lockRepository.save(lock);

    }
}

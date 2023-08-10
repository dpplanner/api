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
//        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        createLock(new Period(start.minusDays(2), start.minusDays(1))); // Not Included

        createLock(new Period(start,end)); // Exactly Same
        createLock(new Period(start.plusDays(1), end.minusDays(1))); // Start , End Included
        createLock(new Period(start.plusDays(1), end.plusDays(1))); // Start Included
        createLock(new Period(start.minusDays(1), end.minusDays(5))); // End Included
        createLock(new Period(start.minusDays(1), end.plusDays(1))); // Start , End Outer Included


        List<Lock> locks = lockRepository.findLocksBetween(start, end, resource.getId());

        System.out.println(start + " " + end);
        for (Lock lock : locks) {
            System.out.println(lock.getPeriod().getStartDateTime() + " " + lock.getPeriod().getEndDateTime());
        }

        assertThat(locks.size()).isEqualTo(5);

    }

    private Lock createLock(Period period) {
        Lock lock = Lock.builder()
                .resource(resource)
                .period(period)
                .build();

        return lockRepository.save(lock);

    }
}

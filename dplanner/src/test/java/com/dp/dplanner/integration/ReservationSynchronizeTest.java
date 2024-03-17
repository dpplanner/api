package com.dp.dplanner.integration;

import com.dp.dplanner.TestConfig;
import com.dp.dplanner.adapter.dto.ReservationDto;
import com.dp.dplanner.service.RedisReservationService;
import com.dp.dplanner.repository.*;
import com.dp.dplanner.service.ReservationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import({TestConfig.class})
@Transactional
public class ReservationSynchronizeTest {




    @Autowired
    Clock clock;
    @Autowired
    ReservationService reservationService;

    @Autowired
    RedisReservationService redisReservationService;

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    ClubRepository clubRepository;
    @Autowired
    ResourceRepository resourceRepository;
    @Autowired
    ClubMemberRepository clubMemberRepository;


    @Sql(scripts = "/test-data.sql",
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
    @Sql(scripts = "/test-data-delete.sql",
            config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
    ,executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @RepeatedTest(10)
    @DisplayName("중복 예약 테스트")
    @Disabled("로컬에서 테스트")
    public void ReservationCreateSynchronizeTest() throws Exception {

        //given
        int threadCount = 10;
        Long clubMemberId = 1L;
        Long resourceId = 1L;

        AtomicInteger failCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);


        //when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                        try {
                            reservationService.createReservation(clubMemberId,
                                    getCreateDto(resourceId,
                                            "reservation",
                                            "usage",
                                            false,
                                            LocalDateTime.of(2023, 8, 10, 20, 0),
                                            LocalDateTime.of(2023, 8, 10, 21, 0))
                            );
                            successCount.addAndGet(1);
                        } catch (Exception e) {
                            failCount.addAndGet(1);
                            System.out.println(e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }
            );
        }

        latch.await(1,TimeUnit.SECONDS);

        redisReservationService.deleteReservation(LocalDateTime.of(2023, 8, 10, 20, 0), LocalDateTime.of(2023, 8, 10, 21, 0), 1L);

        assertThat(failCount.get()).isEqualTo(threadCount - 1);
        assertThat(successCount.get()).isEqualTo(1);

    }


    /**
     * Reservation util method
     */

    private ReservationDto.Create getCreateDto(
            Long resourceId, String title, String usage, boolean sharing, LocalDateTime start, LocalDateTime end) {

        return ReservationDto.Create.builder()
                .resourceId(resourceId)
                .title(title)
                .usage(usage)
                .sharing(sharing)
                .startDateTime(start)
                .endDateTime(end)
                .build();
    }



    /**
     * Time util method
     */
    private static LocalDateTime getTime(int hour) {
        return LocalDateTime.of(2023, 8, 10, hour, 0);
    }

}

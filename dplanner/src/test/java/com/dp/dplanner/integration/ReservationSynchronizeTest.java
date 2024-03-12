package com.dp.dplanner.integration;

import com.dp.dplanner.TestConfig;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.repository.*;
import com.dp.dplanner.service.ReservationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    MemberRepository memberRepository;
    @Autowired
    ClubRepository clubRepository;
    @Autowired
    ResourceRepository resourceRepository;
    @Autowired
    ClubMemberRepository clubMemberRepository;
    @Autowired
    TransactionManager tm;


    Member member;
    Club club;
    ClubMember clubMember;
    Resource resource;

    TransactionTemplate transaction;

    @BeforeEach
    void setUp() {

        transaction = new TransactionTemplate((PlatformTransactionManager) tm);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        member = Member.builder().name("testMember").build();
        club = Club.builder().clubName("testClub").build();
        transaction.execute(status -> memberRepository.saveAndFlush(member));
        transaction.execute(status -> clubRepository.saveAndFlush(club));

        resource = Resource.builder().club(club).name("testResource").build();
        transaction.execute(status -> resourceRepository.saveAndFlush(resource));

        clubMember = ClubMember.builder().member(member).club(club).build();
        clubMember.confirm();
        transaction.execute(status -> clubMemberRepository.saveAndFlush(clubMember));


    }

    @RepeatedTest(10)
    @DisplayName("중복 예약 테스트")
    public void ReservationCreateSynchronizeTest() throws Exception
    {
        //given
        int threadCount = 10;
        Long clubMemberId = clubMember.getId();
        Long resourceId = resource.getId();

        AtomicInteger failCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        assert clubMemberId != null;
        assert resourceId != null;

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

        System.out.println("successCount = " + successCount);
        System.out.println("failCount = " + failCount);

        assertThat(failCount.get()).isEqualTo(threadCount-1);
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

    private static Period getPeriod(int start, int end) {
        return new Period(getTime(start), getTime(end));
    }

}

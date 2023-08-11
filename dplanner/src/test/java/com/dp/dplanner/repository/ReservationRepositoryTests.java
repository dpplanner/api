package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.Resource;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
public class ReservationRepositoryTests {

    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    TestEntityManager entityManager;

    ClubMember clubMember;
    Resource resource;

    @BeforeEach
    void setUp() {
        Member member = Member.builder().build();
        Club club = Club.builder().build();
        clubMember = ClubMember.builder().member(member).club(club).build();
        resource = Resource.builder().club(club).build();

        entityManager.persist(member);
        entityManager.persist(club);
        entityManager.persist(clubMember);
        entityManager.persist(resource);
    }

    @Test
    @DisplayName("예약 등록")
    public void save() throws Exception {
        //given
        Period reservationPeriod = new Period(
                getTime(19),
                getTime(20)
        );

        Reservation reservation = Reservation.builder()
                .resource(resource)
                .clubMember(clubMember)
                .title("reservationTitle")
                .usage("reservationUsage")
                .period(reservationPeriod)
                .sharing(true)
                .build();

        //when
        Reservation savedReservation = reservationRepository.save(reservation);

        //then
        Reservation findReservation = reservationRepository.findById(savedReservation.getId()).orElse(null);

        assertThat(findReservation).as("예약이 등록되어야 한다").isNotNull();
        assertThat(findReservation.getClubMember()).as("예약을 등록한 사람이 일치해야 한다").isEqualTo(clubMember);
        assertThat(findReservation.getResource()).as("예약 정보에 있는 리소스가 일치해야 한다").isEqualTo(resource);
        assertThat(findReservation.getTitle()).as("예약 제목이 일치해야 한다").isEqualTo(reservation.getTitle());
        assertThat(findReservation.getUsage()).as("예약 용도가 일치해야 한다").isEqualTo(reservation.getUsage());
        assertThat(findReservation.isSharing()).as("공유 정보가 일치해야 한다").isEqualTo(reservation.isSharing());
        assertThat(findReservation.getStatus()).as("승인 상태가 일치해야 한다").isEqualTo(reservation.getStatus());
        assertThat(findReservation.getPeriod()).as("예약 시간이 일치해야 한다").isEqualTo(reservation.getPeriod());
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 1. exists 17:00 ~ 20:00 --> true")
    public void existsBetween1() throws Exception {
        //given
        persistReservation(17, 20);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 2. exists 18:00 ~ 19:00 --> true")
    public void existsBetween2() throws Exception {
        //given
        persistReservation(18, 19);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 3. exists 16:00 ~ 21:00 --> true")
    public void existsBetween3() throws Exception {
        //given
        persistReservation(16, 21);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 4. exists 19:00 ~ 21:00 --> true")
    public void existsBetween4() throws Exception {
        //given
        persistReservation(19, 21);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 5. exists 16:00 ~ 18:00 --> true")
    public void existsBetween5() throws Exception {
        //given
        persistReservation(16, 18);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 6. exists 16:00 ~ 17:00 --> false")
    public void existsBetween6() throws Exception {
        //given
        persistReservation(16, 17);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 7. exists 20:00 ~ 21:00 --> false")
    public void existsBetween7() throws Exception {
        //given
        persistReservation(20, 21);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 8. exists 17:00 ~ 18:00 --> true")
    public void existsBetween8() throws Exception {
        //given
        persistReservation(17, 18);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 9. exists 19:00 ~ 20:00 --> true")
    public void existsBetween9() throws Exception {
        //given
        persistReservation(19, 20);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 10. exists 16:00 ~ 20:00 --> true")
    public void existsBetween10() throws Exception {
        //given
        persistReservation(16, 20);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 안에 예약이 있는지 검사(request : 17:00 ~ 20:00) -- 11. exists 17:00 ~ 21:00 --> true")
    public void existsBetween11() throws Exception {
        //given
        persistReservation(17, 21);

        //when
        LocalDateTime searchStartTime = getTime(17);
        LocalDateTime searchEndTime = getTime(20);
        boolean result = reservationRepository.existsBetween(searchStartTime, searchEndTime, resource.getId());

        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("주어진 기간 내에 특정 예약을 제외한 다른 예약이 있는지 검사")
    public void existsOthersBetween() throws Exception {
        //given
        Reservation reservation = persistReservation(17, 20);
        Reservation OtherReservation = persistReservation(20, 22);

        //when
        boolean result1 = reservationRepository.existsOthersBetween(
                getTime(17), getTime(22), resource.getId(), reservation.getId());

        boolean result2 = reservationRepository.existsOthersBetween(
                getTime(17), getTime(20), resource.getId(), reservation.getId());

        //then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }


    private static LocalDateTime getTime(int hour) {
        return LocalDateTime.of(2023, 8, 10, hour, 0);
    }

    private Reservation persistReservation(int startHour, int endHour) {
        Reservation reservation = Reservation.builder()
                .clubMember(clubMember)
                .resource(resource)
                .period(new Period(getTime(startHour), getTime(endHour)))
                .build();

        entityManager.persist(reservation);
        return reservation;
    }
}

package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.ReservationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query(
            "select exists (" +
            "select r " +
            "from Reservation r " +
            "where r.resource.id = :resourceId " +
            "and r.status != 'REJECTED' " +
            "and ((r.period.startDateTime <= :start and :start < r.period.endDateTime) " +
            "or (:start <= r.period.startDateTime and r.period.startDateTime < :end))" +
            ")"
    )
    boolean existsBetween(@Param("start") LocalDateTime startDateTime, @Param("end") LocalDateTime endDateTime, @Param("resourceId") Long resourceId);


    @Query(
            "select exists (" +
                    "select r " +
                    "from Reservation r " +
                    "where r.resource.id = :resourceId " +
                    "and r.id != :reservationId " +
                    "and r.status != 'REJECTED' " +
                    "and ((r.period.startDateTime <= :start and :start < r.period.endDateTime) " +
                    "or (:start <= r.period.startDateTime and r.period.startDateTime < :end))" +
                    ")"
    )
    boolean existsOthersBetween(@Param("start") LocalDateTime startDateTime,
                                @Param("end") LocalDateTime endDateTime,
                                @Param("resourceId") Long resourceId,
                                @Param("reservationId") Long reservationId);


    @Query("select r " +
            "from Reservation r " +
            "join fetch r.clubMember cm " +
            "join fetch r.resource res " +
            "where r.resource.id = :resourceId " +
            "and ((r.period.startDateTime <= :start and :start < r.period.endDateTime) " +
            "or (:start <= r.period.startDateTime and r.period.startDateTime < :end)) " +
            "order by r.period.startDateTime asc , r.period.endDateTime asc")
    List<Reservation> findAllBetween(@Param("start") LocalDateTime startDateTime,
                                     @Param("end") LocalDateTime endDateTime,
                                     @Param("resourceId") Long resourceId);

    @Query("select r " +
            "from Reservation r " +
            "join fetch r.clubMember cm " +
            "join fetch r.resource res " +
            "where r.resource.id = :resourceId " +
            "and r.status = :status " +
            "and ((r.period.startDateTime <= :start and :start < r.period.endDateTime) " +
            "or (:start <= r.period.startDateTime and r.period.startDateTime < :end)) " +
            "order by r.period.startDateTime asc , r.period.endDateTime asc")
    List<Reservation> findAllBetweenAndStatus(@Param("start") LocalDateTime startDateTime,
                                     @Param("end") LocalDateTime endDateTime,
                                     @Param("resourceId") Long resourceId,
                                     @Param("status") ReservationStatus status);

    @Query("select r " +
            "from Reservation r " +
            "join fetch r.clubMember cm " +
            "join fetch r.resource res " +
            "where r.resource.id = :resourceId " +
            "and r.status != 'REJECTED' " +
            "and ((r.period.startDateTime <= :start and :start < r.period.endDateTime) " +
            "or (:start <= r.period.startDateTime and r.period.startDateTime < :end)) " +
            "order by r.period.startDateTime asc , r.period.endDateTime asc")
    List<Reservation> findAllBetweenForScheduler(@Param("start") LocalDateTime startDateTime,
                                              @Param("end") LocalDateTime endDateTime,
                                              @Param("resourceId") Long resourceId);

    @Query("select r " +
            "from Reservation r " +
            "join fetch r.clubMember cm " +
            "join fetch r.resource res " +
            "where r.resource.id = :resourceId " +
            "and r.status != 'CONFIRMED' " +
            "order by r.period.startDateTime asc, r.period.endDateTime asc")
    List<Reservation> findAllNotConfirmed(@Param("resourceId") Long resourceId);

    @Query("""
            SELECT r
            FROM Reservation r
            JOIN FETCH r.clubMember cm
            JOIN FETCH r.resource res
            WHERE res.club.id = :clubId and r.status = :status
            """)
    Slice<Reservation> findReservationsAdmin(@Param("clubId") Long clubId, @Param("status") ReservationStatus status, Pageable pageable);

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE (r.clubMember.id = :clubMemberId or r.id in (SELECT ri.reservation.id FROM ReservationInvitee ri WHERE ri.clubMember.id = :clubMemberId))
            and r.period.startDateTime >= :startDateTime and r.status != 'REJECTED'
            """)
    Slice<Reservation> findMyReservationsAfter(@Param("clubMemberId") Long clubMemberId, @Param("startDateTime") LocalDateTime startDateTime, Pageable pageable);

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE (r.clubMember.id = :clubMemberId or r.id in (SELECT ri.reservation.id FROM ReservationInvitee ri WHERE ri.clubMember.id = :clubMemberId))
            and r.period.startDateTime < :startDateTime and r.status != 'REJECTED'
            """)
    Slice<Reservation> findMyReservationsBefore(@Param("clubMemberId") Long clubMemberId, @Param("startDateTime") LocalDateTime startDateTime, Pageable pageable);



    /**
     *  스케줄링 관련 메서드
     */
    @Query("""
           SELECT r
           FROM Reservation r
           LEFT JOIN FETCH r.reservationInvitees ri
           WHERE r.status = 'CONFIRMED' AND (r.period.startDateTime BETWEEN :now AND :plusMinutes)
           """)
    List<Reservation> findAllAboutToStart(@Param("now") LocalDateTime now, @Param("plusMinutes") LocalDateTime plusMinutes);

    @Query("""
           SELECT r
           FROM Reservation r
           LEFT JOIN FETCH r.reservationInvitees ri
           WHERE r.status = 'CONFIRMED' AND (r.period.endDateTime BETWEEN :now AND :plusMinutes)
           """)
    List<Reservation> findAllAboutToFinish(@Param("now") LocalDateTime now, @Param("plusMinutes") LocalDateTime plusMinutes);

    @Query(
            """
            SELECT r
            FROM Reservation r
            JOIN FETCH r.resource rs
            WHERE r.status = 'CONFIRMED' AND rs.returnMessageRequired = true
            AND r.isReturned = false AND r.period.endDateTime  between  :time1 and :time2
            """
    )
    List<Reservation> findAllNotReturned(@Param("time1") LocalDateTime time1, @Param("time2") LocalDateTime time2);


    @Query(
            """
            select r
            from Reservation r
            where r.status = 'REQUEST' AND r.period.endDateTime  <= :now
            """
    )
    List<Reservation> findNotConfirmedReservationStatus(@Param("now") LocalDateTime now);
    /**
     *  스케줄링 관련 메서드
     */
}

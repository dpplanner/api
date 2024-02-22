package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.ReservationStatus;
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
           LEFT JOIN FETCH r.reservationInvitees ri
           WHERE r.status = 'CONFIRMED' AND (r.period.startDateTime BETWEEN :now AND :nowPlus10Minutes)
           """)
    List<Reservation> findAllAboutToStart(@Param("now") LocalDateTime now, @Param("nowPlus10Minutes") LocalDateTime nowPlus10Minutes);

    @Query("""
           SELECT r
           FROM Reservation r
           LEFT JOIN FETCH r.reservationInvitees ri
           WHERE r.status = 'CONFIRMED' AND (r.period.endDateTime BETWEEN :now AND :nowPlus10Minutes)
           """)
    List<Reservation> findAllAboutToFinish(@Param("now") LocalDateTime now, @Param("nowPlus10Minutes") LocalDateTime nowPlus10Minutes);

    @Query(
            """
            SELECT r
            FROM Reservation r
            WHERE r.status = 'CONFIRMED' AND r.isReturned = false AND r.period.endDateTime  >= :now
            """
    )
    List<Reservation> findAllNotReturned(@Param("now") LocalDateTime now);
}

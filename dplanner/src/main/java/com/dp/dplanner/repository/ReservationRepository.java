package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query(
            "select exists (" +
            "select r " +
            "from Reservation r " +
            "where r.resource.id = :resourceId " +
            "and (r.period.startDateTime <= :start and :start < r.period.endDateTime) " +
            "or (:start <= r.period.startDateTime and r.period.startDateTime < :end)" +
            ")"
    )
    boolean existsBetween(@Param("start") LocalDateTime startDateTime, @Param("end") LocalDateTime endDateTime, @Param("resourceId") Long resourceId);

}

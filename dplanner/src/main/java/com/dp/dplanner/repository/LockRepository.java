package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LockRepository extends JpaRepository<Lock, Long> {

    @Query("select l " +
            "from Lock l " +
            "where l.resource.id = :resourceId " +
            "and (( l.period.startDateTime <= :start and :start < l.period.endDateTime) or (:start <= l.period.startDateTime AND l.period.startDateTime < :end )) " +
            "order by l.period.startDateTime asc , l.period.endDateTime asc")
    List<Lock> findLocksBetween(@Param(value = "start") LocalDateTime start, @Param(value = "end") LocalDateTime end,@Param(value = "resourceId") Long resourceId);

    @Query("select exists (select l " +
            "from Lock l " +
            "where l.resource.id = :resourceId " +
            "and (( l.period.startDateTime <= :start and :start < l.period.endDateTime) or (:start <= l.period.startDateTime AND l.period.startDateTime < :end )) " +
            "order by l.period.startDateTime asc , l.period.endDateTime asc)")
    boolean existsLocksBetween(@Param(value = "start") LocalDateTime start, @Param(value = "end") LocalDateTime end,@Param(value = "resourceId") Long resourceId);
}

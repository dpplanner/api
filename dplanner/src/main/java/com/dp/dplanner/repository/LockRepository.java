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
            "and ((:start < l.period.startDateTime and l.period.startDateTime < :end) or (:start < l.period.endDateTime AND l.period.endDateTime <  :end ))" +
            "order by l.period.startDateTime asc , l.period.endDateTime asc")
    List<Lock> findLocksBetween(@Param(value = "start") LocalDateTime start, @Param(value = "end") LocalDateTime end,@Param(value = "resourceId") Long resourceId);




}

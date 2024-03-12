package com.dp.dplanner.repository;

import com.dp.dplanner.domain.message.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<PrivateMessage,Long> {


    @Query("SELECT pm FROM PrivateMessage pm " +
            "WHERE pm.clubMember.id = :clubMemberId " +
            "AND pm.createdDate >= :sixMonthsAgo " +
            "ORDER BY pm.createdDate desc ")
    List<PrivateMessage> findAll(@Param("clubMemberId") Long clubMemberId,@Param("sixMonthsAgo") LocalDateTime sixMonthsAgo);

}

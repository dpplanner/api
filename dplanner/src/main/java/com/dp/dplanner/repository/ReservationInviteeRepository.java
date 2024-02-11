package com.dp.dplanner.repository;

import com.dp.dplanner.domain.ReservationInvitee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;



public interface ReservationInviteeRepository extends JpaRepository<ReservationInvitee,Long> {

//    @Modifying
//    @Query(value =
//            "INSERT INTO reservation_invitee (reservation_id, club_member_id) " +
//            "SELECT :reservationId, cm.id FROM club_member cm WHERE cm.id IN :clubMemberIds",
//            nativeQuery = true
//    )
//    int saveAll(List<Long> clubMemberIds, Long reservationId);


    List<ReservationInvitee> findAllByReservationId(Long reservationId);

    int deleteReservationInviteeByReservationId(Long reservationId);


}

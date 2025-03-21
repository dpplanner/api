package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationInvitee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clubMember_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    ClubMember clubMember;

    @Builder
    public ReservationInvitee(Reservation reservation, ClubMember clubMember) {
        this.reservation = reservation;
        this.clubMember = clubMember;
        reservation.getReservationInvitees().add(this);
    }
}

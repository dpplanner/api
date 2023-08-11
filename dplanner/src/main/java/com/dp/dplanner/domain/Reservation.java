package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.dp.dplanner.domain.ReservationStatus.*;
import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id")
    private ClubMember clubMember;

    @Embedded
    private Period period;
    private boolean sharing;
    private String title;
    private String usage;
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Builder
    public Reservation(Resource resource, ClubMember clubMember, Period period, boolean sharing, String title, String usage) {
        setResource(resource);
        this.clubMember = clubMember;
        this.period = period;
        this.sharing = sharing;
        this.title = title;
        this.usage = usage;
        this.status = CREATE;
    }

    private void setResource(Resource resource) {
        this.resource = resource;
        resource.getReservations().add(this);
    }

    public void confirm() {
        this.status = CONFIRMED;
    }

    public void update(String title, String usage, LocalDateTime startDateTime, LocalDateTime endDateTime, boolean sharing) {
        this.title = title;
        this.usage = usage;
        this.sharing = sharing;
        this.period = new Period(startDateTime, endDateTime);
        this.status = UPDATE;
    }

    public void cancel() {
        this.status = CANCEL;
    }

    public boolean isConfirmed() {
        return this.status.equals(CONFIRMED);
    }
}

package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.dp.dplanner.domain.ReservationStatus.*;
import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_member_id")
    private ClubMember clubMember;

    @OneToMany(mappedBy = "reservation",cascade = CascadeType.REMOVE)
    private List<Attachment> attachments = new ArrayList();
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.REMOVE)
    private List<ReservationInvitee> reservationInvitees = new ArrayList<>();

    private String returnMessage;

    @Embedded
    private Period period;
    private boolean sharing;
    private boolean isReturned = false;
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
        this.status = REQUEST;
    }

    private void setResource(Resource resource) {
        this.resource = resource;
    }

    public void confirm() {
        this.status = CONFIRMED;
    }

    public void update(String title, String usage, LocalDateTime startDateTime, LocalDateTime endDateTime, boolean sharing) {
        this.title = title;
        this.usage = usage;
        this.sharing = sharing;
        this.period = new Period(startDateTime, endDateTime);
        this.status = REQUEST;
    }

    public void updateNotChangeStatus(String title, String usage, LocalDateTime startDateTime, LocalDateTime endDateTime, boolean sharing) {
        this.title = title;
        this.usage = usage;
        this.sharing = sharing;
        this.period = new Period(startDateTime, endDateTime);
    }


    public void reject() {
        this.status = REJECTED;
    }

    public boolean isConfirmed() {
        return this.status.equals(CONFIRMED);
    }

    public void returned(String returnMessage) {
        this.returnMessage = returnMessage;
        this.isReturned = true;
    }

    public void clearInvitee() {
        this.reservationInvitees = new ArrayList<>();
    }
}

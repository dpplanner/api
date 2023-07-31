package com.dp.dplanner.domain.club;

import com.dp.dplanner.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="club_member_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="member_id")
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="club_id")
    private Club club;

    @Enumerated(EnumType.STRING)
    private ClubRole role;

    private Boolean isConfirmed;

    @Builder
    public ClubMember(Member member, Club club) {
        this.member = member;
        setClub(club);
        this.role = ClubRole.USER;
        this.isConfirmed = false;
    }

    private void setClub(Club club) {
        this.club = club;
        club.getMembers().add(this);
    }

    public void setAdmin() {
        this.role = ClubRole.ADMIN;
    }

    public void setManager() {
        this.role = ClubRole.MANAGER;
    }


}

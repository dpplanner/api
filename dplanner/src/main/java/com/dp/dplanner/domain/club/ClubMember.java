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

    private String name;
    private String info;

    @Enumerated(EnumType.STRING)
    private ClubRole role;

    private Boolean isConfirmed;

    @Builder
    public ClubMember(Member member, Club club, String name, String info) {
        setMember(member);
        setClub(club);
        this.name = name;
        this.info = info;
        this.role = ClubRole.USER;
        this.isConfirmed = false;
    }

    private void setMember(Member member) {
        this.member = member;
        member.getClubMembers().add(this);
    }

    private void setClub(Club club) {
        this.club = club;
        club.getClubMembers().add(this);
    }

    public void setAdmin() {
        this.role = ClubRole.ADMIN;
    }

    public void setManager() {
        changeRole(ClubRole.MANAGER);
    }

    public void confirm() {
        this.isConfirmed = true;
    }

    public boolean checkRoleIs(ClubRole role) {
        return this.role == role;
    }

    public boolean checkRoleIsNot(ClubRole role) {
        return this.role != role;
    }

    public void changeRole(ClubRole role) {
        this.role = role;
    }

    public boolean isConfirmed() {
        return this.isConfirmed;
    }
}

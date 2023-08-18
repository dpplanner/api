package com.dp.dplanner.domain.club;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Resource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.dp.dplanner.domain.club.ClubRole.*;
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
        this.role = USER;
        this.isConfirmed = false;
    }

    public static ClubMember createClubMember(Member member, Club club) {
        return ClubMember.builder().club(club).member(member).build();
    }

    public static ClubMember createAdmin(Member member, Club club) {
        ClubMember clubMember = createClubMember(member, club);
        clubMember.setAdmin();
        clubMember.confirm();
        return clubMember;
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
        this.role = ADMIN;
    }

    public void setManager() {
        changeRole(MANAGER);
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

    public boolean hasAuthority(ClubAuthorityType authority) {
        return this.checkRoleIs(ADMIN)
                || (this.checkRoleIs(MANAGER) && this.club.hasAuthority(authority));
    }

    public boolean isSameClub(Long clubId) {
        return this.club.getId().equals(clubId);
    }

    public boolean isSameClub(ClubMember clubMember) {
        return this.club.equals(clubMember.getClub());
    }

    public boolean isSameClub(Resource resource) {
        return this.club.equals(resource.getClub());
    }

    public void update(String name, String info) {
        this.name = name;
        this.info = info;
    }
}

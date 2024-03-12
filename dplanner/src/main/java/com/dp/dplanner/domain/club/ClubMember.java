package com.dp.dplanner.domain.club;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Reservation;
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
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="member_id")
    private Member member;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="club_id")
    private Club club;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_authority_id")
    private ClubAuthority clubAuthority;

    @Enumerated(EnumType.STRING)
    private ClubRole role;
    private String name;
    private String info;
    private Boolean isConfirmed;
    private String url;

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
        return ClubMember.builder()
                .club(club)
                .member(member)
                .name(member.getName())
                .build();
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

    public void changeRole(ClubRole role) {
        this.role = role;
    }

    public boolean isConfirmed() {
        return this.isConfirmed;
    }

    public boolean hasAuthority(ClubAuthorityType authority) {
        return this.checkRoleIs(ADMIN)
                || (this.checkRoleIs(MANAGER) && this.clubAuthority != null && this.clubAuthority.hasAuthority(authority));
    }
    public boolean hasRole(ClubRole role) {
        return this.checkRoleIs(ADMIN)
        || this.checkRoleIs(role);
    }
    public boolean isSameClub(Long clubId) {
        return this.club.getId().equals(clubId);
    }

    public boolean isSameClub(ClubMember clubMember) {
        return this.club.getId().equals(clubMember.getClub().getId());
    }

    public boolean isSameClub(Resource resource) {
        return this.club.getId().equals(resource.getClub().getId());
    }
    public boolean isSameClub(Reservation reservation) {
        return this.club.getId().equals(reservation.getResource().getClub().getId());
    }

    public void update(String name, String info) {
        this.name = name;
        this.info = info;
    }
    public void updateProfileUrl(String url) {
        this.url = url;
    }
    public void updateClubAuthority(ClubAuthority clubAuthority) {
        this.clubAuthority = clubAuthority;
    }
}

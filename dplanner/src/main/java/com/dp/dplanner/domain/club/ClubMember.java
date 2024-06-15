package com.dp.dplanner.domain.club;

import com.dp.dplanner.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import static com.dp.dplanner.domain.club.ClubRole.*;
import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE club_member set is_deleted=true, is_confirmed=false, name='(탈퇴한 회원)', info='탈퇴한 회원입니다.', club_authority_id=null, role='USER', url =null where id = ?")
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
    private Boolean isDeleted;

    @Builder
    public ClubMember(Member member, Club club, String name, String info) {
        this.member = member;
        member.getClubMembers().add(this);
        this.club = club;
        club.getClubMembers().add(this);
        this.name = name;
        this.info = info;
        this.role = USER;
        this.isConfirmed = false;
        this.isDeleted = false;
    }

    public static ClubMember createClubMember(Member member, Club club) {
        return ClubMember.builder()
                .club(club)
                .member(member)
                .name(member.getName())
                .build();
    }

    public void confirm() {
        this.isConfirmed = true;
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
    public void changeRole(ClubRole role) { this.role = role;}
    public boolean checkRoleIs(ClubRole role) {
        return this.role == role;
    }
    public boolean hasAuthority(ClubAuthorityType authority) {
        return this.checkRoleIs(ADMIN)
                || (this.checkRoleIs(MANAGER) && this.clubAuthority != null && this.clubAuthority.hasAuthority(authority));
    }
    public boolean isSameClub(Long clubId) {
        return this.club.getId().equals(clubId);
    }
}

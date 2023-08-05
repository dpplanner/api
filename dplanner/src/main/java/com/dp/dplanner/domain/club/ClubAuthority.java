package com.dp.dplanner.domain.club;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "authority_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="club_id")
    private Club club;

    @Enumerated(EnumType.STRING)
    private ClubAuthorityType clubAuthorityType;

    @Builder
    public ClubAuthority(Club club, ClubAuthorityType clubAuthorityType) {
        this.club = club;
        this.clubAuthorityType = clubAuthorityType;
    }

    public static List<ClubAuthority> createAuthorities(Club club, List<ClubAuthorityType> authorityTypes) {
        List<ClubAuthority> authorities = new ArrayList<>();

        for (ClubAuthorityType authorityType : authorityTypes) {
            ClubAuthority clubAuthority = ClubAuthority.builder()
                    .club(club)
                    .clubAuthorityType(authorityType)
                    .build();

            authorities.add(clubAuthority);
        }
        club.getManagerAuthorities().clear();
        club.getManagerAuthorities().addAll(authorities);

        return authorities;
    }
}

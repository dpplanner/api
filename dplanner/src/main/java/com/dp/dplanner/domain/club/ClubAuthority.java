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
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name="club_id")
    private Club club;

    @ElementCollection(targetClass = ClubAuthorityType.class)
    @Enumerated(EnumType.STRING)
    private List<ClubAuthorityType> clubAuthorityTypes = new ArrayList<>();
    private String name;
    private String description;

    @Builder
    public ClubAuthority(Club club, List<ClubAuthorityType> clubAuthorityTypes,String name, String description) {
        this.club = club;
        this.clubAuthorityTypes = clubAuthorityTypes;
        this.name = name;
        this.description = description;

    }

    public void update(List<ClubAuthorityType> clubAuthorityTypes, String name, String description) {
        this.clubAuthorityTypes = clubAuthorityTypes;
        this.name = name;
        this.description = description;
    }

    public boolean hasAuthority(ClubAuthorityType authority) {
        return clubAuthorityTypes.contains(authority);
    }
}

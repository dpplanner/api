package com.dp.dplanner.domain.club;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club {
    @Id
    @GeneratedValue
    private Long id;

    private String clubName;

    private String info;

    @OneToMany(mappedBy = "club", cascade = CascadeType.REMOVE)
    private List<ClubMember> clubMembers = new ArrayList<>();

    @Builder
    public Club(String clubName, String info) {
        this.clubName = clubName;
        this.info = info;
    }
}

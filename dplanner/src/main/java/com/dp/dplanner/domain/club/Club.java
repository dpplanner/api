package com.dp.dplanner.domain.club;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club {
    @Id
    @GeneratedValue
    private Long id;

    private String clubName;

    private String info;
    private String url;

    @OneToMany(mappedBy = "club")
    private List<ClubMember> clubMembers = new ArrayList<>();

    @Builder
    public Club(String clubName, String info) {
        this.clubName = clubName;
        this.info = info;
    }

    public Club updateInfo(String updatedClubInfo) {
        this.info = updatedClubInfo;
        return this;
    }

    public void updateUrl(String url) {
        this.url = url;
    }

}

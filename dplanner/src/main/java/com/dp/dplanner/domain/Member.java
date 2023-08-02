package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.ClubMember;
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
public class Member {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String info;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
    private List<ClubMember> clubMembers = new ArrayList<>();

    @Builder
    public Member(String name, String info) {
        this.name = name;
        this.info = info;
    }
}

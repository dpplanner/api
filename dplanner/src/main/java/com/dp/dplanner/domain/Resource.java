package com.dp.dplanner.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long id;

    private String name;
    private String info;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.REMOVE)
    private List<Lock> locks = new ArrayList<>();

    @OneToMany(mappedBy = "resource", cascade = CascadeType.REMOVE)
    private List<Reservation> reservations = new ArrayList<>();

}

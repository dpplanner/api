package com.dp.dplanner.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class Club {
    @Id
    @GeneratedValue
    private Long id;

    private String clubName;


}

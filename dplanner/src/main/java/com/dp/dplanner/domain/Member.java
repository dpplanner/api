package com.dp.dplanner.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class Member {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String info;





}

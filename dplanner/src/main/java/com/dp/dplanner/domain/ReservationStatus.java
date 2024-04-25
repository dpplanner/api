package com.dp.dplanner.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReservationStatus {
    REQUEST, CONFIRMED, REJECTED;

    @JsonCreator
    public static ReservationStatus ReservationStatusFrom(String s) {
        return ReservationStatus.valueOf(s.toUpperCase());
    }

}

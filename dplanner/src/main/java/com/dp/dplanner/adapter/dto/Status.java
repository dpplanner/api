package com.dp.dplanner.adapter.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Status {
    LIKE,DISLIKE;

    @JsonCreator
    public static Status StatusFrom(String s) {
        return Status.valueOf(s.toUpperCase());
    }
}

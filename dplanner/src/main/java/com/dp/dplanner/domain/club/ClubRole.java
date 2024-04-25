package com.dp.dplanner.domain.club;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ClubRole {
    ADMIN, MANAGER, USER, NONE;

    @JsonCreator
    public static ClubRole ClubRoleFrom(String s) {
        return ClubRole.valueOf(s.toUpperCase());
    }
}

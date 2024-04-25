package com.dp.dplanner.domain.club;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ClubAuthorityType {
    MEMBER_ALL, SCHEDULE_ALL, POST_ALL, RESOURCE_ALL, NONE;

    @JsonCreator
    public static ClubAuthorityType ClubAuthorityTypeFrom(String s) {
        return ClubAuthorityType.valueOf(s.toUpperCase());
    }
}

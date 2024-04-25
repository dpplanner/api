package com.dp.dplanner.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ResourceType {
    PLACE, THING;

    @JsonCreator
    public static ResourceType ResourceTypeFrom(String s) {
        return ResourceType.valueOf(s.toUpperCase());
    }
}

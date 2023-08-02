package com.dp.dplanner.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.time.LocalDateTime;

@Embeddable
@Getter
public class Period{

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    protected Period() {
    }

    public Period(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }
}

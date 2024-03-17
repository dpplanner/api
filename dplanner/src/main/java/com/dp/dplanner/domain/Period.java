package com.dp.dplanner.domain;

import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.service.exception.ServiceException;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Period{

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    public Period(LocalDateTime startDateTime, LocalDateTime endDateTime) {

        check(startDateTime, endDateTime);
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    private void check(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime.isAfter(endDateTime) || startDateTime.equals(endDateTime)) {
            throw new ServiceException(ErrorResult.REQUEST_IS_INVALID);
        }
    }

    @Override
    public boolean equals(Object obj) {
        Period period = (Period) obj;
        return this.startDateTime.isEqual(period.startDateTime) && this.endDateTime.isEqual(period.endDateTime);
    }

}

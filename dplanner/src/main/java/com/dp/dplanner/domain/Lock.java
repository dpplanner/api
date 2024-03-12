package com.dp.dplanner.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lock extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Embedded
    private Period period;

    private String message;

    @Builder
    public Lock(Resource resource, Period period, String message) {
        setResource(resource);
        this.period = period;
        this.message = message;
    }

    private void setResource(Resource resource) {
        this.resource = resource;
    }

    public void update(Period period, String message) {
        this.period = period;
        this.message = message;
    }
}

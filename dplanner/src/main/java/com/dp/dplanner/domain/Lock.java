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
    @Column(name = "lock_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Embedded
    private Period period;

    @Builder
    public Lock(Resource resource, Period period) {
        setResource(resource);
        this.period = period;
    }

    private void setResource(Resource resource) {
        this.resource = resource;
        resource.getLocks().add(this);
    }

    public void update(Period period) {
        this.period = period;
    }
}

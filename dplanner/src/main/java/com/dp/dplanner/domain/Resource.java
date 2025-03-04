package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.Club;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.*;

@Getter
@Entity
@Table(name = "resources")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String info;
    private boolean returnMessageRequired = false;
    @Column(columnDefinition = "text")
    private String notice;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;
    private Long bookableSpan;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Club club;

    @OneToMany(mappedBy = "resource",cascade = CascadeType.REMOVE)
    private List<Reservation> reservations = new ArrayList();

    @OneToMany(mappedBy = "resource",cascade = CascadeType.REMOVE)
    private List<Lock> locks = new ArrayList();

    @Builder
    public Resource(String name, String info, Club club,boolean returnMessageRequired,String notice,ResourceType resourceType,Long bookableSpan) {
        this.name = name;
        this.info = info;
        this.club = club; // 양방향 연관관계는 아니기 때문에 연관관계 메서드 사용 X
        this.returnMessageRequired = returnMessageRequired;
        this.notice = notice;
        this.resourceType = resourceType;
        this.bookableSpan = bookableSpan;
    }

    public void update(String name, String info, boolean returnMessageRequired,String notice, ResourceType resourceType,Long bookableSpan) {
        this.name = name;
        this.info = info;
        this.returnMessageRequired = returnMessageRequired;
        this.notice = notice;
        this.resourceType = resourceType;
        this.bookableSpan = bookableSpan;
    }
}

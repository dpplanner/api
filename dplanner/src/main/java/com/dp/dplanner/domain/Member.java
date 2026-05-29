package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE member set is_deleted=true where id = ?")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String name;
    private String refreshToken;
    private String fcmToken;
    private Boolean isDeleted;
    // 서비스 이용 약관 동의 여부
    @Column(columnDefinition = "boolean default false")
    private Boolean eula;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recent_club_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Club recentClub;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
    private List<ClubMember> clubMembers = new ArrayList<>();

    @Builder
    public Member(String email, String name, String refreshToken) {
        this.email = email;
        this.name = name;
        this.refreshToken = refreshToken;
        this.isDeleted = false;
        this.eula = false;
    }

    public void updateRecentClub(Club club) {
        this.recentClub = club;
    }

    public void updateFCMToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void agreeEula() {
        this.eula = true;
    }
}

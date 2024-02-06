package com.dp.dplanner.domain.message;

import com.dp.dplanner.domain.BaseEntity;
import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivateMessage extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id")
    private ClubMember clubMember;

    private String title;

    private String content;

    private String redirectUrl; // 앱 내에서 redirectUrl

    private Boolean isRead; // 읽음 여부

    @Builder
    public PrivateMessage(ClubMember clubMember, String title, String content, String redirectUrl) {
        this.clubMember = clubMember;
        this.title = title;
        this.content = content;
        this.redirectUrl = redirectUrl;
        this.isRead = false;
    }

    public void read() {
        this.isRead = true;
    }
}

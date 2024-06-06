package com.dp.dplanner.domain.message;

import com.dp.dplanner.domain.BaseEntity;
import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    @Enumerated(EnumType.STRING)
    private InfoType infoType;
    @Enumerated(EnumType.STRING)
    private MessageType type;

    private String info;

    public void read() {
        this.isRead = true;
    }
}

package com.dp.dplanner.domain;


import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostMemberLike {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClubMember clubMember;

    @Builder
    public PostMemberLike(Post post, ClubMember clubMember) {
        this.post = post;
        this.clubMember = clubMember;
    }
}

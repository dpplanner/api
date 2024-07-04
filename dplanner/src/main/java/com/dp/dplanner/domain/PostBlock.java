package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostBlock {

    @Builder
    public PostBlock(Post post, ClubMember clubMember) {
        this.id = new PostBlockId(post, clubMember);
    }

    @EmbeddedId
    private PostBlockId id;

    @Getter
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostBlockId implements Serializable {

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "post_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private Post post;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "club_member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private ClubMember clubMember;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PostBlockId that = (PostBlockId) o;
            return Objects.equals(post.getId(), that.post.getId()) && Objects.equals(clubMember.getId(), that.clubMember.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(post.getId(), clubMember.getId());
        }
    }
}

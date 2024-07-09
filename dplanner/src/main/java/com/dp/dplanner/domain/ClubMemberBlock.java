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
public class ClubMemberBlock {
    @EmbeddedId
    private ClubMemberBlockId id;
    @Builder
    public ClubMemberBlock(ClubMember blockedClubMember, ClubMember clubMember) {
        this.id = new ClubMemberBlockId(blockedClubMember, clubMember);
    }
    @Getter
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClubMemberBlockId implements Serializable {


        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "blocked_club_member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private ClubMember blockedClubMember;

        @ManyToOne(fetch = LAZY)
        @JoinColumn(name = "club_member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private ClubMember clubMember;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClubMemberBlockId that = (ClubMemberBlockId) o;
            return Objects.equals(blockedClubMember.getId(), that.blockedClubMember.getId()) && Objects.equals(clubMember.getId(), that.clubMember.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(blockedClubMember.getId(), clubMember.getId());
        }
    }
}

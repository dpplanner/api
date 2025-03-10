package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
    private Boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClubMember clubMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Club club;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Comment parent;


    @OneToMany(mappedBy = "parent", orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    @OneToMany(mappedBy = "comment",cascade = CascadeType.REMOVE)
    private List<CommentMemberLike> commentMemberLikes = new ArrayList<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.REMOVE)
    private List<CommentReport> commentReports = new ArrayList<>();


    @Builder
    public Comment(ClubMember clubMember, Post post, Comment parent, String content, Club club) {
        setPost(post);
        if (parent != null) {
            addChildren(parent);
        }
        this.clubMember = clubMember;
        this.parent = parent;
        this.content = content;
        this.club = club;
        this.isDeleted = false;
    }

    public void update(String content) {
        this.content = content;
    }
    public void delete() {
        this.isDeleted = true;
    }

    private void setPost(Post post) {
        post.getComments().add(this);
        this.post = post;
    }
    private void addChildren(Comment parent) {
        parent.getChildren().add(this);
        this.parent = parent;
    }
}

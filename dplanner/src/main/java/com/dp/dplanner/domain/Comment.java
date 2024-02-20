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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity{

    @Id
    @GeneratedValue
    private Long id;
    private String content;
    private Boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id")
    private ClubMember clubMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;


    @OneToMany(mappedBy = "parent", orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    @OneToMany(mappedBy = "comment",cascade = CascadeType.REMOVE)
    private List<CommentMemberLike> commentMemberLikes = new ArrayList<>();

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

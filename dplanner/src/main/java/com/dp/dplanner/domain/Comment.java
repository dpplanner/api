package com.dp.dplanner.domain;

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

//    public static final Comment EMPTY = new Comment();
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id")
    private ClubMember clubMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    private String content;

    @OneToMany(mappedBy = "parent" , cascade = CascadeType.REMOVE)
    private List<Comment> children = new ArrayList<>();


    @Builder
    public Comment(ClubMember clubMember, Post post, Comment parent, String content) {
        setPost(post);
        if (parent != null) {
            addChildren(parent);
        }
        this.clubMember = clubMember;
        this.parent = parent;
        this.content = content;
    }

    private void setPost(Post post) {
        post.getComments().add(this);
        this.post = post;
    }
    private void addChildren(Comment parent) {
        parent.getChildren().add(this);
        this.parent = parent;
    }


    public void update(String content) {
        this.content = content;
    }
}

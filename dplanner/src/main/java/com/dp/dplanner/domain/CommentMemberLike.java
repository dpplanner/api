package com.dp.dplanner.domain;


import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class CommentMemberLike {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

}

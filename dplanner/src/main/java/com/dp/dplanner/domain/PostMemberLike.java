package com.dp.dplanner.domain;


import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class PostMemberLike {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

}

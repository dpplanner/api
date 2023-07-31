package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.Club;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Post {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String content;
    private Boolean isFixed;

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<Attachment> attachments = new ArrayList();

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList();

}

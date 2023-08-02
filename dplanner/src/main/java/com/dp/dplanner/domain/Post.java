package com.dp.dplanner.domain;

import com.dp.dplanner.dto.PostDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private Boolean isFixed = false;

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<Attachment> attachments = new ArrayList();

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList();

    @Builder
    public Post(Club club, Member member, String content, Boolean isFixed) {
        this.club = club;
        this.member = member;
        this.content = content;
        this.isFixed = isFixed;
    }



    public void update(PostDto.Update update) {

        this.content = update.getContent();

    }
}

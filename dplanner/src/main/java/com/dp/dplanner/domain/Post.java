package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.dto.PostDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity{
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="club_id")
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id")
    private ClubMember clubMember;

    private String content;
    private Boolean isFixed;

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<Attachment> attachments = new ArrayList();

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList();

    @Builder
    public Post(Club club, ClubMember clubMember, String content, Boolean isFixed) {
        this.club = club;
        this.clubMember = clubMember;
        this.content = content;

        if (isFixed == null) {
            this.isFixed = false;
        }else{
            this.isFixed = isFixed;
        }
    }



    public void update(PostDto.Update update) {

        this.content = update.getContent();

    }

    public void toggleIsFixed() {
        isFixed = !isFixed;
    }
}

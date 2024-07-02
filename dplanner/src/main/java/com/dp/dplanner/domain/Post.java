package com.dp.dplanner.domain;

import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
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
    @JoinColumn(name = "club_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ClubMember clubMember;

    @Column(columnDefinition = "text")
    private String content;
    private String title;
    private Boolean isFixed;

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<Attachment> attachments = new ArrayList();

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList();

    @OneToMany(mappedBy = "post",cascade = CascadeType.REMOVE)
    private List<PostMemberLike> postMemberLikes = new ArrayList();

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE)
    private List<PostReport> postReports = new ArrayList<>();

    @Builder
    public Post(Club club, ClubMember clubMember, String content,String title, Boolean isFixed) {
        this.club = club;
        this.clubMember = clubMember;
        this.content = content;
        this.title = title;

        if (isFixed == null) {
            this.isFixed = false;
        }else{
            this.isFixed = isFixed;
        }
    }

    public void removeAttachment(Attachment attachment) {
        this.attachments.remove(attachment);
    }

    public void updatePost(String title, String content) {
        this.title = title;
        this.content = content;

    }

    public void toggleIsFixed() {
        isFixed = !isFixed;
    }
}

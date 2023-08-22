package com.dp.dplanner.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment extends BaseEntity {


    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    Post post;

    @Column(unique = true)
    String url;
    FileType type;

    @Builder
    public Attachment(Post post, String url, FileType type) {
        setPost(post);
        this.url = url;
        this.type = type;
    }

    private void setPost(Post post) {
        this.post = post;
        post.getAttachments().add(this);
    }

}

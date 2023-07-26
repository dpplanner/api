package com.dp.dplanner.domain;

import jakarta.persistence.*;
import lombok.Getter;


@Entity
@Getter
public class Attachment {


    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    Post post;

    String url;
    FileType type;
}

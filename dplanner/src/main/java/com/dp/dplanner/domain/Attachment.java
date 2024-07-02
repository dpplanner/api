package com.dp.dplanner.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
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
    @JoinColumn(name = "post_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    Reservation reservation;


    @Column(unique = true)
    String url;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    FileType type;

    public Attachment(Post post, String url, FileType type) {
        setPost(post);
        this.url = url;
        this.type = type;
    }
    public Attachment(Reservation reservation, String url, FileType type) {
        setReservation(reservation);
        this.url = url;
        this.type = type;
    }

    private void setPost(Post post) {
        this.post = post;
        post.getAttachments().add(this);
    }

    private void setReservation(Reservation reservation) {
        this.reservation = reservation;
        reservation.getAttachments().add(this);
    }

}

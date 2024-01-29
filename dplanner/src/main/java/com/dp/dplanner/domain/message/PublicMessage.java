package com.dp.dplanner.domain.message;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicMessage {

    @Id
    @GeneratedValue
    private Long id;

    private String title;

    private String content;

    private String redirectUrl; // 앱 내에서 redirectUrl

}

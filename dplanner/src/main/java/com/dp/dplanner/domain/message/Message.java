package com.dp.dplanner.domain.message;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Message {

    private String title;
    private String content;
    private String redirectUrl;

    @Builder
    public Message(String title, String content, String redirectUrl) {
        this.title = title;
        this.content = content;
        this.redirectUrl = redirectUrl;
    }
}

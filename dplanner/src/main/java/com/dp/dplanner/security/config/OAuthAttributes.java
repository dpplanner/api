package com.dp.dplanner.security.config;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;


@Getter
public class OAuthAttributes {

    private String name;
    private String email;
    private String nameAttributeKey;
    private Map<String, Object> attributes;

    @Builder
    public OAuthAttributes(String name, String email, String nameAttributeKey, Map<String, Object> attributes) {
        this.name = name;
        this.email = email;
        this.nameAttributeKey = nameAttributeKey;
        this.attributes = attributes;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if (registrationId.equals("google")) {
            return ofGoogle(userNameAttributeName, attributes);
        }
        return null;
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {

        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }
}



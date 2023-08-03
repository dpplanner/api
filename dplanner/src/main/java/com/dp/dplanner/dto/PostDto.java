package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Post;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
public class PostDto {


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create{

        private String content;

        public Post toEntity() {
            return Post.builder()
                    .content(this.content)
                    .build();
        }



    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response{
        private Long id;
        private String content;
        private Boolean isFixed;
        private Long clubId;

        // Entity -> DTO
        public static PostDto.Response of(Post post) {

            return Response.builder()
                    .id(post.getId())
                    .content(post.getContent())
                    .isFixed(post.getIsFixed())
                    .clubId(post.getClub().getId())
                    .build();
        }

        public static List<PostDto.Response> ofList(List<Post> posts) {
            return posts.stream().map(PostDto.Response::of)
                    .collect(Collectors.toList());
        }
    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Update {
        private Long id;
        private String content;


    }
}

package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import lombok.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

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
        private long clubId;

        public Post toEntity(ClubMember clubMember, Club club) {
            return Post.builder()
                    .clubMember(clubMember)
                    .club(club)
                    .content(this.content)
                    .isFixed(false)
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
        public static Response of(Post post) {

            return Response.builder()
                    .id(post.getId())
                    .content(post.getContent())
                    .isFixed(post.getIsFixed())
                    .clubId(post.getClub().getId())
                    .build();
        }

        public static List<Response> ofList(List<Post> posts) {
            return posts.stream().map(Response::of)
                    .collect(Collectors.toList());
        }
    }

    @Getter
    @Setter
    public static class SliceResponse extends SliceImpl<Response> {
        public SliceResponse(List content, Pageable pageable, boolean hasNext) {
            super(content, pageable, hasNext);
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

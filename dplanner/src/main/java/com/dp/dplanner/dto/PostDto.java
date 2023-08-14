package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import lombok.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Builder
@Data
public class PostDto {


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create{

        private long clubId;
        private String content;
        private List<MultipartFile> files;

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
        private String clubMemberName;
        private int likeCount;
        private int commentCount;
        private List<String> attachmentsUrl;
        private LocalDateTime createdTime;
        private LocalDateTime lastModifiedTime;


        // Entity -> DTO
        public static Response of(Post post,int likeCount,int commentCount) {

            return Response.builder()
                    .id(post.getId())
                    .content(post.getContent())
                    .isFixed(post.getIsFixed())
                    .clubId(post.getClub().getId())
                    .clubMemberName(post.getClubMember().getName())
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .createdTime(post.getCreatedDate())
                    .lastModifiedTime(post.getLastModifiedDate())
                    .attachmentsUrl(post.getAttachments().stream().map(Attachment::getUrl).collect(Collectors.toList()))
                    .build();
        }

        public static List<Response> ofList(List<Post> posts, List<Integer> likeCounts, List<Integer> commentCounts) {
            return IntStream.range(0,posts.size())
                    .mapToObj(index->of(posts.get(index),likeCounts.get(index),commentCounts.get(index)))
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

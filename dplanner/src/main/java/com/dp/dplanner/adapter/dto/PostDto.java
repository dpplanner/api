package com.dp.dplanner.adapter.dto;

import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @NoArgsConstructor
    public static class Create{
        @NotNull
        private Long clubId;
        private String content;
        private String title;
        private List<MultipartFile> files;

        public Post toEntity(ClubMember clubMember, Club club) {
            return Post.builder()
                    .clubMember(clubMember)
                    .club(club)
                    .content(content.isBlank() ? " " : content)
                    .title(title.isBlank() ? "제목 없음" : title)
                    .isFixed(false)
                    .build();
        }



    }
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class PostResponseDto {
        private Post post;
        private Boolean likeStatus;
        private Long likeCount;
        private Long commentCount;

    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response{
        private Long id;
        private String content;
        private String title;
        private Boolean isFixed;
        private Long clubId;
        private Long clubMemberId;
        private String clubMemberName;
        private Boolean clubMemberIsDeleted;
        private String profileUrl;
        private String clubRole;
        private Long likeCount;
        private Long commentCount;
        private Boolean likeStatus;
        private List<String> attachmentsUrl;
        private LocalDateTime createdTime;
        private LocalDateTime lastModifiedTime;


        // Entity -> DTO
        public static Response of(Post post,Long likeCount,Long commentCount,Boolean likeStatus) {

            return Response.builder()
                    .id(post.getId())
                    .content(post.getContent())
                    .title(post.getTitle())
                    .isFixed(post.getIsFixed())
                    .clubId(post.getClub().getId())
                    .clubMemberName(post.getClubMember().getName())
                    .clubMemberId(post.getClubMember().getId())
                    .clubMemberIsDeleted(post.getClubMember().getIsDeleted())
                    .profileUrl(post.getClubMember().getUrl())
                    .clubRole(post.getClubMember().getRole().name())
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .likeStatus(likeStatus)
                    .createdTime(post.getCreatedDate())
                    .lastModifiedTime(post.getLastModifiedDate())
                    .attachmentsUrl(post.getAttachments().stream().map(Attachment::getUrl).collect(Collectors.toList()))
                    .build();
        }

        public static List<Response> ofList(List<PostResponseDto> postResponseDtos) {
            return IntStream.range(0, postResponseDtos.size())
                    .mapToObj(index -> of(
                            postResponseDtos.get(index).getPost(),
                            postResponseDtos.get(index).getLikeCount(),
                            postResponseDtos.get(index).getCommentCount(),
                            postResponseDtos.get(index).getLikeStatus()))
                    .collect(Collectors.toList());
        }


    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class SliceResponse {
        private List<Response> content;
        private Pageable pageable;
        private boolean hasNext;
    }


    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Update {
        private Long id;
        private String content;
        private String title;
        @Builder.Default
        private List<String> attachmentUrl = new ArrayList<>();
        private List<MultipartFile> files;
    }

    public static PostDto.SliceResponse getSliceResponse(Pageable pageable, Slice<Object[]> postSlice) {
        List<PostDto.PostResponseDto> postResponseDtos = new ArrayList<>();
        for(Object[] object : postSlice){
            postResponseDtos.add(
                    PostDto.PostResponseDto.builder()
                            .post((Post) object[0])
                            .likeStatus(object[1] != null)
                            .likeCount((Long) object[2])
                            .commentCount((Long) object[3])
                            .build()
            );
        }
        return new PostDto.SliceResponse(PostDto.Response.ofList(postResponseDtos), pageable, postSlice.hasNext());
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Report{
        private Long postId;
        private Long clubMemberId;
        @NotNull
        private String reportMessage;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Block {
        private Long postId;
        private Long clubMemberId;
    }
}

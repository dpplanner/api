package com.dp.dplanner.dto;

import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.FileType;
import com.dp.dplanner.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

public class AttachmentDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Create{
        Long postId;
        List<MultipartFile> files;

        public Attachment toEntity(Post post, String url, FileType type) {

            return Attachment.builder()
                    .post(post)
                    .url(url)
                    .type(type)
                    .build();

        }
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class Response{
        Long id;
        Long postId;
        String url;
        FileType type;

        public static Response of(Attachment attachment) {
            return Response.builder()
                    .id(attachment.getId())
                    .postId(attachment.getPost().getId())
                    .url(attachment.getUrl())
                    .type(attachment.getType())
                    .build();
        }

        public static List<Response> ofList(List<Attachment> attachments) {
            return attachments.stream().map(Response::of).collect(Collectors.toList());
        }

    }
}

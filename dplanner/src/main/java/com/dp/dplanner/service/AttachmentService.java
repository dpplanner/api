package com.dp.dplanner.service;

import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.FileType;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.repository.AttachmentRepository;
import com.dp.dplanner.repository.PostRepository;
import com.dp.dplanner.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dp.dplanner.dto.AttachmentDto.*;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final PostRepository postRepository;
    private final UploadService uploadService;

    public List<Response> createAttachment(Create createDto) {

        Post post = postRepository.findById(createDto.getPostId()).orElseThrow(RuntimeException::new);
        List<Attachment> attachments = new ArrayList<>();
        createDto.getFiles().stream().forEach(file -> {
            try {
                String url = uploadService.uploadFile(file);
                FileType fileType = uploadService.getFileType(url);
                attachments.add(attachmentRepository.save(createDto.toEntity(post, url, fileType)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return Response.ofList(attachments);
    }


    public List<Response> getAttachmentsByPostId(Long postId) {

        postRepository.findById(postId).orElseThrow(RuntimeException::new);

        List<Attachment> attachments = attachmentRepository.findAttachmentsByPostId(postId);

        /**
         * 각 attachment에 있는 url이 유효한가?
         */
        return Response.ofList(attachments);

    }
}
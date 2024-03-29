package com.dp.dplanner.service;

import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.FileType;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.AttachmentRepository;
import com.dp.dplanner.repository.PostRepository;
import com.dp.dplanner.repository.ReservationRepository;
import com.dp.dplanner.service.upload.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.adapter.dto.AttachmentDto.*;
import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final PostRepository postRepository;
    private final ReservationRepository reservationRepository;
    private final UploadService uploadService;

    public List<Response> createAttachment(Create createDto) {

        Post post = postRepository.findById(createDto.getPostId()).orElseThrow(() -> new ServiceException(POST_NOT_FOUND));

        List<Attachment> attachments = new ArrayList<>();
        createDto.getFiles().forEach(file -> {
            try {
                if (file.getSize() > 0) {
                    String url = uploadService.uploadFile(file);
                    FileType fileType = uploadService.getFileType(file.getContentType());
                    attachments.add(attachmentRepository.save(createDto.toEntity(post, url, fileType)));
                }
            } catch (RuntimeException e) {
                throw new ServiceException(FILE_EXCEPTION);
            }
        });

        return Response.ofList(attachments);
    }

    public void createAttachmentReservation(Create createDto) {

        Reservation reservation= reservationRepository.findById(createDto.getReservationId()).orElseThrow(() -> new ServiceException(RESERVATION_NOT_FOUND));

        List<Attachment> attachments = new ArrayList<>();
        createDto.getFiles().stream().forEach(file -> {
            try {
                if (file.getSize() > 0) {
                    String url = uploadService.uploadFile(file);
                    FileType fileType = uploadService.getFileType(file.getContentType());
                    attachments.add(attachmentRepository.save(createDto.toEntity(reservation, url, fileType)));
                }
            } catch (RuntimeException e) {
                throw new ServiceException(FILE_EXCEPTION);
            }
        });


    }


    public List<Response> getAttachmentsByPostId(Long postId) {

        List<Attachment> attachments = attachmentRepository.findAttachmentsByPostId(postId);

        // TODO 각 attachment에 있는 url이 유효한가?
        return Response.ofList(attachments);

    }

    public List<Response> getAttachmentsByReservationId(Long reservationId) {

//        List<Attachment> attachments = attachmentRepository.findAttachmentsByPostId(postId);

        // TODO 각 attachment에 있는 url이 유효한가?
//        return Response.ofList(attachments);

        return null;
    }

    public void deleteAttachmentsByUrl(Post post, List<String> urls) {

        urls.forEach(url -> {
            Optional<Attachment> optional = attachmentRepository.findByUrl(url);
            if (optional.isPresent()) {
                Attachment attachment = optional.get();
                attachmentRepository.delete(attachment);
                post.removeAttachment(attachment);
            }
        });

    }
}

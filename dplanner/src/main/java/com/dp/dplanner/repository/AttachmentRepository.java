package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findAttachmentsByPostId(Long postId);
}

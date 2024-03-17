package com.dp.dplanner.service;

import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.FileType;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.repository.AttachmentRepository;
import com.dp.dplanner.repository.PostRepository;
import com.dp.dplanner.service.upload.UploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.dp.dplanner.adapter.dto.AttachmentDto.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AttachmentServiceTest {

    @Mock
    AttachmentRepository attachmentRepository;
    @Mock
    PostRepository postRepository;
    @Mock
    UploadService uploadService;

    @InjectMocks
    AttachmentService attachmentService;

    Post post;
    Long postId;

    @BeforeEach
    public void setUp() {
        post = Post.builder().build();
        postId = 10L;
        ReflectionTestUtils.setField(post, "id", postId);
    }
    @Test
    public void AttachmentService_CreateAttachment_ReturnListResponseDto() throws Exception {

        String fileName = "testUpload";
        String contentType = "jpg";
        String filePath = "src/test/resources/test/testUpload.jpg";
        String savedPath = "src/main/resources/test/save/" + fileName + "." + contentType;
        FileInputStream fileInputStream = new FileInputStream(filePath);
        MockMultipartFile multipartFile = new MockMultipartFile(fileName, fileName + "." + contentType, contentType, fileInputStream);

        Create createDto = Create.builder()
                .postId(postId)
                .files(Arrays.asList(multipartFile))
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post));
        when(uploadService.uploadFile(multipartFile)).thenReturn(savedPath);
        when(uploadService.getFileType(contentType)).thenReturn(FileType.IMAGE);
        when(attachmentRepository.save(Mockito.any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Response> responseList = attachmentService.createAttachment(createDto);

        Response create = responseList.get(0);

        assertThat(create.getPostId()).isEqualTo(postId);
        assertThat(create.getUrl()).isEqualTo("src/main/resources/test/save/testUpload.jpg");
        assertThat(create.getType()).isEqualTo(FileType.IMAGE);
        assertThat(post.getAttachments().size()).isEqualTo(1);
    }

    @Test
    public void AttachmentService_getAttachmentsByPostId_ReturnListResponseDto() {

        Attachment attachment = new Attachment(post, "testUrl", FileType.IMAGE);
        Attachment attachment2 = new Attachment(post, "testUrl2", FileType.IMAGE);

        when(attachmentRepository.findAttachmentsByPostId(postId)).thenReturn(Arrays.asList(attachment, attachment2));

        List<Response> responseList = attachmentService.getAttachmentsByPostId(postId);

        assertThat(responseList.size()).isEqualTo(2);
        assertThat(responseList).extracting(Response::getPostId).containsOnly(postId);
    }
}

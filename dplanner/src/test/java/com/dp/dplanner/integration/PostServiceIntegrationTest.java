package com.dp.dplanner.integration;

import com.dp.dplanner.TestConfig;
import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.FileType;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.repository.AttachmentRepository;
import com.dp.dplanner.service.PostService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static com.dp.dplanner.adapter.dto.PostDto.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestConfig.class})
@Transactional
public class PostServiceIntegrationTest {

    @Autowired
    PostService postService;
    @Autowired
    AttachmentRepository attachmentRepository;
    @Autowired
    EntityManager entityManager;

    Member member;
    Club club;
    ClubMember clubMember;

    @BeforeEach
    public void setUp() {

        member = Member.builder().build();
        club = Club.builder().build();
        clubMember = ClubMember.builder().club(club).member(member).build();

        entityManager.persist(member);
        entityManager.persist(club);
        entityManager.persist(clubMember);

    }


    @Test
    public void PostService_CreatePost_ReturnPostResponseDto() throws Exception{

        String fileName = "testUpload";
        String contentType = "jpg";
        String filePath = "src/test/resources/test/testUpload.jpg";
        String savedPath = "src/main/resources/test/save/" + fileName + "." + contentType;
        FileInputStream fileInputStream = new FileInputStream(filePath);
        MockMultipartFile multipartFile = new MockMultipartFile(fileName, fileName + "." + contentType, contentType, fileInputStream);

        Create createDto = Create.builder()
                .clubId(club.getId())
                .content("test")
                .files(List.of(multipartFile))
                .build();


        Response response = postService.createPost(clubMember.getId(),createDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isGreaterThan(0);
        assertThat(response.getContent()).isEqualTo("test");
        assertThat(response.getClubId()).isEqualTo(club.getId());
        assertThat(response.getIsFixed()).isEqualTo(false);
        assertThat(response.getClubMemberName()).isEqualTo(clubMember.getName());
        assertThat(response.getLikeCount()).isEqualTo(0);
        assertThat(response.getCommentCount()).isEqualTo(0);
        assertThat(response.getAttachmentsUrl().size()).isEqualTo(1);
        assertThat(response.getAttachmentsUrl().get(0)).isEqualTo(savedPath);

    }

    @Test
    public void PostService_GetPostById_ReturnPostResponseDto() throws Exception{


        String fileName = "testUpload";
        String contentType = "jpg";
        String filePath = "src/test/resources/test/testUpload.jpg";
        String savedPath = "src/main/resources/test/save/" + fileName + "." + contentType;
        FileInputStream fileInputStream = new FileInputStream(filePath);
        MockMultipartFile multipartFile = new MockMultipartFile(fileName, fileName + "." + contentType, contentType, fileInputStream);

        Create createDto = Create.builder()
                .clubId(club.getId())
                .content("test")
                .files(List.of(multipartFile))
                .build();

        Response post = postService.createPost(clubMember.getId(), createDto);

        entityManager.flush();
        entityManager.clear();

        Response foundPost = postService.getPostById(clubMember.getId(),post.getId());

        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getId()).isEqualTo(post.getId());
        assertThat(foundPost.getContent()).isEqualTo("test");
        assertThat(foundPost.getClubId()).isEqualTo(post.getClubId());
        assertThat(foundPost.getIsFixed()).isEqualTo(false);
        assertThat(foundPost.getCommentCount()).isEqualTo(0);
        assertThat(foundPost.getLikeCount()).isEqualTo(0);
        assertThat(foundPost.getAttachmentsUrl().size()).isEqualTo(1);
        assertThat(foundPost.getAttachmentsUrl().get(0)).isEqualTo(savedPath);
        
    }
    

    @Test
    public void PostService_UpdatePost_ReturnResponseDto() throws IOException {
        Post post = Post.builder().clubMember(clubMember).club(club).content("test").build();
        entityManager.persist(post);

        Attachment attachment = new Attachment(post, "testUrl", FileType.IMAGE);
        Attachment attachment2 = new Attachment(post, "testUrl2", FileType.IMAGE);
        Attachment attachment3 = new Attachment(post, "testUrl3", FileType.IMAGE);
        entityManager.persist(attachment);
        entityManager.persist(attachment2);
        entityManager.persist(attachment3);

        Response response = postService.getPostById(clubMember.getId(), post.getId());
        assertThat(response.getAttachmentsUrl().size()).isEqualTo(3);
        assertThat(response.getAttachmentsUrl()).containsOnly("testUrl","testUrl2","testUrl3");

        entityManager.flush();
        entityManager.clear();

        String fileName = "testUpload";
        String contentType = "jpg";
        String filePath = "src/test/resources/test/testUpload.jpg";
        String savedPath = "src/main/resources/test/save/" + fileName + "." + contentType;
        FileInputStream fileInputStream = new FileInputStream(filePath);
        MockMultipartFile multipartFile = new MockMultipartFile(fileName, fileName + "." + contentType, contentType, fileInputStream);

        Update update = Update.builder()
                .id(post.getId())
                .content("update")
                .attachmentUrl(List.of("testUrl"))
                .files(List.of(multipartFile))
                .build();

        Response updateResponse = postService.updatePost(clubMember.getId(), update);
        assertThat(updateResponse.getContent()).isEqualTo("update");
        assertThat(updateResponse.getAttachmentsUrl()).containsOnly("testUrl", savedPath);
    }

    @Test
    public void PostService_DeletePostById() throws Exception {
        Post post = Post.builder().clubMember(clubMember).club(club).content("test").build();
        entityManager.persist(post);

        Attachment attachment = new Attachment(post, "testUrl", FileType.IMAGE);
        Attachment attachment2 = new Attachment(post, "testUrl2", FileType.IMAGE);
        Attachment attachment3 = new Attachment(post, "testUrl3", FileType.IMAGE);

        entityManager.persist(attachment);
        entityManager.persist(attachment2);
        entityManager.persist(attachment3);

        entityManager.flush();
        entityManager.clear();

        postService.deletePostById(clubMember.getId(), post.getId());

        List<Attachment> attachments = attachmentRepository.findAttachmentsByPostId(post.getId());
        assertThat(attachments.size()).isEqualTo(0);
    }
}

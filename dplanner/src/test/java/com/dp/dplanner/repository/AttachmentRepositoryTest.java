package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Attachment;
import com.dp.dplanner.domain.FileType;
import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.Post;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
public class AttachmentRepositoryTest {

    @Autowired
    AttachmentRepository attachmentRepository;
    @Autowired
    TestEntityManager testEntityManager;

    Member member;
    Club club;
    ClubMember clubMember;
    Post post;
    Long postId;
    @BeforeEach
    public void setUp() {
        member = Member.builder().build();
        club = Club.builder().build();
        clubMember = ClubMember.builder().club(club).member(member).name("test").build();
        post = Post.builder().clubMember(clubMember).club(club).build();

        testEntityManager.persist(member);
        testEntityManager.persist(club);
        testEntityManager.persist(clubMember);
        testEntityManager.persist(post);
        postId = post.getId();
    }
    @Test
    public void AttachmentRepository_CreateAttachment_ReturnAttachment() {

        Attachment attachment = createAttachment("test");

        Attachment save = attachmentRepository.save(attachment);

        assertThat(save).isNotNull();
        assertThat(save.getId()).isGreaterThan(0L);
        assertThat(save.getUrl()).isEqualTo("test");
        assertThat(save.getType()).isEqualTo(FileType.IMAGE);

        assertThat(post.getAttachments().size()).isEqualTo(1);
        assertThat(post.getAttachments().get(0)).isEqualTo(save);


    }


    @Test
    public void AttachmentRepository_findByPostId_ReturnAttachments() {

        Attachment attachment = createAttachment("test");
        Attachment attachment2 = createAttachment("test2");

        postId = post.getId();
        attachmentRepository.save(attachment);
        attachmentRepository.save(attachment2);

        List<Attachment> attachments = attachmentRepository.findAttachmentsByPostId(postId);

        assertThat(attachments.size()).isEqualTo(2);
        assertThat(attachments).extracting(Attachment::getPost).extracting(Post::getId).containsOnly(postId);
        assertThat(post.getAttachments().size()).isEqualTo(2);
    }

    @Test
    public void AttachmentRepository_delete(){
        Attachment attachment = createAttachment("test");
        attachmentRepository.save(attachment);

        attachmentRepository.delete(attachment);

        Optional<Attachment> deletedAttachment = attachmentRepository.findById(attachment.getId());
        assertThat(deletedAttachment).isEmpty();

    }

    @Test
    public void AttachmentRepository_deleteByUrl() throws Exception
    {
        Attachment attachment = createAttachment("test");
        Attachment attachment2 = createAttachment("test2");
        attachmentRepository.save(attachment);
        attachmentRepository.save(attachment2);


        attachmentRepository.deleteByUrl("test");
        Optional<Attachment> deletedAttachment = attachmentRepository.findById(attachment.getId());
        List<Attachment> attachments = attachmentRepository.findAttachmentsByPostId(postId);

        assertThat(deletedAttachment).isEmpty();
        assertThat(attachments.size()).isEqualTo(1);
        assertThat(attachments.get(0)).isEqualTo(attachment2);
    }
    private Attachment createAttachment(String url) {
        return Attachment.builder()
                .url(url)
                .post(post)
                .type(FileType.IMAGE)
                .build();
    }
}

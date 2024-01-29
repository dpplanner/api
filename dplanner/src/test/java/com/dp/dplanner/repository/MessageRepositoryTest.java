package com.dp.dplanner.repository;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.PrivateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(MessageRepository.class)
public class MessageRepositoryTest {

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    TestEntityManager testEntityManager;

    Club club;
    Member member;
    ClubMember clubMember;

    Long clubMemberId;


    @BeforeEach
    public void setUp() {
        club = Club.builder().build();
        member = Member.builder().build();
        clubMember = ClubMember.builder().member(member).club(club).build();

        testEntityManager.persist(club);
        testEntityManager.persist(member);
        testEntityManager.persist(clubMember);

        clubMemberId = clubMember.getId();
    }

    @Test
    @DisplayName("개인 메시지 저장 성공")
    public void createPrivateMessage () throws Exception
    {
        PrivateMessage privateMessage = PrivateMessage.builder()
                .clubMember(clubMember)
                .title("title")
                .content("content")
                .redirectUrl("redirectUrl")
                .build();

        messageRepository.save(privateMessage);

        assertThat(privateMessage.getId()).isGreaterThan(0);
        assertThat(privateMessage.getContent()).isEqualTo("content");
        assertThat(privateMessage.getTitle()).isEqualTo("title");
        assertThat(privateMessage.getRedirectUrl()).isEqualTo("redirectUrl");
    }

    @Test
    @DisplayName("clubMemberId를 이용해 6개월 이내에 메시지를 조회한다.")
    public void findAllPrivateMessage() throws Exception
    {
        //given
        PrivateMessage privateMessage = PrivateMessage.builder()
                .clubMember(clubMember)
                .title("title")
                .content("content")
                .redirectUrl("redirectUrl")
                .build();
        PrivateMessage privateMessage2 = PrivateMessage.builder()
                .clubMember(clubMember)
                .title("title")
                .content("content")
                .redirectUrl("redirectUrl")
                .build();

        messageRepository.save(privateMessage);
        messageRepository.save(privateMessage2);


        // When: findAll 메서드 호출
        List<PrivateMessage> privateMessages = messageRepository.findAll(clubMemberId);

        // Then: 결과 검증
        assertThat(privateMessages.size()).isEqualTo(2);

    }

    @Test
    @DisplayName("invalid clubMemberId를 이용해 6개월 이내에 메시지를 조회한다.")
    public void findAllPrivateMessageUsingWrongClubMemberId() throws Exception
    {
        //given
        PrivateMessage privateMessage = PrivateMessage.builder()
                .clubMember(clubMember)
                .title("title")
                .content("content")
                .redirectUrl("redirectUrl")
                .build();
        PrivateMessage privateMessage2 = PrivateMessage.builder()
                .clubMember(clubMember)
                .title("title")
                .content("content")
                .redirectUrl("redirectUrl")
                .build();

        messageRepository.save(privateMessage);
        messageRepository.save(privateMessage2);

        long wrongClubMemberId = 999L;

        // When: findAll 메서드 호출
        List<PrivateMessage> privateMessages = messageRepository.findAll(wrongClubMemberId);

        // Then: 결과 검증
        assertThat(privateMessages.size()).isEqualTo(0);
        assertThat(privateMessages).isEmpty();
    }
}
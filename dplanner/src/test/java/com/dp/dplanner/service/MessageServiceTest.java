package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

    @Mock
    FCMService fcmService;
    @Mock
    MessageRepository messageRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private MessageService messageService;

    Club club;

    @BeforeEach
    void setUp() {
        club = Club.builder().build();

    }

    @Test
    void createPrivateMessage() {
        // Given
        Long clubMemberId1 = 1L;
        Long clubMemberId2 = 2L;

        ClubMember clubMember1 = ClubMember.builder().club(club).member(Member.builder().build()).name("clubMember1").build();
        ClubMember clubMember2 = ClubMember.builder().club(club).member(Member.builder().build()).name("clubMember2").build();
        List<ClubMember> clubMembers = Arrays.asList(clubMember1, clubMember2);

        Message message = Message.builder().title("title").content("content").redirectUrl("redirectUrl").build();
        // When
        messageService.createPrivateMessage(clubMembers, message);

        // Then
//        verify(messageRepository, times(2)).save(any(PrivateMessage.class));
        verify(messageRepository, times(1)).saveAll(any());

    }

}

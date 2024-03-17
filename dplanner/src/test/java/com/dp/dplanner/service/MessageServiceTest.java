package com.dp.dplanner.service;


import com.dp.dplanner.domain.Member;
import com.dp.dplanner.domain.club.Club;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.domain.message.PrivateMessage;
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
        List<Long> clubMemberIds = Arrays.asList(clubMemberId1, clubMemberId2);

        ClubMember clubMember1 = ClubMember.builder().club(club).member(Member.builder().build()).name("clubMember1").build();
        ClubMember clubMember2 = ClubMember.builder().club(club).member(Member.builder().build()).name("clubMember2").build();

        Message message = new Message("title","content","redirectUrl");

        when(clubMemberRepository.findById(clubMemberId1)).thenReturn(Optional.of(clubMember1));
        when(clubMemberRepository.findById(clubMemberId2)).thenReturn(Optional.of(clubMember2));

        // When
        messageService.createPrivateMessage(clubMemberIds, message);

        // Then
        verify(messageRepository, times(2)).save(any(PrivateMessage.class));
    }

    @Test
    void createPrivateMessageWithInvalidClubMember() {
        // Given
        Long invalidClubMemberId = 999L;
        List<Long> clubMemberIds = Arrays.asList(invalidClubMemberId);

        Message message = new Message("title","content","redirectUrl");

        when(clubMemberRepository.findById(invalidClubMemberId)).thenReturn(Optional.empty());

        // When, Then
        assertThrows(ServiceException.class, () -> messageService.createPrivateMessage(clubMemberIds, message));
    }
}

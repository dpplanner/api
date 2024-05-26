package com.dp.dplanner.service;

import com.dp.dplanner.adapter.dto.FCMDto;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.domain.message.PrivateMessage;
import com.dp.dplanner.adapter.dto.MessageDto;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final FCMService fcmService;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public MessageDto.ResponseList findMyMessage(Long clubMemberId) {

        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<PrivateMessage> messages = messageRepository.findAll(clubMemberId,sixMonthsAgo);
        long notReadCount = 0;
        List<MessageDto.Response> responseList = new ArrayList<>();

        for (PrivateMessage message : messages) {
            if (!message.getIsRead()) {
                notReadCount++;
            }
            responseList.add(MessageDto.Response.of(message));
        }
        // 읽지 않은 메시지 수와 읽은 메시지 리스트를 반환
        return MessageDto.ResponseList.builder()
                .responseList(responseList)
                .notRead(notReadCount)
                .build();
    }

    @Async
    @Transactional
    public void createPrivateMessage(List<ClubMember> clubMembers, Message message) {

        List<PrivateMessage> privateMessages = new ArrayList<>();
        List<Long> clubMemberIds = new ArrayList<>();
        clubMembers.forEach(clubMember -> {

            PrivateMessage privateMessage = PrivateMessage.builder()
                    .clubMember(clubMember)
                    .content(message.getContent())
                    .title(message.getTitle())
                    .redirectUrl(message.getRedirectUrl())
                    .type(message.getType())
                    .info(message.getInfo())
                    .isRead(false)
                    .build();

            privateMessages.add(privateMessage);
            clubMemberIds.add(clubMember.getId());
        });

        messageRepository.saveAll(privateMessages);

        fcmService.sendNotification(clubMemberIds,
                FCMDto.Send.builder().
                        title(message.getTitle()).
                        content(message.getContent()).
                        build());


    }

    @Transactional
    public void readMessage(Long clubMemberId, Long messageId)  {

        PrivateMessage privateMessage = messageRepository.findById(messageId).orElseThrow(() -> new ServiceException(MESSAGE_NOT_FOUND));

        if (!privateMessage.getClubMember().getId().equals(clubMemberId)) {
            throw new ServiceException(UPDATE_AUTHORIZATION_DENIED);
        }

        privateMessage.read();

    }
}

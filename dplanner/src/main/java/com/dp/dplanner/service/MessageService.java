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
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final FCMService fcmService;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public MessageDto.ResponseList findMyMessage(Long clubMemberId, Long months) {

        if (ObjectUtils.isEmpty(months)) {
            months = 3L;
        }
        LocalDateTime monthsAgo = LocalDateTime.now().minusMonths(months);
        List<PrivateMessage> messages = messageRepository.findAll(clubMemberId,monthsAgo);
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

        // Initialize collections with expected sizes
        List<PrivateMessage> privateMessages = new ArrayList<>(clubMembers.size());
        List<Long> clubMemberIds = new ArrayList<>(clubMembers.size());
        List<FCMDto.Send> fcmDtos = new ArrayList<>(clubMembers.size());

        // Create private messages and FCM DTOs
        for (ClubMember clubMember : clubMembers) {
            PrivateMessage privateMessage = createPrivateMessage(clubMember, message);
            FCMDto.Send fcmDto = createFcmDto(clubMember, message);

            privateMessages.add(privateMessage);
            fcmDtos.add(fcmDto);
            clubMemberIds.add(clubMember.getId());
        }

        // Save private messages and update FCM DTOs with saved message IDs
        List<PrivateMessage> savedPrivateMessages = messageRepository.saveAll(privateMessages);
        updateFcmDtosWithIds(savedPrivateMessages, fcmDtos);

        // Send notifications
        fcmService.sendNotification(clubMemberIds, fcmDtos);
    }

    // Helper method to create a private message
    private PrivateMessage createPrivateMessage(ClubMember clubMember, Message message) {
        return PrivateMessage.builder()
                .clubMember(clubMember)
                .content(message.getContent())
                .title(message.getTitle())
                .redirectUrl(message.getRedirectUrl())
                .type(message.getType())
                .infoType(message.getInfoType())
                .info(message.getInfo())
                .isRead(false)
                .build();
    }

    // Helper method to create an FCM DTO
    private FCMDto.Send createFcmDto(ClubMember clubMember, Message message) {
        Map<String, String> data = new HashMap<>();
        data.put("clubId", String.valueOf(clubMember.getClub().getId()));

        return FCMDto.Send.builder()
                .title(message.getTitle())
                .content(message.getContent())
                .data(data)
                .build();
    }

    // Helper method to update FCM DTOs with saved message IDs
    private void updateFcmDtosWithIds(List<PrivateMessage> savedPrivateMessages, List<FCMDto.Send> fcmDtos) {
        for (int i = 0; i < savedPrivateMessages.size(); i++) {
            fcmDtos.get(i).getData().put("id", String.valueOf(savedPrivateMessages.get(i).getId()));
        }
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

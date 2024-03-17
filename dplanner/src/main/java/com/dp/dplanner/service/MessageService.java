package com.dp.dplanner.service;

import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.domain.message.PrivateMessage;
import com.dp.dplanner.adapter.dto.MessageDto;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.service.exception.ServiceException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ClubMemberRepository clubMemberRepository;

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

    @Transactional
    public void createPrivateMessage(List<Long> clubMemberIds, Message message) {

        clubMemberIds.forEach(clubMemberId -> {
            ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ServiceException(ErrorResult.CLUBMEMBER_NOT_FOUND));

            PrivateMessage privateMessage = PrivateMessage.builder()
                    .clubMember(clubMember)
                    .content(message.getContent())
                    .title(message.getTitle())
                    .redirectUrl(message.getRedirectUrl())
                    .build();

            messageRepository.save(privateMessage);
        });

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

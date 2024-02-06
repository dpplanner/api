package com.dp.dplanner.service;

import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.domain.message.PrivateMessage;
import com.dp.dplanner.dto.MessageDto;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.exception.MessageException;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.dp.dplanner.exception.ErrorResult.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ClubMemberRepository clubMemberRepository;

    public MessageDto.ResponseList findMyMessage(Long clubMemberId) {

        List<PrivateMessage> messages = messageRepository.findAll(clubMemberId);
        final long[] notReadCount = {0};
        List<MessageDto.Response> responseList = messages.stream()
                .map(message -> {
                    if (!message.getIsRead()) {
                        notReadCount[0] += 1;
                    }
                    return MessageDto.Response.of(message);
                })
                .collect(Collectors.toList());

        // 읽지 않은 메시지 수와 읽은 메시지 리스트를 반환
        return MessageDto.ResponseList.builder()
                .responseList(responseList)
                .notRead(notReadCount[0])
                .build();
    }

    @Transactional
    public void createPrivateMessage(List<Long> clubMemberIds, Message message) {

        clubMemberIds.forEach(clubMemberId -> {

            ClubMember clubMember = clubMemberRepository.findById(clubMemberId).orElseThrow(() -> new ClubMemberException(ErrorResult.CLUBMEMBER_NOT_FOUND));

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

        PrivateMessage privateMessage = messageRepository.findById(messageId).orElseThrow(() -> new MessageException(MESSAGE_NOT_FOUND));

        if (!privateMessage.getClubMember().getId().equals(clubMemberId)) {
            throw new MessageException(UPDATE_AUTHORIZATION_DENIED);
        }

        privateMessage.read();

    }
}

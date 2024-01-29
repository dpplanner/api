package com.dp.dplanner.service;

import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.domain.message.PrivateMessage;
import com.dp.dplanner.exception.ClubMemberException;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.repository.ClubMemberRepository;
import com.dp.dplanner.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ClubMemberRepository clubMemberRepository;

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

}

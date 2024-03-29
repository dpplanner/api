package com.dp.dplanner.service;

import com.dp.dplanner.adapter.dto.FCMDto;
import com.dp.dplanner.repository.MemberRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;
    private final MemberRepository memberRepository;


    public void sendNotification(List<Long> clubMemberIds, FCMDto.Send sendDto) {

        List<String> fcmTokens = memberRepository.getFcmTokensUsingClubMemberIds(clubMemberIds);
        for (String fcmToken : fcmTokens) {
            try {
                if (fcmToken != null) {
                    Notification notification = Notification.builder()
                            .setTitle(sendDto.getTitle())
                            .setBody(sendDto.getContent())
                            .build();

                    Message message = Message.builder()
                            .setNotification(notification)
                            .setToken(fcmToken)
                            .build();
                    firebaseMessaging.send(message);
                }
            }  catch (FirebaseMessagingException e) {
                log.warn("send fcm notification error {}", fcmToken);
            }

        }
    }
}

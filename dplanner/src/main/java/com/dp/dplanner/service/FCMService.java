package com.dp.dplanner.service;

import com.dp.dplanner.domain.message.PrivateMessage;
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

    public void sendNotification(PrivateMessage privateMessage) {

        String fcmToken = privateMessage.getClubMember().getMember().getFcmToken();
        if (fcmToken != null) {
            Notification notification = Notification.builder()
                    .setTitle(privateMessage.getTitle())
                    .setBody(privateMessage.getContent())
                    .build();

            Message message = Message.builder()
                    .setNotification(notification)
                    .setToken(fcmToken)
                    .build();
            try {
                firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                log.error("FirebaseMessagingException {}", fcmToken);
            }
        }
    }

    public void sendNotifications(List<PrivateMessage> requestDtoList) {
        requestDtoList.stream().forEach(this::sendNotification);
    }

}

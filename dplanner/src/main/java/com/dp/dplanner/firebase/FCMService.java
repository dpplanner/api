package com.dp.dplanner.firebase;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.exception.ServiceException;
import com.dp.dplanner.repository.MemberRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;
    private final MemberRepository memberRepository;

    public void sendNotificationByToken(FCMNotificationRequestDto requestDto) {
        Member member = memberRepository.findById(requestDto.getMemberId()).orElseThrow(() -> new ServiceException(ErrorResult.MEMBER_NOT_FOUND));

        if (member.getFcmToken() != null) {
            Notification notification = Notification.builder()
                    .setTitle(requestDto.getTitle())
                    .setBody(requestDto.getBody())
                    .build();

            Message message = Message.builder()
                    .setNotification(notification)
                    .setToken(member.getFcmToken())
                    .build();
            try{
                firebaseMessaging.send(message);
            } catch (FirebaseMessagingException e) {
                throw new RuntimeException("FCM sending failure. memberId : " + member.getId());
            }
        }
    }

    public void sendNotifications(List<FCMNotificationRequestDto> requestDtoList) {
        requestDtoList.stream().forEach(this::sendNotificationByToken);
    }

}

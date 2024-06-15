package com.dp.dplanner.service;

import com.dp.dplanner.adapter.dto.FCMDto;
import com.dp.dplanner.repository.MemberRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;
    private final MemberRepository memberRepository;


    public void sendNotification(List<Long> clubMemberIds, List<FCMDto.Send> sendDtos) {

        Map<Long, String> fcmTokensMap = memberRepository.getFcmTokensUsingClubMemberIds(clubMemberIds).
                stream().collect(Collectors.toMap(
                        obj -> (Long) obj[0], obj -> obj[1] != null ? (String) obj[1] : "", (o1, o2) -> o1)
                );

        for (int i = 0; i < clubMemberIds.size(); i++) {
            Long clubMemberId = clubMemberIds.get(i);
            FCMDto.Send sendDto = sendDtos.get(i);
            String fcmToken = fcmTokensMap.get(clubMemberId);

            if (!ObjectUtils.isEmpty(fcmToken)) {

                try {
                    Notification notification = Notification.builder()
                            .setTitle(sendDto.getTitle())
                            .setBody(sendDto.getContent())
                            .build();

                    Message message = Message.builder()
                            .setNotification(notification)
                            .setToken(fcmToken)
                            .putAllData(sendDto.getData())
//                            .setFcmOptions(FcmOptions.builder().setAnalyticsLabel("dataMessage").build())
                            .build();

                    firebaseMessaging.send(message);
                } catch (FirebaseMessagingException e) {
                    log.warn("send fcm notification error {}", fcmToken);
                }
            }
        }
    }
}

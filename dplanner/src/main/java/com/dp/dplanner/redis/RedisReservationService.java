package com.dp.dplanner.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisReservationService {
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${expire.defaultTime}")
    private int defaultTime;
    //예약은 정시 기준으로만 가능. 13:00 ~ 17:00 예약이면 -> 13,14,15,16 시간 예약
    public Boolean saveReservation(LocalDateTime startDateTime, LocalDateTime endDateTime, Long resourceId) {

        int startHour = startDateTime.getHour();
        int endHour = endDateTime.getHour() ;

        if(endHour == 0){
            endHour = 24;
        }

        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < (endHour - startHour); i++) {
            String key = generateKey(startDateTime.plusHours(i),resourceId);
            String value = "r"; // reserved
            map.put(key, value);
        }

        Boolean ret = redisTemplate.opsForValue()
                .multiSetIfAbsent(map);

        if(ret){
            expireReservation(startDateTime, endDateTime, resourceId);
        }

        return ret;

    }

    public void expireReservation(LocalDateTime startDateTime, LocalDateTime endDateTime, Long resourceId) {
        int startHour = startDateTime.getHour();
        int endHour = endDateTime.getHour();

        if(endHour == 0){
            endHour = 24;
        }

        for (int i = 0; i < (endHour - startHour); i++) {
            String key = generateKey(startDateTime.plusHours(i),resourceId);
            redisTemplate.expire(key, Duration.ofSeconds(defaultTime));
        }
    }

    public void deleteReservation(LocalDateTime startDateTime, LocalDateTime endDateTime, Long resourceId) {
        int startHour = startDateTime.getHour();
        int endHour = endDateTime.getHour();

        if(endHour == 0){
            endHour = 24;
        }
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < (endHour - startHour); i++) {
            String key = generateKey(startDateTime.plusHours(i),resourceId);
            keys.add(key);
        }
        redisTemplate.delete(keys);
    }


    private String generateKey(LocalDateTime reservedTime, Long resourceId) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedTime = reservedTime.format(formatter);
        return resourceId.toString() + ":" + formattedTime;
    }

}

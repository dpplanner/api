package com.dp.dplanner.rabbitMQ;

import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("production")
@Slf4j
@RequiredArgsConstructor
public class RabbitMQConsumer {
    private final ReservationService reservationService;

    @RabbitListener(queues = {"${rabbitmq.queue.name.1}"})
    public void consumeQueue1(RabbitMQDto messageDto) {
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(messageDto.getResourceId())
                .startDateTime(messageDto.getStartDateTime())
                .endDateTime(messageDto.getEndDateTime())
                .title(messageDto.getTitle())
                .usage(messageDto.getUsage())
                .sharing(messageDto.isSharing())
                .build();

        try {
            reservationService.createReservation(messageDto.getClubMemberId(), createDto);
        } catch (Exception e) {
            log.error("error : {} , param : {}", e.getMessage(), messageDto);
        }
    }

    @RabbitListener(queues = {"${rabbitmq.queue.name.2}"})
    public void consumeQueue2(RabbitMQDto messageDto) {
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(messageDto.getResourceId())
                .startDateTime(messageDto.getStartDateTime())
                .endDateTime(messageDto.getEndDateTime())
                .title(messageDto.getTitle())
                .usage(messageDto.getUsage())
                .sharing(messageDto.isSharing())
                .build();

        try {
            reservationService.createReservation(messageDto.getClubMemberId(), createDto);
        } catch (Exception e) {
            log.error("error : {} , param : {}", e.getMessage(), messageDto);
        }
    }

    @RabbitListener(queues = {"${rabbitmq.queue.name.3}"})
    public void consumeQueue3(RabbitMQDto messageDto) {
        ReservationDto.Create createDto = ReservationDto.Create.builder()
                .resourceId(messageDto.getResourceId())
                .startDateTime(messageDto.getStartDateTime())
                .endDateTime(messageDto.getEndDateTime())
                .title(messageDto.getTitle())
                .usage(messageDto.getUsage())
                .sharing(messageDto.isSharing())
                .build();

        try {
            reservationService.createReservation(messageDto.getClubMemberId(), createDto);
        } catch (Exception e) {
            log.error("error : {} , param : {}", e.getMessage(), messageDto);
        }


    }
}


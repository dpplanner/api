//package com.dp.dplanner.rabbitMQ;
//
//import com.dp.dplanner.security.PrincipalDetails;
//import lombok.RequiredArgsConstructor;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Profile;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//
////@Controller
//@Profile("!test")
//@RequiredArgsConstructor
//public class ReservationRabbitMQController {
//
//    private final RabbitTemplate rabbitTemplate;
//
//
//
//    @Value("${rabbitmq.exchange.name}")
//    private String exchangeName;
//
//    @Value("${rabbitmq.routing.key.1}")
//    private String routingKey1;
//
//    @Value("${rabbitmq.routing.key.2}")
//    private String routingKey2;
//
//    @Value("${rabbitmq.routing.key.3}")
//    private String routingKey3;
//
//
////    @PostMapping(value = "/reservations")
//    public ResponseEntity createReservation(@AuthenticationPrincipal PrincipalDetails principal,
//                                            @RequestBody RabbitMQDto messageDto) {
//
//        messageDto.setClubMemberId(principal.getClubMemberId());
//
//        if (principal.getClubId() % 3 == 0){
//            rabbitTemplate.convertAndSend(exchangeName, routingKey1, messageDto);
//        }else if(principal.getClubId() % 3 == 1){
//            rabbitTemplate.convertAndSend(exchangeName, routingKey2, messageDto);
//        }else{
//            rabbitTemplate.convertAndSend(exchangeName, routingKey3, messageDto);
//        }
//
//        return ResponseEntity.noContent().build();
//    }
//
//
//
//}

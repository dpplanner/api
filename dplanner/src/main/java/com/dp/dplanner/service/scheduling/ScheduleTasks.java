package com.dp.dplanner.service.scheduling;

import com.dp.dplanner.domain.Reservation;
import com.dp.dplanner.domain.club.ClubMember;
import com.dp.dplanner.domain.message.Message;
import com.dp.dplanner.repository.ReservationRepository;
import com.dp.dplanner.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleTasks {

    private final MessageService messageService;
    private final ReservationRepository reservationRepository;

    // 예약 시작 1시간 전 알림
    @Scheduled(cron = "0 0 * * * *")
    public void task0() {
        try{
            Set<Long> clubMemberIds = new HashSet<>();
            List<ClubMember> clubMembers = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            List<Reservation> reservations = reservationRepository.findAllAboutToStart(now, now.plusMinutes(60));

            reservations.forEach(
                    reservation -> {
                        if (!clubMemberIds.contains(reservation.getClubMember().getId())) {
                            clubMemberIds.add(reservation.getClubMember().getId());
                            clubMembers.add(reservation.getClubMember());
                        }
                        reservation.getReservationInvitees().forEach(
                                invitee -> {
                                    if (!clubMemberIds.contains(invitee.getClubMember().getId())) {
                                        clubMemberIds.add(invitee.getClubMember().getId());
                                        clubMembers.add(invitee.getClubMember());
                                    }
                                }
                        );
                    }
            );

            messageService.createPrivateMessage(clubMembers, Message.aboutToStartMessage());
            log.info("ScheduleTasks0");
        }catch (RuntimeException ex){
            log.error("ScheduleTasks0");
        }
    }

    // 예약 시작 10분 전 알림
    @Scheduled(cron = "0 50 * * * *")
    public void task1() {
        try {
            Set<Long> clubMemberIds = new HashSet<>();
            List<ClubMember> clubMembers = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            List<Reservation> reservations = reservationRepository.findAllAboutToStart(now, now.plusMinutes(10));

            reservations.forEach(
                    reservation -> {
                        if (!clubMemberIds.contains(reservation.getClubMember().getId())) {
                            clubMemberIds.add(reservation.getClubMember().getId());
                            clubMembers.add(reservation.getClubMember());
                        }
                        reservation.getReservationInvitees().forEach(
                                invitee -> {
                                    if (!clubMemberIds.contains(invitee.getClubMember().getId())) {
                                        clubMemberIds.add(invitee.getClubMember().getId());
                                        clubMembers.add(invitee.getClubMember());
                                    }
                                }
                        );
                    }
            );

            messageService.createPrivateMessage(clubMembers, Message.aboutToStartMessage());
            log.info("ScheduleTasks1");
        } catch (RuntimeException exception) {
            log.error("ScheduleTasks1");
        }
    }

    // 예약 종료 10분 전 알림
    @Scheduled(fixedRate = 600000)
    public void task2() {
        try{
            Set<Long> clubMemberIds = new HashSet<>();
            List<ClubMember> clubMembers = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            List<Reservation> reservations = reservationRepository.findAllAboutToFinish(now, now.plusMinutes(10));

            reservations.forEach(
                    reservation -> {
                        if (!clubMemberIds.contains(reservation.getClubMember().getId())) {
                            clubMemberIds.add(reservation.getClubMember().getId());
                            clubMembers.add(reservation.getClubMember());
                        }
                        reservation.getReservationInvitees().forEach(
                                invitee -> {
                                    if (!clubMemberIds.contains(invitee.getClubMember().getId())) {
                                        clubMemberIds.add(invitee.getClubMember().getId());
                                        clubMembers.add(invitee.getClubMember());
                                    }
                                }
                        );
                    }
            );

            messageService.createPrivateMessage(clubMembers, Message.aboutToFinishMessage());
            log.info("ScheduleTasks2");
        }catch (RuntimeException ex){
            log.error("ScheduleTasks2");
        }
    }

    // 반납 메시지 요청 매 시간마다
    @Scheduled(cron = "0 0 * * * *")
    public void task3() {
        try{
            Set<Long> clubMemberIds = new HashSet<>();
            List<ClubMember> clubMembers = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            List<Reservation> reservations = reservationRepository.findAllNotReturned(now);

            reservations.forEach(
                    reservation -> {
                        if (!clubMemberIds.contains(reservation.getClubMember().getId())) {
                            clubMemberIds.add(reservation.getClubMember().getId());
                            clubMembers.add(reservation.getClubMember());
                        }
                    }
            );

            messageService.createPrivateMessage(clubMembers, Message.requestReturnMessage());
            log.info("ScheduleTasks3");
        }catch (RuntimeException ex){
            log.error("ScheduleTasks3");
        }
    }

}

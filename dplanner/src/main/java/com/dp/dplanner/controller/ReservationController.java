package com.dp.dplanner.controller;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.dp.dplanner.dto.ReservationDto.*;

@RestController
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @PostMapping(value = "/reservations", params = "clubId")
    public ResponseEntity<Response> createReservation(@GeneratedClubMemberId Long clubMemberId,
                                                      @RequestParam Long clubId,
                                                      @RequestBody Create createDto) {

        Response response = reservationService.createReservation(clubMemberId, createDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping(value = "/reservations/{reservationId}/update", params = "clubId", name = "update")
    public ResponseEntity<Response> updateReservations(@GeneratedClubMemberId Long clubMemberId,
                                                       @RequestParam Long clubId,
                                                       @PathVariable Long reservationId,
                                                       @RequestBody Update updateDto) {

        Response response = reservationService.updateReservation(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);

    }

    @PutMapping(value = "/reservations/{reservationId}/cancel", params = "clubId", name = "delete")
    public ResponseEntity cancelReservations(@GeneratedClubMemberId Long clubMemberId,
                                             @RequestParam Long clubId,
                                             @PathVariable Long reservationId,
                                             @RequestBody Delete deleteDto) {

        reservationService.cancelReservation(clubMemberId, deleteDto);

        return ResponseEntity.noContent().build();
    }


    @DeleteMapping(value = "/reservations/delete", params = "clubId")
    public ResponseEntity deleteReservation(@GeneratedClubMemberId Long clubMemberId,
                                            @RequestParam Long clubId,
                                            @RequestBody Delete deleteDto) {

        reservationService.deleteReservation(clubMemberId, deleteDto);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/reservations", params = {"clubId","confirm"})
    public ResponseEntity confirmReservations(@GeneratedClubMemberId Long clubMemberId,
                                              @RequestParam Long clubId,
                                              @RequestParam Boolean confirm,
                                              @RequestBody List<Request> requestDto) {
        if (confirm) {
            reservationService.confirmAllReservations(clubMemberId, requestDto);
        }else{
            reservationService.rejectAllReservations(clubMemberId, requestDto);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/reservations/{reservationId}", params = "clubId")
    public ResponseEntity<Response> getReservation(@GeneratedClubMemberId Long clubMemberId,
                                                   @RequestParam Long clubId,
                                                   @PathVariable Long reservationId) {

        Request requestDto = new Request(reservationId);
        Response response = reservationService.findReservationById(clubMemberId, requestDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/reservations", params = {"clubId","resourceId","start","end"})
    public ResponseEntity<List<Response>> getAllReservationsByPeriod(@GeneratedClubMemberId Long clubMemberId,
                                                                    @RequestParam Long clubId,
                                                                    @RequestParam Long resourceId,
                                                                    @RequestParam String start,
                                                                    @RequestParam String end) {

        Request requestDto = new Request(
                resourceId,
                LocalDateTime.parse(start, formatter),
                LocalDateTime.parse(end, formatter)
        );

        List<Response> response = reservationService.findAllReservationsByPeriod(clubMemberId, requestDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/reservations", params = {"clubId","resourceId","status"})
    public ResponseEntity<List<Response>> getAllReservationsNotConfirmed(@GeneratedClubMemberId Long clubMemberId,
                                                                         @RequestParam Long clubId,
                                                                         @RequestParam String status,
                                                                         @RequestParam Long resourceId) {

        Request requestDto = Request.builder().resourceId(resourceId).build();
        List<Response> response = reservationService.findAllNotConfirmedReservations(clubMemberId, requestDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }
}

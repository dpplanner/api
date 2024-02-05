package com.dp.dplanner.controller;


import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
//    @PostMapping(value = "/reservations")
//    public ResponseEntity<Response> createReservation(@AuthenticationPrincipal PrincipalDetails principal,
//                                                      @RequestBody Create createDto) {
//        Long clubMemberId = principal.getClubMemberId();
//
//        Response response = reservationService.createReservation(clubMemberId, createDto);
//
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(response);
//    }

    @PatchMapping(value = "/reservations/{reservationId}/update", name = "update")
    public CommonResponse<Response> updateReservations(@AuthenticationPrincipal PrincipalDetails principal,
                                                       @PathVariable Long reservationId,
                                                       @RequestBody Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = reservationService.updateReservation(clubMemberId, updateDto);

        return CommonResponse.createSuccess(response);

    }

    @PutMapping(value = "/reservations/{reservationId}/cancel", name = "delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse cancelReservations(@AuthenticationPrincipal PrincipalDetails principal,
                                             @PathVariable Long reservationId,
                                             @RequestBody Delete deleteDto) {

        Long clubMemberId = principal.getClubMemberId();

        reservationService.cancelReservation(clubMemberId, deleteDto);

        return CommonResponse.createSuccessWithNoContent();
    }


    @DeleteMapping(value = "/reservations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse deleteReservation(@AuthenticationPrincipal PrincipalDetails principal,
                                            @RequestBody Delete deleteDto) {

        Long clubMemberId = principal.getClubMemberId();

        reservationService.deleteReservation(clubMemberId, deleteDto);

        return CommonResponse.createSuccessWithNoContent();
    }

    @PutMapping(value = "/reservations", params = "confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse confirmReservations(@AuthenticationPrincipal PrincipalDetails principal,
                                              @RequestParam Boolean confirm,
                                              @RequestBody List<Request> requestDto) {

        Long clubMemberId = principal.getClubMemberId();

        if (confirm) {
            reservationService.confirmAllReservations(clubMemberId, requestDto);
        } else {
            reservationService.rejectAllReservations(clubMemberId, requestDto);
        }

        return CommonResponse.createSuccessWithNoContent();
    }

    @GetMapping(value = "/reservations/{reservationId}")
    public CommonResponse<Response> getReservation(@AuthenticationPrincipal PrincipalDetails principal,
                                                   @PathVariable Long reservationId) {

        Long clubMemberId = principal.getClubMemberId();

        Request requestDto = new Request(reservationId);
        Response response = reservationService.findReservationById(clubMemberId, requestDto);

        return CommonResponse.createSuccess(response);
    }


    // todo status 따라 조회 부분 수정 및 통합 필요
    @GetMapping(value = "/reservations", params = {"resourceId", "start", "end"})
    public CommonResponse<List<Response>> getAllReservationsByPeriod(@AuthenticationPrincipal PrincipalDetails principal,
                                                                     @RequestParam Long resourceId,
                                                                     @RequestParam String start,
                                                                     @RequestParam String end) {

        Long clubMemberId = principal.getClubMemberId();

        Request requestDto = new Request(
                resourceId,
                LocalDateTime.parse(start, formatter),
                LocalDateTime.parse(end, formatter)
        );

        List<Response> response = reservationService.findAllReservationsByPeriod(clubMemberId, requestDto);

        return CommonResponse.createSuccess(response);

    }


    @GetMapping(value = "/reservations", params = {"resourceId","status"})
    public CommonResponse<List<Response>> getAllReservationsNotConfirmed(@AuthenticationPrincipal PrincipalDetails principal,
                                                                         @RequestParam String status,
                                                                         @RequestParam Long resourceId) {
        Long clubMemberId = principal.getClubMemberId();

        Request requestDto = Request.builder().resourceId(resourceId).build();
        List<Response> response = null;
        if(status.equals("NOT CONFIRMED")){
            response = reservationService.findAllNotConfirmedReservations(clubMemberId, requestDto);

        }

        return CommonResponse.createSuccess(response);
    }
}

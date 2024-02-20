package com.dp.dplanner.controller;


import com.dp.dplanner.domain.ReservationStatus;
import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.dto.ReservationDto;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.dp.dplanner.dto.ReservationDto.*;

@RestController
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PutMapping(value = "/reservations/{reservationId}/update", name = "update")
    public CommonResponse<Response> updateReservations(@AuthenticationPrincipal PrincipalDetails principal,
                                                       @PathVariable Long reservationId,
                                                       @RequestBody Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();
        updateDto.setReservationId(reservationId);
        Response response = reservationService.updateReservation(clubMemberId, updateDto);

        return CommonResponse.createSuccess(response);

    }

    @PatchMapping(value = "/reservations/{reservationId}/cancel", name = "cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse cancelReservations(@AuthenticationPrincipal PrincipalDetails principal,
                                             @PathVariable Long reservationId) {

        Long clubMemberId = principal.getClubMemberId();
        Delete deleteDto = Delete.builder().reservationId(reservationId).build();
        reservationService.cancelReservation(clubMemberId, deleteDto);

        return CommonResponse.createSuccessWithNoContent();
    }

    @PostMapping(value = "/reservations/{reservationId}/return", name = "return")
    public CommonResponse returnReservation(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable Long reservationId,
                                            @RequestPart ReservationDto.Return returnDto,
                                            @RequestPart(required = false) final List<MultipartFile> files) {

        Long clubMemberId = principal.getClubMemberId();
        returnDto.setReservationId(reservationId);
        returnDto.setFiles(files);

        Response response = reservationService.returnReservation(clubMemberId, returnDto);

        return CommonResponse.createSuccess(response);
    }

    @DeleteMapping(value = "/reservations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse deleteReservation(@AuthenticationPrincipal PrincipalDetails principal,
                                            @RequestBody Delete deleteDto) {

        Long clubMemberId = principal.getClubMemberId();

        reservationService.deleteReservation(clubMemberId, deleteDto);

        return CommonResponse.createSuccessWithNoContent();
    }

    @PatchMapping(value = "/reservations", params = "confirm")
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


    @GetMapping(value = "/reservations", params = {"resourceId", "start", "end","status"})
    public CommonResponse<List<Response>> getReservationsByPeriod(@AuthenticationPrincipal PrincipalDetails principal,
                                                                     @RequestParam Long resourceId,
                                                                     @RequestParam String start,
                                                                     @RequestParam String end,
                                                                     @RequestParam String status) {

        Long clubMemberId = principal.getClubMemberId();

        Request requestDto = new Request(
                resourceId,
                LocalDateTime.parse(start, formatter),
                LocalDateTime.parse(end, formatter)
        );
        List<Response> response = null;
        if (status.equals("ALL")){
            response = reservationService.findAllReservationsByPeriod(clubMemberId, requestDto);
        } else if (status.equals(ReservationStatus.REQUEST.name()) || status.equals(ReservationStatus.CONFIRMED.name()) || status.equals(ReservationStatus.REJECTED.name())) {
            response = reservationService.findAllReservationsByPeriodAndStatus(clubMemberId, requestDto, ReservationStatus.valueOf(status));
        } else if(status.equals("SCHEDULER")){
            // 스케줄러에서 확인 가능한 예약 조회 (REJECTED 상태가 아닌 예약만 조회)
            response = reservationService.findAllReservationsByPeriodForScheduler(clubMemberId, requestDto);
        }


        return CommonResponse.createSuccess(response);

    }

// REQUEST STATUS로 조회
//    @GetMapping(value = "/reservations", params = {"resourceId","status"})
//    public CommonResponse<List<Response>> getAllReservationsNotConfirmed(@AuthenticationPrincipal PrincipalDetails principal,
//                                                                         @RequestParam String status,
//                                                                         @RequestParam Long resourceId) {
//        Long clubMemberId = principal.getClubMemberId();
//
//        Request requestDto = Request.builder().resourceId(resourceId).build();
//        List<Response> response = null;
//        if(status.equals("NOT CONFIRMED")){
//            response = reservationService.findAllNotConfirmedReservations(clubMemberId, requestDto);
//
//        }
//
//        return CommonResponse.createSuccess(response);
//    }
}

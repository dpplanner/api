package com.dp.dplanner.adapter.controller;


import com.dp.dplanner.adapter.exception.ApiException;
import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.adapter.dto.ReservationDto;
import com.dp.dplanner.config.security.PrincipalDetails;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.dp.dplanner.adapter.dto.ReservationDto.*;

@RestController
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping(value = "/reservations", name="create")
    public CommonResponse<Response> createReservation(@AuthenticationPrincipal PrincipalDetails principal,
                                                      @RequestBody ReservationDto.Create createDto) {
        Long clubMemberId = principal.getClubMemberId();
        Response response = reservationService.createReservation(clubMemberId, createDto);

        return CommonResponse.createSuccess(response);
    }

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

    @GetMapping(value = "/reservations/scheduler", params = {"resourceId", "start", "end"})
    public CommonResponse<List<Response>> getReservations(@AuthenticationPrincipal PrincipalDetails principal,
                                                     @RequestParam Long resourceId,
                                                     @RequestParam String start,
                                                     @RequestParam String end) {

        Long clubMemberId = principal.getClubMemberId();
        Request requestDto = new Request(
                resourceId,
                LocalDateTime.parse(start, formatter),
                LocalDateTime.parse(end, formatter)
        );
        List<Response> response = reservationService.findAllReservationsForScheduler(clubMemberId, requestDto);

        return CommonResponse.createSuccess(response);
    }

    @GetMapping(value = "/reservations/admin", params = {"clubId", "status"})
    public CommonResponse<SliceResponse> getReservationsAdmin(@AuthenticationPrincipal PrincipalDetails principal,
                                                  @RequestParam Long clubId,
                                                  @RequestParam String status,
                                                  @PageableDefault Pageable pageable) {

        Long clubMemberId = principal.getClubMemberId();
        if (!clubId.equals(principal.getClubId())) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Request requestDto = Request.builder().clubId(clubId).build();

        SliceResponse response = reservationService.findAllReservationsByStatus(clubMemberId, requestDto, status, pageable);

        return CommonResponse.createSuccess(response);
    }

    @GetMapping(value = "reservations/my-reservations",params = "status")
    public CommonResponse<ReservationDto.SliceResponse> getMyReservations(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                            @RequestParam(name = "status") String status,
                                                            @PageableDefault Pageable pageable) {

        Long clubMemberId = principalDetails.getClubMemberId();

        ReservationDto.SliceResponse response= null;
        if(status.equals("previous") ){
            response = reservationService.findMyReservationsPrevious(clubMemberId, pageable);
        }else if(status.equals("upcoming")){
            response = reservationService.findMyReservationsUpComing(clubMemberId, pageable);
        }
        else if(status.equals("reject")){
            response = reservationService.findMyReservationsReject(clubMemberId, pageable);
        }
        else{
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        return CommonResponse.createSuccess(response);

    }
}

package com.dp.dplanner.adapter.controller;

import com.dp.dplanner.domain.Period;
import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.config.security.PrincipalDetails;
import com.dp.dplanner.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.dp.dplanner.adapter.dto.LockDto.*;

@RestController
@RequiredArgsConstructor
public class LockController {

    private final LockService lockService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping(value = "/locks/resources/{resourceId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<Response> createLock(@AuthenticationPrincipal PrincipalDetails principal,
                                               @PathVariable Long resourceId,
                                               @RequestBody Create createDto) {
        Long clubMemberId = principal.getClubMemberId();
        createDto.setResourceId(resourceId);
        Response response = lockService.createLock(clubMemberId, createDto);

        return CommonResponse.createSuccess(response);

    }

    @GetMapping(value = "/locks/resources/{resourceId}", params = {"startDateTime","endDateTime"})
    public CommonResponse<List<Response>> getLocks(@AuthenticationPrincipal PrincipalDetails principal,
                                                   @RequestParam String startDateTime,
                                                   @RequestParam String endDateTime,
                                                   @PathVariable Long resourceId) {

        Long clubMemberId = principal.getClubMemberId();

        Request request = Request.builder()
                .startDateTime(LocalDateTime.parse(startDateTime, formatter))
                .endDateTime(LocalDateTime.parse(endDateTime, formatter))
                .build();

        List<Response> response = lockService.getLocks(clubMemberId, resourceId, new Period(request.getStartDateTime(),request.getEndDateTime()));

        return CommonResponse.createSuccess(response);
    }

    @GetMapping(value = "/locks/{lockId}")
    public CommonResponse<Response> getLock(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable Long lockId) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = lockService.getLock(clubMemberId, lockId);

        return CommonResponse.createSuccess(response);

    }

    @PutMapping(value = "/locks/{lockId}")
    public CommonResponse<Response> updateLock(@AuthenticationPrincipal PrincipalDetails principal,
                                               @PathVariable Long lockId,
                                               @RequestBody Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();
        updateDto.setId(lockId);
        Response response = lockService.updateLock(clubMemberId, updateDto);

        return CommonResponse.createSuccess(response);

    }

    @DeleteMapping(value = "/locks/{lockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse deleteLock(@AuthenticationPrincipal PrincipalDetails principal,
                                     @PathVariable Long lockId) {
        Long clubMemberId = principal.getClubMemberId();

        lockService.deleteLock(clubMemberId, lockId);

        return CommonResponse.createSuccessWithNoContent();
    }
}

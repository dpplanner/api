package com.dp.dplanner.controller;

import com.dp.dplanner.domain.Period;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.dp.dplanner.dto.LockDto.*;

@RestController
@RequiredArgsConstructor
public class LockController {

    private final LockService lockService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping(value = "/locks/resources/{resourceId}")
    public ResponseEntity<Response> createLock(@AuthenticationPrincipal PrincipalDetails principal,
                                               @PathVariable Long resourceId,
                                               @RequestBody Create createDto) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = lockService.createLock(clubMemberId, createDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);

    }

    @GetMapping(value = "/locks/resources/{resourceId}", params = {"start","end"})
    public ResponseEntity<List<Response>> getLocks(@AuthenticationPrincipal PrincipalDetails principal,
                                                   @RequestParam String start,
                                                   @RequestParam String end,
                                                   @PathVariable Long resourceId) {

        Long clubMemberId = principal.getClubMemberId();

        Request request = Request.builder()
                .startDateTime(LocalDateTime.parse(start, formatter))
                .endDateTime(LocalDateTime.parse(end, formatter))
                .build();

        List<Response> response = lockService.getLocks(clubMemberId, resourceId, new Period(request.getStartDateTime(),request.getEndDateTime()));

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/locks/{lockId}")
    public ResponseEntity<Response> getLock(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable Long lockId) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = lockService.getLock(clubMemberId, lockId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);

    }

    @PutMapping(value = "/locks/{lockId}")
    public ResponseEntity<Response> updateLock(@AuthenticationPrincipal PrincipalDetails principal,
                                               @PathVariable Long lockId,
                                               @RequestBody Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = lockService.updateLock(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);

    }

    @DeleteMapping(value = "/locks/{lockId}")
    public ResponseEntity deleteLock(@AuthenticationPrincipal PrincipalDetails principal,
                                     @PathVariable Long lockId) {
        Long clubMemberId = principal.getClubMemberId();

        lockService.deleteLock(clubMemberId, lockId);

        return ResponseEntity.noContent().build();
    }
}

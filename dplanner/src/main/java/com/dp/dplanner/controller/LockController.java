package com.dp.dplanner.controller;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.domain.Period;
import com.dp.dplanner.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping(value = "/locks/resources/{resourceId}", params = "clubId")
    public ResponseEntity<Response> createLock(@GeneratedClubMemberId Long clubMemberId,
                                               @RequestParam Long clubId,
                                               @PathVariable Long resourceId,
                                               @RequestBody Create createDto) {
        Response response = lockService.createLock(clubMemberId, createDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);

    }

    @GetMapping(value = "/locks/resources/{resourceId}", params = {"clubId","start","end"})
    public ResponseEntity<List<Response>> getLocks(@GeneratedClubMemberId Long clubMemberId,
                                                   @RequestParam Long clubId,
                                                   @RequestParam String start,
                                                   @RequestParam String end,
                                                   @PathVariable Long resourceId) {

        Request request = Request.builder()
                .startDateTime(LocalDateTime.parse(start, formatter))
                .endDateTime(LocalDateTime.parse(end, formatter))
                .build();

        List<Response> response = lockService.getLocks(clubMemberId, resourceId, new Period(request.getStartDateTime(),request.getEndDateTime()));

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/locks/{lockId}", params = "clubId")
    public ResponseEntity<Response> getLock(@GeneratedClubMemberId Long clubMemberId,
                                               @RequestParam Long clubId,
                                               @PathVariable Long lockId) {

        Response response = lockService.getLock(clubMemberId,lockId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);

    }

    @PutMapping(value = "/locks/{lockId}", params = "clubId")
    public ResponseEntity<Response> updateLock(@GeneratedClubMemberId Long clubMemberId,
                                               @RequestParam Long clubId,
                                               @PathVariable Long lockId,
                                               @RequestBody Update updateDto) {

        Response response = lockService.updateLock(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);

    }

    @DeleteMapping(value = "/locks/{lockId}", params = "clubId")
    public ResponseEntity deleteLock(@GeneratedClubMemberId Long clubMemberId,
                                     @RequestParam Long clubId,
                                     @PathVariable Long lockId) {

        lockService.deleteLock(clubMemberId, lockId);

        return ResponseEntity.noContent().build();
    }
}

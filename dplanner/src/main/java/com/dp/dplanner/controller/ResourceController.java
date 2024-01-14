package com.dp.dplanner.controller;

import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.dp.dplanner.dto.ResourceDto.*;

@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(value = "/resources")
    public ResponseEntity<Response> createResource(@AuthenticationPrincipal PrincipalDetails principal,
                                                   @RequestBody Create createDto) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = resourceService.createResource(clubMemberId, createDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);

    }

    @GetMapping(value = "/resources")
    public ResponseEntity<List<Response>> getResources(@AuthenticationPrincipal PrincipalDetails principal) {

        Long clubMemberId = principal.getClubMemberId();
        Long clubId = principal.getClubId();

        List<Response> response = resourceService.getResourceByClubId(clubMemberId, clubId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/resources/{resourceId}")
    public ResponseEntity<Response> getResource(@AuthenticationPrincipal PrincipalDetails principal,
                                                @PathVariable Long resourceId) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = resourceService.getResourceById(clubMemberId, resourceId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping(value = "/resources/{resourceId}")
    public ResponseEntity<Response> updateResource(@AuthenticationPrincipal PrincipalDetails principal,
                                                   @PathVariable Long resourceId,
                                                   @RequestBody Update updateDto) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = resourceService.updateResource(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping(value = "/resources/{resourceId}")
    public ResponseEntity deleteResource(@AuthenticationPrincipal PrincipalDetails principal,
                                         @PathVariable Long resourceId) {

        Long clubMemberId = principal.getClubMemberId();

        resourceService.deleteResource(clubMemberId, resourceId);

        return ResponseEntity.noContent().build();
    }





}

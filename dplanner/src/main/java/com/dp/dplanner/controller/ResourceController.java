package com.dp.dplanner.controller;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.dp.dplanner.dto.ResourceDto.*;

@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(value = "/resources", params = "clubId")
    public ResponseEntity<Response> createResource(@GeneratedClubMemberId Long clubMemberId,
                                                   @RequestParam Long clubId,
                                                   @RequestBody Create createDto) {
        Response response = resourceService.createResource(clubMemberId, createDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);

    }

    @GetMapping(value = "/resources", params = "clubId")
    public ResponseEntity<List<Response>> getResources(@GeneratedClubMemberId Long clubMemberId,
                                                       @RequestParam Long clubId) {

        List<Response> response = resourceService.getResourceByClubId(clubMemberId, clubId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/resources/{resourceId}", params = "clubId")
    public ResponseEntity<Response> getResource(@GeneratedClubMemberId Long clubMemberId,
                                                       @RequestParam Long clubId,
                                                      @PathVariable Long resourceId) {

        Response response = resourceService.getResourceById(clubMemberId, resourceId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping(value = "/resources/{resourceId}", params = "clubId")
    public ResponseEntity<Response> updateResource(@GeneratedClubMemberId Long clubMemberId,
                                                   @RequestParam Long clubId,
                                                   @PathVariable Long resourceId,
                                                   @RequestBody Update updateDto) {

        Response response = resourceService.updateResource(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping(value = "/resources/{resourceId}", params = "clubId")
    public ResponseEntity deleteResource(@GeneratedClubMemberId Long clubMemberId,
                                                @RequestParam Long clubId,
                                                @PathVariable Long resourceId) {

        resourceService.deleteResource(clubMemberId, resourceId);

        return ResponseEntity.noContent().build();
    }





}

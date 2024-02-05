package com.dp.dplanner.controller;

import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.dp.dplanner.dto.ResourceDto.*;

@RestController
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(value = "/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<Response> createResource(@AuthenticationPrincipal PrincipalDetails principal,
                                                   @RequestBody Create createDto) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = resourceService.createResource(clubMemberId, createDto);

        return CommonResponse.createSuccess(response);

    }

    @GetMapping(value = "/resources")
    public CommonResponse<List<Response>> getResources(@AuthenticationPrincipal PrincipalDetails principal) {

        Long clubMemberId = principal.getClubMemberId();
        Long clubId = principal.getClubId();

        List<Response> response = resourceService.getResourceByClubId(clubMemberId, clubId);

        return CommonResponse.createSuccess(response);

    }

    @GetMapping(value = "/resources/{resourceId}")
    public CommonResponse<Response> getResource(@AuthenticationPrincipal PrincipalDetails principal,
                                                @PathVariable Long resourceId) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = resourceService.getResourceById(clubMemberId, resourceId);

        return CommonResponse.createSuccess(response);

    }

    @PutMapping(value = "/resources/{resourceId}")
    public CommonResponse<Response> updateResource(@AuthenticationPrincipal PrincipalDetails principal,
                                                   @PathVariable Long resourceId,
                                                   @RequestBody Update updateDto) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = resourceService.updateResource(clubMemberId, updateDto);

        return CommonResponse.createSuccess(response);

    }

    @DeleteMapping(value = "/resources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse deleteResource(@AuthenticationPrincipal PrincipalDetails principal,
                                         @PathVariable Long resourceId) {

        Long clubMemberId = principal.getClubMemberId();

        resourceService.deleteResource(clubMemberId, resourceId);

        return CommonResponse.createSuccessWithNoContent();
    }





}

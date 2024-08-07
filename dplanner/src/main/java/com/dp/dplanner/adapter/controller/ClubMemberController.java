package com.dp.dplanner.adapter.controller;

import com.dp.dplanner.adapter.dto.ClubMemberDto;
import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.adapter.exception.ApiException;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.config.security.PrincipalDetails;
import com.dp.dplanner.service.ClubMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs/{clubId}/club-members")

public class ClubMemberController {

    private final ClubMemberService clubMemberService;


    @GetMapping(value = "")
    public CommonResponse<List<ClubMemberDto.Response>> findMyClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                                                          @PathVariable("clubId") Long clubId,
                                                                          @RequestParam(name="confirmed",required = false) Boolean confirmed) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Long clubMemberId = principal.getClubMemberId();
        List<ClubMemberDto.Response> response;

        if (confirmed == null || confirmed) {
            response = clubMemberService.findMyClubMembers(clubMemberId,clubId);
        }else{
            response = clubMemberService.findMyClubMembers(clubMemberId,clubId, false);
        }
        return CommonResponse.createSuccess(response);
    }

    @PatchMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse confirmClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                             @PathVariable("clubId") Long clubId,
                                             @RequestBody @Valid ClubMemberDto.Request requestDto) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Long clubMemberId = principal.getClubMemberId();

        clubMemberService.confirmAll(clubMemberId, List.of(requestDto));
        return CommonResponse.createSuccessWithNoContent();
    }

    @PatchMapping("/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse rejectClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                             @PathVariable("clubId") Long clubId,
                                             @RequestBody @Valid ClubMemberDto.Request requestDto) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Long clubMemberId = principal.getClubMemberId();

        clubMemberService.rejectAll(clubMemberId, List.of(requestDto));
        return CommonResponse.createSuccessWithNoContent();
    }

    @GetMapping("/{clubMemberId}")
    public CommonResponse<ClubMemberDto.Response> findClubMemberById(@AuthenticationPrincipal PrincipalDetails principal,
                                                                     @PathVariable("clubId") Long clubId,
                                                                     @PathVariable("clubMemberId") Long requestClubMemberId) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Long clubMemberId = principal.getClubMemberId();

        ClubMemberDto.Response response =
                clubMemberService.findById(clubMemberId, new ClubMemberDto.Request(requestClubMemberId));

        return CommonResponse.createSuccess(response);
    }

    @PatchMapping("/{clubMemberId}")
    public CommonResponse<ClubMemberDto.Response> updateClubMember(@AuthenticationPrincipal PrincipalDetails principal,
                                                                   @PathVariable("clubMemberId") Long requestClubMemberId,
                                                                   @RequestBody @Valid ClubMemberDto.Update updateDto) {
        if (!principal.getClubMemberId().equals(requestClubMemberId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Long clubMemberId = principal.getClubMemberId();

        updateDto.setId(requestClubMemberId);
        ClubMemberDto.Response response = clubMemberService.update(clubMemberId, updateDto);
        return CommonResponse.createSuccess(response);
    }

    @DeleteMapping(value = "/{clubMemberId}/kickout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse kickOutClubMember(@AuthenticationPrincipal PrincipalDetails principal,
                                           @PathVariable("clubId") Long clubId,
                                           @PathVariable("clubMemberId") Long requestClubMemberId){
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Long managerId = principal.getClubMemberId();
        clubMemberService.kickOut(managerId, clubId, new ClubMemberDto.Request(requestClubMemberId));

        return CommonResponse.createSuccessWithNoContent();
    }

    @DeleteMapping(value = "/{clubMemberId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse leaveClub(@AuthenticationPrincipal PrincipalDetails principal,
                                           @PathVariable("clubMemberId") Long requestClubMemberId) {
        if (!principal.getClubMemberId().equals(requestClubMemberId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Long clubMemberId = principal.getClubMemberId();
        clubMemberService.leaveClub(clubMemberId,new ClubMemberDto.Request(requestClubMemberId));
        return CommonResponse.createSuccessWithNoContent();
    }

//    @DeleteMapping("")
//    public CommonResponse<List<ClubMemberDto.Response>> deleteClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
//                                                                          @PathVariable("clubId") Long clubId,
//                                                                          @RequestBody @Valid List<ClubMemberDto.Request> requestDto) {
//        Long clubMemberId = principal.getClubMemberId();
//
//        List<ClubMemberDto.Response> response = clubMemberService.kickOutAll(clubMemberId, clubId, requestDto);
//        return CommonResponse.createSuccess(response);
//    }

    @PatchMapping("/{clubMemberId}/role")
    public CommonResponse<ClubMemberDto.Response> updateClubMemberRole(@AuthenticationPrincipal PrincipalDetails principal,
                                                                       @PathVariable("clubId") Long clubId,
                                                                       @PathVariable("clubMemberId") Long requestClubMemberId,
                                                                       @RequestBody @Valid ClubMemberDto.Update updateDto) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        Long clubMemberId = principal.getClubMemberId();

        updateDto.setId(requestClubMemberId);
        ClubMemberDto.Response response = clubMemberService.updateClubMemberClubAuthority(clubMemberId, clubId, updateDto);

        return CommonResponse.createSuccess(response);
    }

    @PostMapping("/{clubMemberId}/update-profile-image")
    public CommonResponse<ClubMemberDto.Response> changeClubMemberProfileImage(@AuthenticationPrincipal PrincipalDetails principal,
                                                                          @PathVariable("clubMemberId") Long clubMemberId,
                                                                          @RequestBody MultipartFile image) {
        if (!principal.getClubMemberId().equals(clubMemberId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        ClubMemberDto.Response responseDto = clubMemberService.changeClubMemberProfileImage(clubMemberId, image);
        return CommonResponse.createSuccess(responseDto);
    }

    @PostMapping("{blockedClubMemberId}/block")
    public CommonResponse blockClubMember(@AuthenticationPrincipal PrincipalDetails principal,
                                          @PathVariable("blockedClubMemberId") Long blockedClubMemberId) {
        Long clubMemberId = principal.getClubMemberId();
        clubMemberService.blockClubMember(clubMemberId, blockedClubMemberId);
        return CommonResponse.createSuccessWithNoContent();
    }
}

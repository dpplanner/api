package com.dp.dplanner.controller;

import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ClubMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs/{clubId}/club-members")

public class ClubMemberController {

    private final ClubMemberService clubMemberService;

//    @GetMapping("")
//    public ResponseEntity<List<ClubMemberDto.Response>> findMyClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
//                                                                          @PathVariable("clubId") Long clubId) {
//
//        Long clubMemberId = principal.getClubMemberId();
//        List<ClubMemberDto.Response> response = clubMemberService.findMyClubMembers(clubMemberId,clubId);
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(response);
//    }

    @GetMapping(value = "")
    public CommonResponse<List<ClubMemberDto.Response>> findMyClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                                                          @PathVariable("clubId") Long clubId,
                                                                          @RequestParam(required = false) Boolean confirmed) {
        Long clubMemberId = principal.getClubMemberId();
        List<ClubMemberDto.Response> response;

        if (confirmed == null) {
            // ToDo 리팩토링 필요
            response = clubMemberService.findMyClubMembers(clubMemberId,clubId);
        }else{
            response = clubMemberService.findMyClubMembers(clubMemberId,clubId, confirmed);

        }
        return CommonResponse.createSuccess(response);
    }

    @DeleteMapping("")
    public CommonResponse<List<ClubMemberDto.Response>> deleteClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                                                          @PathVariable("clubId") Long clubId,
                                                                          @RequestBody @Valid List<ClubMemberDto.Request> requestDto) {
        Long clubMemberId = principal.getClubMemberId();

        List<ClubMemberDto.Response> response = clubMemberService.kickOutAll(clubMemberId, clubId, requestDto);
        return CommonResponse.createSuccess(response);
    }

    @PatchMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse confirmClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                             @PathVariable("clubId") Long clubId,
                                             @RequestBody @Valid List<ClubMemberDto.Request> requestDto) {

        Long clubMemberId = principal.getClubMemberId();

        clubMemberService.confirmAll(clubMemberId, requestDto);
        return CommonResponse.createSuccessWithNoContent();
    }

    @GetMapping("/{clubMemberId}")
    public CommonResponse<ClubMemberDto.Response> findClubMemberById(@AuthenticationPrincipal PrincipalDetails principal,
                                                                     @PathVariable("clubId") Long clubId,
                                                                     @PathVariable("clubMemberId") Long requestClubMemberId) {
        Long clubMemberId = principal.getClubMemberId();

        ClubMemberDto.Response response =
                clubMemberService.findById(clubMemberId, new ClubMemberDto.Request(requestClubMemberId));

        return CommonResponse.createSuccess(response);
    }

    @PatchMapping("/{clubMemberId}")
    public CommonResponse<ClubMemberDto.Response> updateClubMember(@AuthenticationPrincipal PrincipalDetails principal,
                                                                   @PathVariable("clubId") Long clubId,
                                                                   @PathVariable("clubMemberId") Long requestClubMemberId,
                                                                   @RequestBody @Valid ClubMemberDto.Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();

        updateDto.setId(requestClubMemberId);
        ClubMemberDto.Response response = clubMemberService.update(clubMemberId, updateDto);
        return CommonResponse.createSuccess(response);
    }

    @DeleteMapping(value = "/{clubMemberId}", params = "force")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse deleteClubMember(@AuthenticationPrincipal PrincipalDetails principal,
                                           @PathVariable("clubId") Long clubId,
                                           @PathVariable("clubMemberId") Long requestClubMemberId,
                                           @RequestParam(defaultValue = "false") boolean force) {

        Long clubMemberId = principal.getClubMemberId();

        if (force) {
            clubMemberService.kickOut(clubMemberId, clubId, new ClubMemberDto.Request(requestClubMemberId));
        } else {
            clubMemberService.leaveClub(clubMemberId,new ClubMemberDto.Request(requestClubMemberId));
        }
        return CommonResponse.createSuccessWithNoContent();
    }

    @PatchMapping("/{clubMemberId}/role")
    public CommonResponse<ClubMemberDto.Response> updateClubMemberRole(@AuthenticationPrincipal PrincipalDetails principal,
                                                                       @PathVariable("clubId") Long clubId,
                                                                       @PathVariable("clubMemberId") Long requestClubMemberId,
                                                                       @RequestBody @Valid ClubMemberDto.Update updateDto) {
        Long clubMemberId = principal.getClubMemberId();

        updateDto.setId(requestClubMemberId);
        ClubMemberDto.Response response = clubMemberService.updateClubMemberClubAuthority(clubMemberId, clubId, updateDto);

        return CommonResponse.createSuccess(response);
    }
}

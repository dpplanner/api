package com.dp.dplanner.controller;

import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ClubMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs/{clubId}/club-members")
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    @GetMapping("")
    public ResponseEntity<List<ClubMemberDto.Response>> findMyClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                                                          @PathVariable("clubId") Long clubId) {

        Long clubMemberId = principal.getClubMemberId();
        List<ClubMemberDto.Response> response = clubMemberService.findMyClubMembers(clubMemberId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "", params = "confirmed")
    public ResponseEntity<List<ClubMemberDto.Response>> findMyClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                                                          @PathVariable("clubId") Long clubId,
                                                                          @RequestParam(defaultValue = "true") boolean confirmed) {
        Long clubMemberId = principal.getClubMemberId();

        List<ClubMemberDto.Response> response = clubMemberService.findMyClubMembers(clubMemberId, confirmed);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("")
    public ResponseEntity<List<ClubMemberDto.Response>> deleteClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                                                          @PathVariable("clubId") Long clubId,
                                                                          @RequestBody @Valid List<ClubMemberDto.Request> requestDto) {
        Long clubMemberId = principal.getClubMemberId();

        List<ClubMemberDto.Response> response = clubMemberService.kickOutAll(clubMemberId, requestDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/confirm")
    public ResponseEntity confirmClubMembers(@AuthenticationPrincipal PrincipalDetails principal,
                                             @PathVariable("clubId") Long clubId,
                                             @RequestBody @Valid List<ClubMemberDto.Request> requestDto) {

        Long clubMemberId = principal.getClubMemberId();

        clubMemberService.confirmAll(clubMemberId, requestDto);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping("/{clubMemberId}")
    public ResponseEntity<ClubMemberDto.Response> findClubMemberById(@AuthenticationPrincipal PrincipalDetails principal,
                                                                     @PathVariable("clubId") Long clubId,
                                                                     @PathVariable("clubMemberId") Long requestClubMemberId) {
        Long clubMemberId = principal.getClubMemberId();

        ClubMemberDto.Response response =
                clubMemberService.findById(clubMemberId, new ClubMemberDto.Request(requestClubMemberId));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{clubMemberId}")
    public ResponseEntity<ClubMemberDto.Response> updateClubMember(@AuthenticationPrincipal PrincipalDetails principal,
                                                                   @PathVariable("clubId") Long clubId,
                                                                   @PathVariable("clubMemberId") Long requestClubMemberId,
                                                                   @RequestBody @Valid ClubMemberDto.Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();

        updateDto.setId(requestClubMemberId);
        ClubMemberDto.Response response = clubMemberService.update(clubMemberId, updateDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping(value = "/{clubMemberId}", params = "force")
    public ResponseEntity deleteClubMember(@AuthenticationPrincipal PrincipalDetails principal,
                                           @PathVariable("clubId") Long clubId,
                                           @PathVariable("clubMemberId") Long requestClubMemberId,
                                           @RequestParam(defaultValue = "false") boolean force) {

        Long clubMemberId = principal.getClubMemberId();

        if (force) {
            clubMemberService.kickOut(clubMemberId, new ClubMemberDto.Request(requestClubMemberId));
        } else {
            clubMemberService.leaveClub(clubMemberId);
        }
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PatchMapping("/{clubMemberId}/role")
    public ResponseEntity<ClubMemberDto.Response> updateClubMemberRole(@AuthenticationPrincipal PrincipalDetails principal,
                                                                       @PathVariable("clubId") Long clubId,
                                                                       @PathVariable("clubMemberId") Long requestClubMemberId,
                                                                       @RequestBody @Valid ClubMemberDto.Update updateDto) {
        Long clubMemberId = principal.getClubMemberId();

        updateDto.setId(requestClubMemberId);
        ClubMemberDto.Response response = clubMemberService.updateClubMemberRole(clubMemberId, updateDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{clubMemberId}/confirm")
    public ResponseEntity confirmClubMember(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable("clubId") Long clubId,
                                            @PathVariable("clubMemberId") Long requestClubMemberId) {

        Long clubMemberId = principal.getClubMemberId();

        ClubMemberDto.Request requestDto = new ClubMemberDto.Request(requestClubMemberId);
        clubMemberService.confirm(clubMemberId, requestDto);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}

package com.dp.dplanner.controller;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.service.ClubMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs/{clubId}/club-members")
public class ClubMemberController {

    private final ClubMemberService clubMemberService;

    @GetMapping("")
    public ResponseEntity<List<ClubMemberDto.Response>> findMyClubMembers(@GeneratedClubMemberId Long clubMemberId,
                                                                          @PathVariable("clubId") Long clubId) {
        List<ClubMemberDto.Response> response = clubMemberService.findMyClubMembers(clubMemberId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "", params = "confirmed")
    public ResponseEntity<List<ClubMemberDto.Response>> findMyClubMembers(@GeneratedClubMemberId Long clubMemberId,
                                                                          @PathVariable("clubId") Long clubId,
                                                                          @RequestParam(defaultValue = "true") boolean confirmed) {

        List<ClubMemberDto.Response> response = clubMemberService.findMyClubMembers(clubMemberId, confirmed);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("")
    public ResponseEntity<List<ClubMemberDto.Response>> deleteClubMembers(@GeneratedClubMemberId Long clubMemberId,
                                                                          @PathVariable("clubId") Long clubId,
                                                                          @RequestBody @Valid List<ClubMemberDto.Request> requestDto) {

        List<ClubMemberDto.Response> response = clubMemberService.kickOutAll(clubMemberId, requestDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/confirm")
    public ResponseEntity confirmClubMembers(@GeneratedClubMemberId Long clubMemberId,
                                             @PathVariable("clubId") Long clubId,
                                             @RequestBody @Valid List<ClubMemberDto.Request> requestDto) {
        System.out.println("requestDto = " + requestDto);
        clubMemberService.confirmAll(clubMemberId, requestDto);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @GetMapping("/{clubMemberId}")
    public ResponseEntity<ClubMemberDto.Response> findClubMemberById(@GeneratedClubMemberId Long clubMemberId,
                                                                     @PathVariable("clubId") Long clubId,
                                                                     @PathVariable("clubMemberId") Long requestClubMemberId) {
        ClubMemberDto.Response response =
                clubMemberService.findById(clubMemberId, new ClubMemberDto.Request(requestClubMemberId));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{clubMemberId}")
    public ResponseEntity<ClubMemberDto.Response> updateClubMember(@GeneratedClubMemberId Long clubMemberId,
                                                                   @PathVariable("clubId") Long clubId,
                                                                   @PathVariable("clubMemberId") Long requestClubMemberId,
                                                                   @RequestBody @Valid ClubMemberDto.Update updateDto) {

        updateDto.setId(requestClubMemberId);
        ClubMemberDto.Response response = clubMemberService.update(clubMemberId, updateDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping(value = "/{clubMemberId}", params = "force")
    public ResponseEntity deleteClubMember(@GeneratedClubMemberId Long clubMemberId,
                                           @PathVariable("clubId") Long clubId,
                                           @PathVariable("clubMemberId") Long requestClubMemberId,
                                           @RequestParam(defaultValue = "false") boolean force) {
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
    public ResponseEntity<ClubMemberDto.Response> updateClubMemberRole(@GeneratedClubMemberId Long clubMemberId,
                                                                       @PathVariable("clubId") Long clubId,
                                                                       @PathVariable("clubMemberId") Long requestClubMemberId,
                                                                       @RequestBody @Valid ClubMemberDto.Update updateDto) {
        updateDto.setId(requestClubMemberId);
        ClubMemberDto.Response response = clubMemberService.updateClubMemberRole(clubMemberId, updateDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{clubMemberId}/confirm")
    public ResponseEntity confirmClubMember(@GeneratedClubMemberId Long clubMemberId,
                                            @PathVariable("clubId") Long clubId,
                                            @PathVariable("clubMemberId") Long requestClubMemberId) {
        ClubMemberDto.Request requestDto = new ClubMemberDto.Request(requestClubMemberId);
        clubMemberService.confirm(clubMemberId, requestDto);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}

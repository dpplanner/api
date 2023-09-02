package com.dp.dplanner.controller;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.dto.ClubMemberDto;
import com.dp.dplanner.dto.InviteDto;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;

    @PostMapping("")
    public ResponseEntity<ClubDto.Response> createClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                       @RequestBody @Valid ClubDto.Create createDto) {
        Long memberId = principal.getId();
        ClubDto.Response responseDto = clubService.createClub(memberId, createDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseDto);
    }

    @GetMapping("")
    public ResponseEntity<List<ClubDto.Response>> findMyClubs(@AuthenticationPrincipal PrincipalDetails principal) {
        Long memberId = principal.getId();
        List<ClubDto.Response> myClubs = clubService.findMyClubs(memberId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(myClubs);
    }

    @GetMapping("/{clubId}")
    public ResponseEntity<ClubDto.Response> findClubById(@PathVariable("clubId") Long clubId) {
        ClubDto.Response responseDto = clubService.findClubById(clubId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(responseDto);
    }

    @PatchMapping("/{clubId}")
    public ResponseEntity<ClubDto.Response> updateClubInfo(@GeneratedClubMemberId Long clubMemberId,
                                                           @PathVariable("clubId") Long clubId,
                                                           @RequestBody @Valid ClubDto.Update updateDto) {
        updateDto.setClubId(clubId);
        ClubDto.Response response = clubService.updateClubInfo(clubMemberId, updateDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{clubId}/invite")
    public ResponseEntity<InviteDto> inviteClub(@GeneratedClubMemberId Long clubMemberId,
                                                @PathVariable("clubId") Long clubId) {
        InviteDto response = clubService.inviteClub(clubMemberId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/{clubId}/join")
    public ResponseEntity<ClubMemberDto.Response> joinClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                           @PathVariable("clubId") Long clubId,
                                                           @RequestBody InviteDto inviteDto) {
        Long memberId = principal.getId();

        ClubMemberDto.Response response = clubService.joinClub(memberId, inviteDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{clubId}/authorities")
    public ResponseEntity<ClubAuthorityDto.Response> findManagerAuthorities(@GeneratedClubMemberId Long clubMemberId,
                                                                            @PathVariable("clubId") Long clubId) {
        ClubAuthorityDto.Response authorities =
                clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authorities);
    }

    @PutMapping("/{clubId}/authorities")
    public ResponseEntity<ClubAuthorityDto.Response> setManagerAuthorities(@GeneratedClubMemberId Long clubMemberId,
                                                                           @PathVariable("clubId") Long clubId,
                                                                           @RequestBody @Valid ClubAuthorityDto.Update updateDto) {
        updateDto.setClubId(clubId);
        ClubAuthorityDto.Response authorities = clubService.setManagerAuthority(clubMemberId, updateDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authorities);
    }

}

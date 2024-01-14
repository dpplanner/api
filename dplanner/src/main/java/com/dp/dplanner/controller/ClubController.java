package com.dp.dplanner.controller;

import com.dp.dplanner.dto.ClubAuthorityDto;
import com.dp.dplanner.dto.ClubDto;
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

    @GetMapping("/{clubId}/invite")
    public ResponseEntity<InviteDto> inviteClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                @PathVariable("clubId") Long clubId) {

        Long clubMemberId = principal.getClubMemberId();
        InviteDto response = clubService.inviteClub(clubMemberId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/{clubId}")
    public ResponseEntity<ClubDto.Response> updateClubInfo(@AuthenticationPrincipal PrincipalDetails principal,
                                                           @PathVariable("clubId") Long clubId,
                                                           @RequestBody @Valid ClubDto.Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();
        updateDto.setClubId(clubId);
        ClubDto.Response response = clubService.updateClubInfo(clubMemberId, updateDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    // joinClub

    @GetMapping("/{clubId}/authorities")
    public ResponseEntity<ClubAuthorityDto.Response> findManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                            @PathVariable("clubId") Long clubId) {

        Long clubMemberId = principal.getClubMemberId();
        ClubAuthorityDto.Response authorities =
                clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authorities);
    }

    @PutMapping("/{clubId}/authorities")
    public ResponseEntity<ClubAuthorityDto.Response> setManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                           @PathVariable("clubId") Long clubId,
                                                                           @RequestBody @Valid ClubAuthorityDto.Update updateDto) {
        Long clubMemberId = principal.getClubMemberId();

        updateDto.setClubId(clubId);
        ClubAuthorityDto.Response authorities = clubService.setManagerAuthority(clubMemberId, updateDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authorities);
    }

}

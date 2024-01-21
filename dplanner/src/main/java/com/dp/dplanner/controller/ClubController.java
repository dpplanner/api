package com.dp.dplanner.controller;

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
import java.util.Map;

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

    @GetMapping(value = "")
    public ResponseEntity<List<ClubDto.Response>> findClubs(@RequestParam Map<String, String> param) {

        List<ClubDto.Response> responseDto;

        if (param.get("memberId") != null) {
            Long memberId = Long.valueOf(param.get("memberId"));
            responseDto = clubService.findMyClubs(memberId);
        }else{
            //todo param에 따라 정렬 및 검색 기능 추가
            responseDto = clubService.findClubs(param);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(responseDto);
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
        InviteDto response = clubService.inviteClub(clubMemberId,clubId);

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

    @PostMapping("/{clubId}/join")
    public ResponseEntity<ClubMemberDto.Response> joinClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                           @PathVariable("clubId") Long clubId,
                                                           @RequestBody @Valid InviteDto inviteDto) {
        Long memberId = principal.getId();

        ClubMemberDto.Response response = clubService.joinClub(memberId, inviteDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{clubId}/authorities")
    public ResponseEntity<ClubAuthorityDto.Response> createManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                              @PathVariable("clubId") Long clubId,
                                                                              @RequestBody @Valid ClubAuthorityDto.Create createDto) {
        Long clubMemberId = principal.getClubMemberId();

        createDto.setClubId(clubId);
        ClubAuthorityDto.Response authorities = clubService.createClubAuthority(clubMemberId, createDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authorities);
    }

    @GetMapping("/{clubId}/authorities")
    public ResponseEntity<List<ClubAuthorityDto.Response>> findManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                                  @PathVariable("clubId") Long clubId) {

        Long clubMemberId = principal.getClubMemberId();
        List<ClubAuthorityDto.Response> authorities = clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authorities);
    }

    @PutMapping("/{clubId}/authorities")
    public ResponseEntity<ClubAuthorityDto.Response> updateManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                              @PathVariable("clubId") Long clubId,
                                                                              @RequestBody @Valid ClubAuthorityDto.Update updateDto) {
        Long clubMemberId = principal.getClubMemberId();

        updateDto.setClubId(clubId);
        ClubAuthorityDto.Response authorities = clubService.updateClubAuthority(clubMemberId, updateDto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authorities);
    }

}

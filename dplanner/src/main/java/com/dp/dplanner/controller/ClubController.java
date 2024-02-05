package com.dp.dplanner.controller;

import com.dp.dplanner.dto.*;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<ClubDto.Response> createClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                       @RequestBody @Valid ClubDto.Create createDto) {
        Long memberId = principal.getId();
        ClubDto.Response responseDto = clubService.createClub(memberId, createDto);
        return CommonResponse.createSuccess(responseDto);
    }

    @GetMapping(value = "")
    public CommonResponse<List<ClubDto.Response>> findClubs(@RequestParam Map<String, String> param) {

        List<ClubDto.Response> responseDto;

        if (param.get("memberId") != null) {
            Long memberId = Long.valueOf(param.get("memberId"));
            responseDto = clubService.findMyClubs(memberId);
        }else{
            //todo param에 따라 정렬 및 검색 기능 추가
            responseDto = clubService.findClubs(param);
        }

        return CommonResponse.createSuccess(responseDto);
    }

    @GetMapping("/{clubId}")
    public CommonResponse<ClubDto.Response> findClubById(@PathVariable("clubId") Long clubId) {

        ClubDto.Response responseDto = clubService.findClubById(clubId);
        return CommonResponse.createSuccess(responseDto);
    }

    @GetMapping("/{clubId}/invite")
    public CommonResponse<InviteDto> inviteClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                @PathVariable("clubId") Long clubId) {

        Long clubMemberId = principal.getClubMemberId();
        InviteDto responseDto = clubService.inviteClub(clubMemberId,clubId);

        return CommonResponse.createSuccess(responseDto);
    }

    @PatchMapping("/{clubId}")
    public CommonResponse<ClubDto.Response> updateClubInfo(@AuthenticationPrincipal PrincipalDetails principal,
                                                           @PathVariable("clubId") Long clubId,
                                                           @RequestBody @Valid ClubDto.Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();
        updateDto.setClubId(clubId);
        ClubDto.Response responseDto = clubService.updateClubInfo(clubMemberId, updateDto);

        return CommonResponse.createSuccess(responseDto);
    }

    @PostMapping("/{clubId}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<ClubMemberDto.Response> joinClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                           @PathVariable("clubId") Long clubId,
                                                           @RequestBody @Valid InviteDto inviteDto) {
        Long memberId = principal.getId();

        ClubMemberDto.Response responseDto = clubService.joinClub(memberId, inviteDto);
        return CommonResponse.createSuccess(responseDto);
    }

    @PostMapping("/{clubId}/authorities")
    public CommonResponse<ClubAuthorityDto.Response> createManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                              @PathVariable("clubId") Long clubId,
                                                                              @RequestBody @Valid ClubAuthorityDto.Create createDto) {
        Long clubMemberId = principal.getClubMemberId();

        createDto.setClubId(clubId);
        ClubAuthorityDto.Response responseDto = clubService.createClubAuthority(clubMemberId, createDto);

        return CommonResponse.createSuccess(responseDto);
    }

    @GetMapping("/{clubId}/authorities")
    public CommonResponse<List<ClubAuthorityDto.Response>> findManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                                  @PathVariable("clubId") Long clubId) {

        Long clubMemberId = principal.getClubMemberId();
        List<ClubAuthorityDto.Response> responseDto = clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));

        return CommonResponse.createSuccess(responseDto);
    }

    @PutMapping("/{clubId}/authorities")
    public CommonResponse<ClubAuthorityDto.Response> updateManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                              @PathVariable("clubId") Long clubId,
                                                                              @RequestBody @Valid ClubAuthorityDto.Update updateDto) {
        Long clubMemberId = principal.getClubMemberId();

        updateDto.setClubId(clubId);
        ClubAuthorityDto.Response responseDto = clubService.updateClubAuthority(clubMemberId, updateDto);

        return CommonResponse.createSuccess(responseDto);
    }

}

package com.dp.dplanner.adapter.controller;

import com.dp.dplanner.adapter.dto.*;
import com.dp.dplanner.adapter.exception.ApiException;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.config.security.PrincipalDetails;
import com.dp.dplanner.service.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    @GetMapping(value = "", params = {"memberId"})
    public CommonResponse<List<ClubDto.Response>> findClubs(@AuthenticationPrincipal PrincipalDetails principal,
                                                            @RequestParam("memberId") Long memberId) {
        if (!principal.getId().equals(memberId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        List<ClubDto.Response> responseDto;
        responseDto = clubService.findMyClubs(memberId);
        return CommonResponse.createSuccess(responseDto);
    }

    @GetMapping("/{clubId}")
    public CommonResponse<ClubDto.Response> findClubById(@PathVariable("clubId") Long clubId) {

        ClubDto.Response responseDto = clubService.findClubById(clubId);
        return CommonResponse.createSuccess(responseDto);
    }

    @PatchMapping("/{clubId}")
    public CommonResponse<ClubDto.Response> updateClubInfo(@AuthenticationPrincipal PrincipalDetails principal,
                                                           @PathVariable("clubId") Long clubId,
                                                           @RequestBody @Valid ClubDto.Update updateDto) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubMemberId = principal.getClubMemberId();
        updateDto.setClubId(clubId);
        ClubDto.Response responseDto = clubService.updateClubInfo(clubMemberId, updateDto);

        return CommonResponse.createSuccess(responseDto);
    }

    @PostMapping("/{clubId}/invite") // 초대 코드 생성
    public CommonResponse<InviteDto> inviteClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                @PathVariable("clubId") Long clubId) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubMemberId = principal.getClubMemberId();
        InviteDto responseDto = clubService.inviteClub(clubMemberId,clubId);

        return CommonResponse.createSuccess(responseDto);
    }

    @GetMapping(value = "/join", params = "code")
    public CommonResponse<InviteDto> verifyCode(@RequestParam(name = "code") String code) {

        InviteDto responseDto = clubService.verifyInviteCode(code);

        return CommonResponse.createSuccess(responseDto);
    }

    // todo 바로 여기로 inviteCode 검증 안 하고 요청 보내면?
    @PostMapping(value = "/{clubId}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<ClubMemberDto.Response> joinClub(@AuthenticationPrincipal PrincipalDetails principal,
                                                           @PathVariable("clubId") Long clubId,
                                                           @RequestBody ClubMemberDto.Create create) {

        Long memberId = principal.getId();
        create.setClubId(clubId);
        ClubMemberDto.Response responseDto = clubService.joinClub(memberId, create);

        return CommonResponse.createSuccess(responseDto);
    }


    @PostMapping("/{clubId}/authorities")
    public CommonResponse<ClubAuthorityDto.Response> createManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                              @PathVariable("clubId") Long clubId,
                                                                              @RequestBody @Valid ClubAuthorityDto.Create createDto) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubMemberId = principal.getClubMemberId();
        createDto.setClubId(clubId);
        ClubAuthorityDto.Response responseDto = clubService.createClubAuthority(clubMemberId, createDto);

        return CommonResponse.createSuccess(responseDto);
    }

    @GetMapping("/{clubId}/authorities")
    public CommonResponse<List<ClubAuthorityDto.Response>> findManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                                  @PathVariable("clubId") Long clubId) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubMemberId = principal.getClubMemberId();
        List<ClubAuthorityDto.Response> responseDto = clubService.findClubManagerAuthorities(clubMemberId, new ClubAuthorityDto.Request(clubId));

        return CommonResponse.createSuccess(responseDto);
    }

    @PutMapping("/{clubId}/authorities")
    public CommonResponse<ClubAuthorityDto.Response> updateManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                                              @PathVariable("clubId") Long clubId,
                                                                              @RequestBody @Valid ClubAuthorityDto.Update updateDto) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubMemberId = principal.getClubMemberId();
        updateDto.setClubId(clubId);
        ClubAuthorityDto.Response responseDto = clubService.updateClubAuthority(clubMemberId, updateDto);

        return CommonResponse.createSuccess(responseDto);
    }

    @DeleteMapping("/{clubId}/authorities")
    public CommonResponse deleteManagerAuthorities(@AuthenticationPrincipal PrincipalDetails principal,
                                                   @PathVariable("clubId") Long clubId,
                                                   @RequestBody @Valid ClubAuthorityDto.Delete deleteDto) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubMemberId = principal.getClubMemberId();
        deleteDto.setClubId(clubId);
        clubService.deleteClubAuthority(clubMemberId, deleteDto);

        return CommonResponse.createSuccessWithNoContent();
    }
    @PostMapping("/{clubId}/update-club-image")
    public CommonResponse<ClubDto.Response> changeClubRepresentativeImage(@AuthenticationPrincipal PrincipalDetails principal,
                                                                          @PathVariable("clubId") Long clubId,
                                                                          @RequestBody MultipartFile image) {
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubMemberId = principal.getClubMemberId();
        ClubDto.Response responseDto = clubService.changeClubRepresentativeImage(clubMemberId, clubId, image);
        return CommonResponse.createSuccess(responseDto);
    }
}

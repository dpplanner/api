package com.dp.dplanner.controller;

import com.dp.dplanner.dto.ClubDto;
import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.exception.AuthenticationException;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.firebase.FCMNotificationRequestDto;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PatchMapping("/members/{memberId}/refresh-fcmtoken")
    public CommonResponse updateFcmToken(@AuthenticationPrincipal PrincipalDetails principal,
                                         @PathVariable Long memberId,
                                         @RequestBody FCMNotificationRequestDto requestDto) {
        if (!principal.getId().equals(memberId)) {
            throw new AuthenticationException(ErrorResult.INVALID_TOKEN);
        }
        memberService.updateFCMToken(memberId, requestDto);

        return CommonResponse.createSuccessWithNoContent();

    }

    @PatchMapping("/members/{memberId}/change-club")
    public CommonResponse changeClub(@AuthenticationPrincipal PrincipalDetails principal,
                                     @PathVariable Long memberId,
                                     @RequestBody ClubDto.Request request) {

        if (!principal.getId().equals(memberId)) {
            throw new AuthenticationException(ErrorResult.INVALID_TOKEN);
        }
        memberService.changeRecentClub(memberId, request);

        return CommonResponse.createSuccessWithNoContent();

    }
}

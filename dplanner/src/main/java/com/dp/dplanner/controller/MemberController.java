package com.dp.dplanner.controller;

import com.dp.dplanner.domain.Member;
import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.exception.AuthenticationException;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.exception.MemberException;
import com.dp.dplanner.firebase.FCMNotificationRequestDto;
import com.dp.dplanner.repository.MemberRepository;
import com.dp.dplanner.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @PatchMapping("/members/{memberId}/refreshFcmToken")
    @Transactional
    public CommonResponse updateFcmToken(@AuthenticationPrincipal PrincipalDetails principal,
                                         @PathVariable Long memberId,
                                         @RequestBody FCMNotificationRequestDto requestDto) {
        if (!principal.getId().equals(memberId)) {
            throw new AuthenticationException(ErrorResult.INVALID_TOKEN);
        }
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new MemberException(ErrorResult.MEMBER_NOT_FOUND));
        member.updateFCMToken(requestDto.getRefreshFcmToken());

        return CommonResponse.createSuccessWithNoContent();

    }
}

package com.dp.dplanner.controller;

import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.dto.MessageDto;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/messages")
public class MessageController {
    private final MessageService messageService;

    @GetMapping(value = "")
    public CommonResponse<MessageDto.ResponseList> findMyMessages(@AuthenticationPrincipal PrincipalDetails principalDetails) {

        Long clubMemberId = principalDetails.getClubMemberId();

        MessageDto.ResponseList response = messageService.findMyMessage(clubMemberId);

        return CommonResponse.createSuccess(response);
    }

    @PatchMapping(value = "/{messageId}")
    public CommonResponse readMessage(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                      @PathVariable Long messageId) {

        Long clubMemberId = principalDetails.getClubMemberId();
        messageService.readMessage(clubMemberId, messageId);

        return CommonResponse.createSuccessWithNoContent();
    }
}

package com.dp.dplanner.controller;

import com.dp.dplanner.dto.CommentMemberLikeDto;
import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.dp.dplanner.dto.CommentDto.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping(value = "/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<Response> createComment(@AuthenticationPrincipal PrincipalDetails principal,
                                                  @RequestBody final Create createDto){

        Long clubMemberId = principal.getClubMemberId();
        Response response = commentService.createComment(clubMemberId, createDto);

        return CommonResponse.createSuccess(response);
    }

    @PutMapping(value = "/comments/{commentId}")
    public CommonResponse<Response> updateComment(@AuthenticationPrincipal PrincipalDetails principal,
                                                  @RequestBody final Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = commentService.updateComment(clubMemberId, updateDto);


        return CommonResponse.createSuccess(response);
    }

    @DeleteMapping(value = "/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse updateComment(@AuthenticationPrincipal PrincipalDetails principal,
                                        @PathVariable final Long commentId) {

        Long clubMemberId = principal.getClubMemberId();

        commentService.deleteComment(clubMemberId, commentId);

        return CommonResponse.createSuccessWithNoContent();
    }

    @PutMapping(value = "/comments/{commentId}/like")
    public CommonResponse<CommentMemberLikeDto.Response> likeComment(@AuthenticationPrincipal PrincipalDetails principal,
                                                                     @PathVariable final Long commentId) {

        Long clubMemberId = principal.getClubMemberId();

        CommentMemberLikeDto.Response response = commentService.likeComment(clubMemberId, commentId);


        return CommonResponse.createSuccess(response);
    }


    @GetMapping(value = "/posts/{postId}/comments")
    public CommonResponse<List<Response>> getComments(@AuthenticationPrincipal PrincipalDetails principal,
                                                      @PathVariable final Long postId) {

        Long clubMemberId = principal.getClubMemberId();

        List<Response> response = commentService.getCommentsByPostId(clubMemberId, postId);


        return CommonResponse.createSuccess(response);
    }

    @GetMapping(value = "/members/{memberId}/comments")
    public CommonResponse<List<Response>> getMyComments(@AuthenticationPrincipal PrincipalDetails principal,
                                                        @PathVariable final Long memberId) {

        Long clubMemberId = principal.getClubMemberId();
        Long clubId = principal.getClubId();

        List<Response> response = commentService.getCommentsByClubMemberId(clubMemberId, clubId);


        return CommonResponse.createSuccess(response);
    }

}

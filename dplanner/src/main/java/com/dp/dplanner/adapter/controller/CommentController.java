package com.dp.dplanner.adapter.controller;

import com.dp.dplanner.adapter.dto.CommentDto;
import com.dp.dplanner.adapter.dto.CommentMemberLikeDto;
import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.config.security.PrincipalDetails;
import com.dp.dplanner.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.dp.dplanner.adapter.dto.CommentDto.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping(value = "/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<Response> createComment(@AuthenticationPrincipal PrincipalDetails principal,
                                                  @RequestBody @Valid final Create createDto){

        Long clubMemberId = principal.getClubMemberId();
        Response response = commentService.createComment(clubMemberId, createDto);

        return CommonResponse.createSuccess(response);
    }

    @PutMapping(value = "/comments/{commentId}")
    public CommonResponse<Response> updateComment(@AuthenticationPrincipal PrincipalDetails principal,
                                                  @PathVariable("commentId") final Long commentId,
                                                  @RequestBody final Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();
        updateDto.setId(commentId);
        Response response = commentService.updateComment(clubMemberId, updateDto);


        return CommonResponse.createSuccess(response);
    }

    @DeleteMapping(value = "/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse deleteComment(@AuthenticationPrincipal PrincipalDetails principal,
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

    @PostMapping(value = "/comments/{commentId}/report")
    public CommonResponse reportPost(@AuthenticationPrincipal PrincipalDetails principal,
                                     @RequestBody @Valid CommentDto.Report report,
                                     @PathVariable final Long commentId) {
        report.setCommentId(commentId);
        report.setClubMemberId(principal.getClubMemberId());
        commentService.reportComment(report);
        return CommonResponse.createSuccessWithNoContent();
    }
}

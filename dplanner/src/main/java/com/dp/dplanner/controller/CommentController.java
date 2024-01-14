package com.dp.dplanner.controller;

import com.dp.dplanner.dto.CommentMemberLikeDto;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.dp.dplanner.dto.CommentDto.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping(value = "/comments")
    public ResponseEntity<Response> createComment(@AuthenticationPrincipal PrincipalDetails principal,
                                                  @RequestBody final Create createDto){

        Long clubMemberId = principal.getClubMemberId();
        Response response = commentService.createComment(clubMemberId, createDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping(value = "/comments/{commentId}")
    public ResponseEntity<Response> updateComment(@AuthenticationPrincipal PrincipalDetails principal,
                                                  @RequestBody final Update updateDto) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = commentService.updateComment(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping(value = "/comments/{commentId}")
    public ResponseEntity updateComment(@AuthenticationPrincipal PrincipalDetails principal,
                                        @PathVariable final Long commentId) {

        Long clubMemberId = principal.getClubMemberId();

        commentService.deleteComment(clubMemberId, commentId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/comments/{commentId}/like")
    public ResponseEntity<CommentMemberLikeDto.Response> likeComment(@AuthenticationPrincipal PrincipalDetails principal,
                                                                     @PathVariable final Long commentId) {

        Long clubMemberId = principal.getClubMemberId();

        CommentMemberLikeDto.Response response = commentService.likeComment(clubMemberId, commentId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }


    @GetMapping(value = "/posts/{postId}/comments")
    public ResponseEntity<List<Response>> getComments(@AuthenticationPrincipal PrincipalDetails principal,
                                                      @PathVariable final Long postId) {

        Long clubMemberId = principal.getClubMemberId();

        List<Response> response = commentService.getCommentsByPostId(clubMemberId, postId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/members/{memberId}/comments")
    public ResponseEntity<List<Response>> getMyComments(@AuthenticationPrincipal PrincipalDetails principal,
                                                        @PathVariable final Long memberId) {

        Long clubMemberId = principal.getClubMemberId();
        Long clubId = principal.getClubId();

        List<Response> response = commentService.getCommentsByClubMemberId(clubMemberId, clubId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

}

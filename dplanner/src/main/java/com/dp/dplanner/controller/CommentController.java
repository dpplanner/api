package com.dp.dplanner.controller;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.dto.CommentMemberLikeDto;
import com.dp.dplanner.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.dp.dplanner.dto.CommentDto.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping(value = "/comments", params = "clubId")
    public ResponseEntity<Response> createComment(@GeneratedClubMemberId Long clubMemberId,
                                                  @RequestParam final Long clubId,
                                                  @RequestBody final Create createDto){

        Response response = commentService.createComment(clubMemberId, createDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping(value = "/comments/{commentId}", params = "clubId")
    public ResponseEntity<Response> updateComment(@GeneratedClubMemberId Long clubMemberId,
                                               @RequestParam final Long clubId,
                                               @PathVariable final Long commentId,
                                               @RequestBody final Update updateDto) {

        Response response = commentService.updateComment(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping(value = "/comments/{commentId}", params = "clubId")
    public ResponseEntity updateComment(@GeneratedClubMemberId Long clubMemberId,
                                                  @RequestParam final Long clubId,
                                                  @PathVariable final Long commentId) {

        commentService.deleteComment(clubMemberId, commentId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/comments/{commentId}/like",params = "clubId")
    public ResponseEntity<CommentMemberLikeDto.Response> likeComment(@GeneratedClubMemberId Long clubMemberId,
                                        @RequestParam final Long clubId,
                                        @PathVariable final Long commentId) {

        CommentMemberLikeDto.Response response = commentService.likeComment(clubMemberId, commentId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }



    @GetMapping(value = "/posts/{postId}/comments", params = "clubId")
    public ResponseEntity<List<Response>> getComments(@GeneratedClubMemberId Long clubMemberId,
                                                      @RequestParam final Long clubId,
                                                      @PathVariable final Long postId) {

        List<Response> response = commentService.getCommentsByPostId(clubMemberId, postId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/members/{memberId}/comments", params = "clubId")
    public ResponseEntity<List<Response>> getMyComments(@GeneratedClubMemberId Long clubMemberId,
                                                        @RequestParam final Long clubId,
                                                        @PathVariable final Long memberId) {

        List<Response> response = commentService.getCommentsByClubMemberId(clubMemberId, clubId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

}

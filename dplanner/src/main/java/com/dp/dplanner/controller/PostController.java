package com.dp.dplanner.controller;

import com.dp.dplanner.aop.annotation.GeneratedClubMemberId;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.dp.dplanner.dto.PostDto.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;

    @PostMapping("/clubs/{clubId}/posts")
    public ResponseEntity<Response> createPost(@GeneratedClubMemberId Long clubMemberId,
                                               @PathVariable final Long clubId,
                                               @RequestBody @Valid final Create create) {
        Response response = postService.createPost(clubMemberId, create);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/clubs/{clubId}/posts")
    public ResponseEntity<Slice> getClubPosts(@GeneratedClubMemberId Long clubMemberId,
                                              @PathVariable final Long clubId,
                                              @PageableDefault final Pageable pageable) {
        SliceResponse response = postService.getPostsByClubId(clubMemberId, clubId, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }


    @GetMapping("/members/{memberId}/clubs/{clubId}/posts")
    public ResponseEntity<Slice> getMyPosts(@GeneratedClubMemberId Long clubMemberId,
                                            @PathVariable final Long clubId,
                                            @PathVariable final Long memberId,
                                            @PageableDefault final Pageable pageable) {

        SliceResponse response = postService.getMyPostsByClubId(clubMemberId, clubId, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<Response> getPost(@GeneratedClubMemberId Long clubMemberId,
                                            @PathVariable final Long postId) {
        Response response = postService.getPostById(clubMemberId, postId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity deletePost(@GeneratedClubMemberId Long clubMemberId,
                                     @PathVariable final Long postId) {
        postService.deletePostById(clubMemberId, postId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<Response> updatePost(@GeneratedClubMemberId Long clubMemberId,
                                               @PathVariable final Long postId,
                                               @RequestBody final Update updateDto) {
        Response response = postService.updatePost(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/posts/{postId}/like")
    public ResponseEntity<PostMemberLikeDto.Response> likePost(@GeneratedClubMemberId Long clubMemberId,
                                                               @PathVariable final Long postId) {

        PostMemberLikeDto.Response response = postService.likePost(clubMemberId, postId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/posts/{postId}/fix")
    public ResponseEntity<Response> fixPost(@GeneratedClubMemberId Long clubMemberId,
                                  @PathVariable final Long postId) {

        Response response = postService.toggleIsFixed(clubMemberId, postId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }
}

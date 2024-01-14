package com.dp.dplanner.controller;

import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.dp.dplanner.dto.PostDto.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;

    @PostMapping(value = "/posts")
    public ResponseEntity<Response> createPost(@AuthenticationPrincipal PrincipalDetails principal,
                                               @RequestBody @Valid final Create create) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = postService.createPost(clubMemberId, create);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping(value = "/posts")
    public ResponseEntity<Slice> getClubPosts(@AuthenticationPrincipal PrincipalDetails principal,
                                              @PageableDefault final Pageable pageable) {

        Long clubMemberId = principal.getClubMemberId();
        Long clubId = principal.getClubId();

        SliceResponse response = postService.getPostsByClubId(clubMemberId, clubId, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }


    @GetMapping(value = "/members/{memberId}/posts")
    public ResponseEntity<SliceResponse> getMyPosts(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable final Long memberId,
                                            @PageableDefault final Pageable pageable) {
        Long clubMemberId = principal.getClubMemberId();
        Long clubId = principal.getClubId();

        SliceResponse response = postService.getMyPostsByClubId(clubMemberId, clubId, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping(value = "/posts/{postId}")
    public ResponseEntity<Response> getPost(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable final Long postId) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = postService.getPostById(clubMemberId, postId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping(value = "/posts/{postId}")
    public ResponseEntity deletePost(@AuthenticationPrincipal PrincipalDetails principal,
                                     @PathVariable final Long postId) {
        Long clubMemberId = principal.getClubMemberId();

        postService.deletePostById(clubMemberId, postId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/posts/{postId}")
    public ResponseEntity<Response> updatePost(@AuthenticationPrincipal PrincipalDetails principal,
                                               @PathVariable final Long postId,
                                               @RequestBody final Update updateDto) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = postService.updatePost(clubMemberId, updateDto);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping(value = "/posts/{postId}/like")
    public ResponseEntity<PostMemberLikeDto.Response> likePost(@AuthenticationPrincipal PrincipalDetails principal,
                                                               @PathVariable final Long postId) {
        Long clubMemberId = principal.getClubMemberId();

        PostMemberLikeDto.Response response = postService.likePost(clubMemberId, postId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping(value = "/posts/{postId}/fix")
    public ResponseEntity<Response> fixPost(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable final Long postId) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = postService.toggleIsFixed(clubMemberId, postId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }
}

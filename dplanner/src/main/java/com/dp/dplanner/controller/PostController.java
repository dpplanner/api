package com.dp.dplanner.controller;

import com.dp.dplanner.dto.CommonResponse;
import com.dp.dplanner.dto.PostMemberLikeDto;
import com.dp.dplanner.security.PrincipalDetails;
import com.dp.dplanner.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.dp.dplanner.dto.PostDto.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<Response> createPost(@AuthenticationPrincipal PrincipalDetails principal,
                                               @RequestPart @Valid final Create create,
                                               @RequestPart(required = false) final List<MultipartFile> files) {

        Long clubMemberId = principal.getClubMemberId();
        create.setFiles(files);

        Response response = postService.createPost(clubMemberId, create);

        return CommonResponse.createSuccess(response);
    }

    @GetMapping(value = "")
    public CommonResponse<SliceResponse> getClubPosts(@AuthenticationPrincipal PrincipalDetails principal,
                                              @PageableDefault final Pageable pageable) {

        Long clubMemberId = principal.getClubMemberId();
        Long clubId = principal.getClubId();

        SliceResponse response = postService.getPostsByClubId(clubMemberId, clubId, pageable);

        return CommonResponse.createSuccess(response);

    }


    @GetMapping(value = "/members/{memberId}")
    public CommonResponse<SliceResponse> getMyPosts(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable final Long memberId,
                                            @PageableDefault final Pageable pageable) {
        Long clubMemberId = principal.getClubMemberId();
        Long clubId = principal.getClubId();

        SliceResponse response = postService.getMyPostsByClubId(clubMemberId, clubId, pageable);

        return CommonResponse.createSuccess(response);

    }

    @GetMapping(value = "/{postId}")
    public CommonResponse<Response> getPost(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable final Long postId) {
        Long clubMemberId = principal.getClubMemberId();

        Response response = postService.getPostById(clubMemberId, postId);

        return CommonResponse.createSuccess(response);

    }

    @DeleteMapping(value = "/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public CommonResponse deletePost(@AuthenticationPrincipal PrincipalDetails principal,
                                     @PathVariable final Long postId) {
        Long clubMemberId = principal.getClubMemberId();

        postService.deletePostById(clubMemberId, postId);

        return CommonResponse.createSuccessWithNoContent();
    }

    @PostMapping(value = "/{postId}") // Post? Put? 해당 동작은 멱등하지 않는데.. 그럼 POST?
    public CommonResponse<Response> updatePost(@AuthenticationPrincipal PrincipalDetails principal,
                                               @PathVariable final Long postId,
                                               @Valid @RequestPart final Update update,
                                               @RequestPart(required = false) final List<MultipartFile> files) {
        Long clubMemberId = principal.getClubMemberId();
        update.setFiles(files);

        Response response = postService.updatePost(clubMemberId, update);

        return CommonResponse.createSuccess(response);

    }

    @PutMapping(value = "/{postId}/like")
    public CommonResponse<PostMemberLikeDto.Response> likePost(@AuthenticationPrincipal PrincipalDetails principal,
                                                               @PathVariable final Long postId) {
        Long clubMemberId = principal.getClubMemberId();

        PostMemberLikeDto.Response response = postService.likePost(clubMemberId, postId);

        return CommonResponse.createSuccess(response);

    }

    @PutMapping(value = "/{postId}/fix")
    public CommonResponse<Response> fixPost(@AuthenticationPrincipal PrincipalDetails principal,
                                            @PathVariable final Long postId) {

        Long clubMemberId = principal.getClubMemberId();

        Response response = postService.toggleIsFixed(clubMemberId, postId);

        return CommonResponse.createSuccess(response);

    }
}

package com.dp.dplanner.adapter.controller;

import com.dp.dplanner.adapter.dto.CommonResponse;
import com.dp.dplanner.adapter.dto.PostDto;
import com.dp.dplanner.adapter.dto.PostMemberLikeDto;
import com.dp.dplanner.adapter.exception.ApiException;
import com.dp.dplanner.exception.ErrorResult;
import com.dp.dplanner.config.security.PrincipalDetails;
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

import static com.dp.dplanner.adapter.dto.PostDto.*;

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
        if (!principal.getClubId().equals(create.getClubId())) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubMemberId = principal.getClubMemberId();
        create.setFiles(files);
        Response response = postService.createPost(clubMemberId, create);

        return CommonResponse.createSuccess(response);
    }

    @GetMapping(value = "/clubs/{clubId}")
    public CommonResponse<SliceResponse> getClubPosts(@AuthenticationPrincipal PrincipalDetails principal,
                                                      @PathVariable final Long clubId,
                                                      @PageableDefault final Pageable pageable) {

        Long clubMemberId = principal.getClubMemberId();
        if (!principal.getClubId().equals(clubId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }

        SliceResponse response = postService.getPostsByClubId(clubMemberId, clubId, pageable);

        return CommonResponse.createSuccess(response);

    }


    @GetMapping(value = "/clubMembers/{clubMemberId}")
    public CommonResponse<SliceResponse> getMyPosts(@AuthenticationPrincipal PrincipalDetails principal,
                                                    @PathVariable final Long clubMemberId,
                                                    @PageableDefault final Pageable pageable) {
        if (!principal.getClubMemberId().equals(clubMemberId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubId = principal.getClubId();

        SliceResponse response = postService.getMyPostsByClubId(clubMemberId, clubId, pageable);

        return CommonResponse.createSuccess(response);

    }

    @GetMapping(value = "/clubMembers/{clubMemberId}/like")
    public CommonResponse<SliceResponse> getLikedPosts(@AuthenticationPrincipal PrincipalDetails principal,
                                                       @PathVariable final Long clubMemberId,
                                                       @PageableDefault final Pageable pageable) {
        if (!principal.getClubMemberId().equals(clubMemberId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubId = principal.getClubId();

        SliceResponse response = postService.getLikePosts(clubMemberId, clubId, pageable);

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

    @PutMapping(value = "/{postId}")
    public CommonResponse<Response> updatePost(@AuthenticationPrincipal PrincipalDetails principal,
                                               @Valid @RequestPart final Update update,
                                               @PathVariable("postId") final Long postId,
                                               @RequestPart(required = false) final List<MultipartFile> files) {
        Long clubMemberId = principal.getClubMemberId();
        update.setId(postId);
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

    @GetMapping(value = "/clubMembers/{clubMemberId}/commented")
    public CommonResponse<PostDto.SliceResponse> getMyCommentedPosts(@AuthenticationPrincipal PrincipalDetails principal,
                                                                     @PathVariable final Long clubMemberId,
                                                                     @PageableDefault final Pageable pageable) {
        if (!principal.getClubMemberId().equals(clubMemberId)) {
            throw new ApiException(ErrorResult.REQUEST_IS_INVALID);
        }
        Long clubId = principal.getClubId();
        PostDto.SliceResponse response = postService.getCommentedPosts(clubMemberId, clubId, pageable);
        return CommonResponse.createSuccess(response);
    }

    @PostMapping(value = "/{postId}/report")
    public CommonResponse reportPost(@AuthenticationPrincipal PrincipalDetails principal,
                                     @RequestBody @Valid Report report,
                                     @PathVariable final Long postId) {
        report.setPostId(postId);
        report.setClubMemberId(principal.getClubMemberId());
        postService.reportPost(report);

        return CommonResponse.createSuccessWithNoContent();
    }

    @PostMapping(value = "/{postId}/block")
    public CommonResponse blockPost(@AuthenticationPrincipal PrincipalDetails principal,
                                    @PathVariable final Long postId) {
        Block block = Block.builder()
                .postId(postId)
                .clubMemberId(principal.getClubMemberId())
                .build();
        postService.toggleBlockPost(block);

        return CommonResponse.createSuccessWithNoContent();
    }
}

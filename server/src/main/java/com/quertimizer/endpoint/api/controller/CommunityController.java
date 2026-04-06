package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.CommunityCommentCreateReq;
import com.quertimizer.endpoint.api.dto.request.CommunityPostSaveReq;
import com.quertimizer.endpoint.api.dto.response.CommunityCommentRes;
import com.quertimizer.endpoint.api.dto.response.CommunityPostDetailRes;
import com.quertimizer.endpoint.api.dto.response.CommunityPostPageRes;
import com.quertimizer.endpoint.api.dto.response.CommunityReactionRes;
import com.quertimizer.endpoint.api.dto.response.CommunityTagSuggestionRes;
import com.quertimizer.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/community/posts")
    public ResponseEntity<CommunityPostPageRes> getPosts(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(required = false) String search,
                                                         @RequestParam(required = false) String tag,
                                                         @RequestParam(defaultValue = "latest") String sortKey) {

        // 게시글 목록 검색, 태그 필터, 정렬, 페이징 조회
        return ResponseEntity.ok(communityService.getPosts(page, search, tag, sortKey));
    }

    @GetMapping("/community/posts/{postId}")
    public ResponseEntity<CommunityPostDetailRes> getPostDetail(@PathVariable String postId,
                                                                Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 게시글 상세 조회
        return ResponseEntity.of(communityService.getPostDetail(postId, currentUserId));
    }

    @PostMapping("/community/posts")
    public ResponseEntity<Void> createPost(@Valid @RequestBody CommunityPostSaveReq request,
                                           Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 게시글 작성
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String createdPostId = communityService.createPost(currentUserId, request);
        return ResponseEntity.created(URI.create("/community/posts/" + createdPostId)).build();
    }

    @PutMapping("/community/posts/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable String postId,
                                           @Valid @RequestBody CommunityPostSaveReq request,
                                           Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 본인 게시글 수정
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (communityService.updatePost(postId, currentUserId, request).isPresent()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable String postId, Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 본인 게시글 삭제
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return communityService.deletePost(postId, currentUserId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/community/posts/{postId}/likes")
    public ResponseEntity<CommunityReactionRes> togglePostLike(@PathVariable String postId,
                                                               Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 게시글 좋아요 토글
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(communityService.togglePostLike(postId, currentUserId));
    }

    @PostMapping("/community/posts/{postId}/comments")
    public ResponseEntity<CommunityCommentRes> addComment(@PathVariable String postId,
                                                          @Valid @RequestBody CommunityCommentCreateReq request,
                                                          Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 댓글, 대댓글 작성
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(communityService.addComment(postId, currentUserId, request));
    }

    @PostMapping("/community/comments/{commentId}/likes")
    public ResponseEntity<CommunityReactionRes> toggleCommentLike(@PathVariable Long commentId,
                                                                  Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 댓글 좋아요 토글
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(communityService.toggleCommentLike(commentId, currentUserId));
    }

    @GetMapping("/community/tags/suggestions")
    public ResponseEntity<List<CommunityTagSuggestionRes>> getTagSuggestions(@RequestParam(required = false) String query) {

        // 게시글 작성용 태그 자동완성 조회
        return ResponseEntity.ok(communityService.getTagSuggestions(query));
    }

    private String resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

}

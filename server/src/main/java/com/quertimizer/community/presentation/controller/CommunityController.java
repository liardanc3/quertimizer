package com.quertimizer.community.presentation.controller;

import com.quertimizer.community.application.usecase.AddCommunityComment;
import com.quertimizer.community.application.usecase.CreateCommunityPost;
import com.quertimizer.community.application.usecase.DeleteCommunityPost;
import com.quertimizer.community.application.usecase.GetCommunityPostDetail;
import com.quertimizer.community.application.usecase.GetCommunityPosts;
import com.quertimizer.community.application.usecase.GetCommunityTagSuggestions;
import com.quertimizer.community.application.usecase.GetCommunityImage;
import com.quertimizer.community.application.usecase.ToggleCommunityCommentLike;
import com.quertimizer.community.application.usecase.ToggleCommunityPostLike;
import com.quertimizer.community.application.usecase.UpdateCommunityPost;
import com.quertimizer.community.application.usecase.UploadCommunityImage;
import com.quertimizer.community.presentation.dto.request.CommunityCommentCreateReq;
import com.quertimizer.community.presentation.dto.request.CommunityPostSaveReq;
import com.quertimizer.community.presentation.dto.response.CommunityCommentRes;
import com.quertimizer.community.presentation.dto.response.CommunityImageUploadRes;
import com.quertimizer.community.presentation.dto.response.CommunityPostDetailRes;
import com.quertimizer.community.presentation.dto.response.CommunityPostPageRes;
import com.quertimizer.community.presentation.dto.response.CommunityReactionRes;
import com.quertimizer.community.presentation.dto.response.CommunityTagSuggestionRes;
import com.quertimizer.community.presentation.support.CommunitySupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommunityController {

    private final GetCommunityPosts getCommunityPosts;
    private final GetCommunityPostDetail getCommunityPostDetail;
    private final CreateCommunityPost createCommunityPost;
    private final UpdateCommunityPost updateCommunityPost;
    private final DeleteCommunityPost deleteCommunityPost;
    private final ToggleCommunityPostLike toggleCommunityPostLike;
    private final AddCommunityComment addCommunityComment;
    private final ToggleCommunityCommentLike toggleCommunityCommentLike;
    private final GetCommunityTagSuggestions getCommunityTagSuggestions;
    private final UploadCommunityImage uploadCommunityImage;
    private final GetCommunityImage getCommunityImage;

    private final CommunitySupport communitySupport;

    @GetMapping("/community/posts")
    public ResponseEntity<CommunityPostPageRes> getPosts(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(required = false) String search,
                                                         @RequestParam(required = false) String tag,
                                                         @RequestParam(defaultValue = "all") String category,
                                                         @RequestParam(defaultValue = "default") String sortKey) {
        // 게시글 목록 검색, 태그 필터, 정렬, 페이징 조회
        return ResponseEntity.ok(CommunityPostPageRes.from(
                getCommunityPosts.execute(page, search, tag, category, sortKey)
        ));
    }

    @GetMapping("/community/posts/{postId}")
    public ResponseEntity<CommunityPostDetailRes> getPostDetail(@PathVariable Long postId,
                                                                Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        // 게시글 상세 조회
        return ResponseEntity.of(getCommunityPostDetail.execute(postId, currentHandle).map(CommunityPostDetailRes::from));
    }

    @PostMapping("/community/posts")
    public ResponseEntity<Void> createPost(@Valid @RequestBody CommunityPostSaveReq request,
                                           Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        // 게시글 작성
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long createdPostId = createCommunityPost.execute(currentHandle, request.toCommunityPostInput());
        return ResponseEntity.created(communitySupport.buildPostLocation(createdPostId)).build();
    }

    @PostMapping("/community/images")
    public ResponseEntity<CommunityImageUploadRes> uploadImage(@RequestParam("file") MultipartFile file,
                                                               Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        // 커뮤니티 글쓰기 이미지를 업로드
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(CommunityImageUploadRes.from(uploadCommunityImage.execute(file)));
    }

    @GetMapping("/community/images/{imageId}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId) {
        // 커뮤니티 이미지를 조회
        return getCommunityImage.execute(imageId)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        .body(image.getResource()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/community/posts/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @Valid @RequestBody CommunityPostSaveReq request,
                                           Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        // 본인 게시글 수정
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (updateCommunityPost.execute(postId, currentHandle, request.toCommunityPostInput()).isPresent()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        // 본인 게시글 삭제
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return deleteCommunityPost.execute(postId, currentHandle)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/community/posts/{postId}/likes")
    public ResponseEntity<CommunityReactionRes> togglePostLike(@PathVariable Long postId,
                                                               Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        // 게시글 좋아요 토글
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(toggleCommunityPostLike.execute(postId, currentHandle).map(CommunityReactionRes::from));
    }

    @PostMapping("/community/posts/{postId}/comments")
    public ResponseEntity<CommunityCommentRes> addComment(@PathVariable Long postId,
                                                          @Valid @RequestBody CommunityCommentCreateReq request,
                                                          Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        // 댓글, 대댓글 작성
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(addCommunityComment.execute(postId, currentHandle, request.toCommunityCommentInput())
                .map(CommunityCommentRes::from));
    }

    @PostMapping("/community/comments/{commentId}/likes")
    public ResponseEntity<CommunityReactionRes> toggleCommentLike(@PathVariable Long commentId,
                                                                  Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        // 댓글 좋아요 토글
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(toggleCommunityCommentLike.execute(commentId, currentHandle).map(CommunityReactionRes::from));
    }

    @GetMapping("/community/tags/suggestions")
    public ResponseEntity<List<CommunityTagSuggestionRes>> getTagSuggestions(@RequestParam(required = false) String query) {
        // 게시글 작성용 태그 자동완성 조회
        return ResponseEntity.ok(getCommunityTagSuggestions.execute(query).stream()
                .map(CommunityTagSuggestionRes::from)
                .toList());
    }
}

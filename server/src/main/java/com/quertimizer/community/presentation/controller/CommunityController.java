package com.quertimizer.community.presentation.controller;

import com.quertimizer.community.application.input.AddCommunityCommentInput;
import com.quertimizer.community.application.input.CommunityPostDetailInput;
import com.quertimizer.community.application.input.CreateCommunityPostInput;
import com.quertimizer.community.application.input.DeleteCommunityPostInput;
import com.quertimizer.community.application.input.ToggleCommunityCommentLikeInput;
import com.quertimizer.community.application.input.ToggleCommunityPostLikeInput;
import com.quertimizer.community.application.input.UpdateCommunityPostInput;
import com.quertimizer.community.application.usecase.AddCommunityComment;
import com.quertimizer.community.application.usecase.CreateCommunityPost;
import com.quertimizer.community.application.usecase.DeleteCommunityPost;
import com.quertimizer.community.application.usecase.GetCommunityImage;
import com.quertimizer.community.application.usecase.GetCommunityPostDetail;
import com.quertimizer.community.application.usecase.GetCommunityPosts;
import com.quertimizer.community.application.usecase.GetCommunityTagSuggestions;
import com.quertimizer.community.application.usecase.ToggleCommunityCommentLike;
import com.quertimizer.community.application.usecase.ToggleCommunityPostLike;
import com.quertimizer.community.application.usecase.UpdateCommunityPost;
import com.quertimizer.community.application.usecase.UploadCommunityImage;
import com.quertimizer.community.presentation.dto.request.CommunityCommentCreateReq;
import com.quertimizer.community.presentation.dto.request.CommunityPostSaveReq;
import com.quertimizer.community.presentation.dto.request.CommunityPostSearchReq;
import com.quertimizer.community.presentation.dto.request.CommunityTagSuggestionReq;
import com.quertimizer.community.presentation.dto.response.CommunityCommentRes;
import com.quertimizer.community.presentation.dto.response.CommunityImageUploadRes;
import com.quertimizer.community.presentation.dto.response.CommunityPostDetailRes;
import com.quertimizer.community.presentation.dto.response.CommunityPostPageRes;
import com.quertimizer.community.presentation.dto.response.CommunityReactionRes;
import com.quertimizer.community.presentation.dto.response.CommunityTagSuggestionRes;
import com.quertimizer.community.presentation.support.CommunitySupport;
import com.quertimizer.global.support.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ClientIpResolver clientIpResolver;

    /**
     * 커뮤니티 게시글 목록을 검색, 필터, 정렬 조건에 맞게 반환한다.
     *
     * <ol>
     *   <li>게시글 검색 입력 생성
     *   <li>게시글 목록 페이지 응답 생성
     * </ol>
     *
     * @param page 요청 페이지 번호
     * @param search 검색어
     * @param tag 태그 필터
     * @param category 카테고리 필터
     * @param sortKey 정렬 기준
     */
    @GetMapping("/community/posts")
    public ResponseEntity<CommunityPostPageRes> getPosts(@Valid CommunityPostSearchReq request) {
        return ResponseEntity.ok(CommunityPostPageRes.from(
                getCommunityPosts.execute(request.toInput())
        ));
    }

    /**
     * 게시글 상세를 현재 사용자 반응 정보와 함께 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>게시글 상세 응답 생성
     * </ol>
     *
     * @param postId 조회할 게시글 번호
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/community/posts/{postId}")
    public ResponseEntity<CommunityPostDetailRes> getPostDetail(@PathVariable Long postId,
                                                                Authentication authentication,
                                                                HttpServletRequest httpRequest) {
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);
        String viewerKey = currentHandle != null
                ? "user:" + currentHandle
                : "ip:" + clientIpResolver.resolve(httpRequest);

        CommunityPostDetailInput input = new CommunityPostDetailInput(postId, currentHandle, viewerKey);
        return ResponseEntity.of(getCommunityPostDetail.execute(input).map(CommunityPostDetailRes::from));
    }

    /**
     * 현재 사용자의 커뮤니티 게시글을 생성하고 Location을 반환한다.
     *
     * <ol>
     *   <li>게시글 생성 후 Location 응답 생성
     * </ol>
     *
     * @param request 생성할 게시글 요청
     * @param authentication 현재 요청의 인증 정보
     */
    @PostMapping("/community/posts")
    public ResponseEntity<Void> createPost(@Valid @RequestBody CommunityPostSaveReq request,
                                           Authentication authentication) {
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        CreateCommunityPostInput input = new CreateCommunityPostInput(currentHandle, request.toCommunityPostInput());
        Long createdPostId = createCommunityPost.execute(input);
        return ResponseEntity.created(communitySupport.buildPostLocation(createdPostId)).build();
    }

    /**
     * 커뮤니티 게시글 작성용 이미지를 업로드한다.
     *
     * <ol>
     *   <li>이미지 업로드 응답 생성
     * </ol>
     *
     * @param file 업로드할 이미지 파일
     */
    @PostMapping("/community/images")
    public ResponseEntity<CommunityImageUploadRes> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(CommunityImageUploadRes.from(uploadCommunityImage.execute(file)));
    }

    /**
     * 커뮤니티 이미지를 리소스 응답으로 반환한다.
     *
     * @param imageId 조회할 커뮤니티 이미지 ID
     */
    @GetMapping("/community/images/{imageId}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId) {
        return getCommunityImage.execute(imageId)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getContentType()))
                        .body(image.getResource()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 현재 사용자의 커뮤니티 게시글을 수정한다.
     *
     * <ol>
     *   <li>게시글 수정 결과 응답 생성
     * </ol>
     *
     * @param postId 수정할 게시글 번호
     * @param request 저장할 게시글 요청
     * @param authentication 현재 요청의 인증 정보
     */
    @PutMapping("/community/posts/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @Valid @RequestBody CommunityPostSaveReq request,
                                           Authentication authentication) {
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        UpdateCommunityPostInput input = new UpdateCommunityPostInput(postId, currentHandle, request.toCommunityPostInput());
        if (updateCommunityPost.execute(input).isPresent()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * 현재 사용자의 커뮤니티 게시글을 삭제한다.
     *
     * <ol>
     *   <li>게시글 삭제 결과 응답 생성
     * </ol>
     *
     * @param postId 삭제할 게시글 번호
     * @param authentication 현재 요청의 인증 정보
     */
    @DeleteMapping("/community/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, Authentication authentication) {
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        return deleteCommunityPost.execute(new DeleteCommunityPostInput(postId, currentHandle))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * 현재 사용자의 게시글 좋아요 상태를 토글한다.
     *
     * <ol>
     *   <li>게시글 좋아요 결과 응답 생성
     * </ol>
     *
     * @param postId 좋아요를 토글할 게시글 번호
     * @param authentication 현재 요청의 인증 정보
     */
    @PostMapping("/community/posts/{postId}/likes")
    public ResponseEntity<CommunityReactionRes> togglePostLike(@PathVariable Long postId,
                                                               Authentication authentication) {
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        ToggleCommunityPostLikeInput input = new ToggleCommunityPostLikeInput(postId, currentHandle);
        return ResponseEntity.of(toggleCommunityPostLike.execute(input).map(CommunityReactionRes::from));
    }

    /**
     * 현재 사용자의 댓글 또는 대댓글을 생성한다.
     *
     * <ol>
     *   <li>댓글 생성 결과 응답 생성
     * </ol>
     *
     * @param postId 댓글을 작성할 게시글 번호
     * @param request 생성할 댓글 요청
     * @param authentication 현재 요청의 인증 정보
     */
    @PostMapping("/community/posts/{postId}/comments")
    public ResponseEntity<CommunityCommentRes> addComment(@PathVariable Long postId,
                                                          @Valid @RequestBody CommunityCommentCreateReq request,
                                                          Authentication authentication) {
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        AddCommunityCommentInput input = new AddCommunityCommentInput(postId, currentHandle, request.toCommunityCommentInput());
        return ResponseEntity.of(addCommunityComment.execute(input)
                .map(CommunityCommentRes::from));
    }

    /**
     * 현재 사용자의 댓글 좋아요 상태를 토글한다.
     *
     * <ol>
     *   <li>댓글 좋아요 결과 응답 생성
     * </ol>
     *
     * @param commentId 좋아요를 토글할 댓글 번호
     * @param authentication 현재 요청의 인증 정보
     */
    @PostMapping("/community/comments/{commentId}/likes")
    public ResponseEntity<CommunityReactionRes> toggleCommentLike(@PathVariable Long commentId,
                                                                  Authentication authentication) {
        String currentHandle = communitySupport.resolveCurrentHandle(authentication);

        ToggleCommunityCommentLikeInput input = new ToggleCommunityCommentLikeInput(commentId, currentHandle);
        return ResponseEntity.of(toggleCommunityCommentLike.execute(input).map(CommunityReactionRes::from));
    }

    /**
     * 게시글 작성용 태그 자동완성 후보를 반환한다.
     *
     * @param query 태그 검색어
     */
    @GetMapping("/community/tags/suggestions")
    public ResponseEntity<List<CommunityTagSuggestionRes>> getTagSuggestions(@Valid CommunityTagSuggestionReq request) {
        return ResponseEntity.ok(getCommunityTagSuggestions.execute(request.getQuery()).stream()
                .map(CommunityTagSuggestionRes::from)
                .toList());
    }
}

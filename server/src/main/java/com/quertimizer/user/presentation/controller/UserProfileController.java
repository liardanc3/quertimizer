package com.quertimizer.user.presentation.controller;

import com.quertimizer.user.application.input.UserProfileAccessInput;
import com.quertimizer.user.application.input.UserProfileActivityPageInput;
import com.quertimizer.user.application.input.UserProfileUpdateCommandInput;
import com.quertimizer.user.application.usecase.GetUserProfileCommunityActivities;
import com.quertimizer.user.application.usecase.GetUserProfileCommunityComments;
import com.quertimizer.user.application.usecase.GetUserProfileCommunityPosts;
import com.quertimizer.user.application.usecase.GetUserProfileLikedComments;
import com.quertimizer.user.application.usecase.GetUserProfileLikedPosts;
import com.quertimizer.user.application.usecase.GetUserProfileSolvedProblems;
import com.quertimizer.user.application.usecase.GetUserProfileSolvedRecords;
import com.quertimizer.user.application.usecase.GetUserProfileSubmissionSummary;
import com.quertimizer.user.application.usecase.GetUserProfileSummary;
import com.quertimizer.user.application.usecase.UpdateUserProfile;
import com.quertimizer.user.presentation.dto.request.UserProfileUpdateReq;
import com.quertimizer.user.presentation.dto.response.UserProfileCommunityActivitiesRes;
import com.quertimizer.user.presentation.dto.response.UserProfileCommunityCommentsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileCommunityPostsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSolvedProblemsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSolvedRecordsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSubmissionSummaryRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSummaryRes;
import com.quertimizer.user.presentation.support.UserProfileSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final GetUserProfileSummary getUserProfileSummary;
    private final GetUserProfileSolvedProblems getUserProfileSolvedProblems;
    private final GetUserProfileSolvedRecords getUserProfileSolvedRecords;
    private final GetUserProfileSubmissionSummary getUserProfileSubmissionSummary;
    private final GetUserProfileCommunityActivities getUserProfileCommunityActivities;
    private final GetUserProfileCommunityPosts getUserProfileCommunityPosts;
    private final GetUserProfileLikedPosts getUserProfileLikedPosts;
    private final GetUserProfileCommunityComments getUserProfileCommunityComments;
    private final GetUserProfileLikedComments getUserProfileLikedComments;
    private final UpdateUserProfile updateUserProfile;

    private final UserProfileSupport userProfileSupport;

    /**
     * 현재 사용자의 프로필 기본 정보를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>프로필 기본 정보 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me")
    public ResponseEntity<UserProfileSummaryRes> getMyProfile(Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileSummary.execute(new UserProfileAccessInput(currentHandle, currentHandle))
                .map(UserProfileSummaryRes::from));
    }

    /**
     * 현재 사용자의 해결한 문제 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>해결한 문제 목록 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/solved-problems")
    public ResponseEntity<UserProfileSolvedProblemsRes> getMySolvedProblems(Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileSolvedProblems.execute(new UserProfileAccessInput(currentHandle, currentHandle))
                .map(UserProfileSolvedProblemsRes::from));
    }

    /**
     * 현재 사용자의 해결 기록 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>해결 기록 목록 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/solved-records")
    public ResponseEntity<UserProfileSolvedRecordsRes> getMySolvedRecords(Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileSolvedRecords.execute(new UserProfileAccessInput(currentHandle, currentHandle))
                .map(UserProfileSolvedRecordsRes::from));
    }

    /**
     * 현재 사용자의 제출 요약 정보를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>제출 요약 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/submission-summary")
    public ResponseEntity<UserProfileSubmissionSummaryRes> getMySubmissionSummary(Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileSubmissionSummary.execute(new UserProfileAccessInput(currentHandle, currentHandle))
                .map(UserProfileSubmissionSummaryRes::from));
    }

    /**
     * 현재 사용자가 작성한 커뮤니티 게시글 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>작성 게시글 목록 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/community/posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getMyCommunityPosts(Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileCommunityPosts.execute(new UserProfileAccessInput(currentHandle, currentHandle))
                .map(UserProfileCommunityPostsRes::from));
    }

    /**
     * 현재 사용자가 좋아요한 커뮤니티 게시글 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>좋아요 게시글 목록 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/community/liked-posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getMyLikedPosts(Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileLikedPosts.execute(new UserProfileAccessInput(currentHandle, currentHandle))
                .map(UserProfileCommunityPostsRes::from));
    }

    /**
     * 현재 사용자가 작성한 커뮤니티 댓글 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>작성 댓글 목록 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/community/comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getMyCommunityComments(Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileCommunityComments.execute(new UserProfileAccessInput(currentHandle, currentHandle))
                .map(UserProfileCommunityCommentsRes::from));
    }

    /**
     * 현재 사용자가 좋아요한 커뮤니티 댓글 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>좋아요 댓글 목록 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/community/liked-comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getMyLikedComments(Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileLikedComments.execute(new UserProfileAccessInput(currentHandle, currentHandle))
                .map(UserProfileCommunityCommentsRes::from));
    }

    /**
     * 현재 사용자의 커뮤니티 활동 페이지를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>커뮤니티 활동 페이지 응답 생성
     * </ol>
     *
     * @param page 요청 페이지 번호
     * @param pageSize 요청 페이지 크기
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/community/activities")
    public ResponseEntity<UserProfileCommunityActivitiesRes> getMyCommunityActivities(@RequestParam(defaultValue = "1") int page,
                                                                                      @RequestParam(required = false) Integer pageSize,
                                                                                      Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserProfileActivityPageInput input = new UserProfileActivityPageInput(currentHandle, currentHandle, page, pageSize);
        return ResponseEntity.of(getUserProfileCommunityActivities.execute(input)
                .map(UserProfileCommunityActivitiesRes::from));
    }

    /**
     * 현재 사용자의 프로필 정보를 수정하고 수정된 기본 정보를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>프로필 수정 결과 응답 생성
     * </ol>
     *
     * @param request 저장할 프로필 요청
     * @param authentication 현재 요청의 인증 정보
     */
    @PutMapping("/profile/me")
    public ResponseEntity<UserProfileSummaryRes> updateMyProfile(@Valid @RequestBody UserProfileUpdateReq request,
                                                                 Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(updateUserProfile.execute(new UserProfileUpdateCommandInput(currentHandle, request.toUserProfileUpdateInput()))
                .map(UserProfileSummaryRes::from));
    }

    /**
     * 공개 프로필 기본 정보를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 프로필 기본 정보 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}")
    public ResponseEntity<UserProfileSummaryRes> getProfile(@PathVariable String handle,
                                                            Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.of(getUserProfileSummary.execute(new UserProfileAccessInput(handle, currentHandle))
                .map(UserProfileSummaryRes::from));
    }

    /**
     * 공개 프로필의 해결한 문제 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 해결한 문제 목록 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}/solved-problems")
    public ResponseEntity<UserProfileSolvedProblemsRes> getSolvedProblems(@PathVariable String handle,
                                                                          Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.of(getUserProfileSolvedProblems.execute(new UserProfileAccessInput(handle, currentHandle))
                .map(UserProfileSolvedProblemsRes::from));
    }

    /**
     * 공개 프로필의 해결 기록 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 해결 기록 목록 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}/solved-records")
    public ResponseEntity<UserProfileSolvedRecordsRes> getSolvedRecords(@PathVariable String handle,
                                                                        Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.of(getUserProfileSolvedRecords.execute(new UserProfileAccessInput(handle, currentHandle))
                .map(UserProfileSolvedRecordsRes::from));
    }

    /**
     * 공개 프로필의 제출 요약 정보를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 제출 요약 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}/submission-summary")
    public ResponseEntity<UserProfileSubmissionSummaryRes> getSubmissionSummary(@PathVariable String handle,
                                                                                Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.of(getUserProfileSubmissionSummary.execute(new UserProfileAccessInput(handle, currentHandle))
                .map(UserProfileSubmissionSummaryRes::from));
    }

    /**
     * 공개 프로필의 작성 게시글 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 작성 게시글 목록 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}/community/posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getCommunityPosts(@PathVariable String handle,
                                                                          Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.of(getUserProfileCommunityPosts.execute(new UserProfileAccessInput(handle, currentHandle))
                .map(UserProfileCommunityPostsRes::from));
    }

    /**
     * 공개 프로필의 좋아요한 게시글 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 좋아요 게시글 목록 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}/community/liked-posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getLikedPosts(@PathVariable String handle,
                                                                      Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.of(getUserProfileLikedPosts.execute(new UserProfileAccessInput(handle, currentHandle))
                .map(UserProfileCommunityPostsRes::from));
    }

    /**
     * 공개 프로필의 작성 댓글 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 작성 댓글 목록 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}/community/comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getCommunityComments(@PathVariable String handle,
                                                                                Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.of(getUserProfileCommunityComments.execute(new UserProfileAccessInput(handle, currentHandle))
                .map(UserProfileCommunityCommentsRes::from));
    }

    /**
     * 공개 프로필의 좋아요한 댓글 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 좋아요 댓글 목록 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}/community/liked-comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getLikedComments(@PathVariable String handle,
                                                                            Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        return ResponseEntity.of(getUserProfileLikedComments.execute(new UserProfileAccessInput(handle, currentHandle))
                .map(UserProfileCommunityCommentsRes::from));
    }

    /**
     * 공개 프로필의 커뮤니티 활동 페이지를 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 handle 확인
     *   <li>공개 커뮤니티 활동 페이지 응답 생성
     * </ol>
     *
     * @param handle 조회할 프로필 handle
     * @param page 요청 페이지 번호
     * @param pageSize 요청 페이지 크기
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profiles/{handle}/community/activities")
    public ResponseEntity<UserProfileCommunityActivitiesRes> getCommunityActivities(@PathVariable String handle,
                                                                                   @RequestParam(defaultValue = "1") int page,
                                                                                   @RequestParam(required = false) Integer pageSize,
                                                                                   Authentication authentication) {
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        UserProfileActivityPageInput input = new UserProfileActivityPageInput(handle, currentHandle, page, pageSize);
        return ResponseEntity.of(getUserProfileCommunityActivities.execute(input)
                .map(UserProfileCommunityActivitiesRes::from));
    }
}

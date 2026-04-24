package com.quertimizer.user.presentation.controller;

import com.quertimizer.user.presentation.dto.request.UserProfileUpdateReq;
import com.quertimizer.user.presentation.dto.response.UserProfileCommunityCommentsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileCommunityPostsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSolvedProblemsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSolvedRecordsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSummaryRes;
import com.quertimizer.user.application.usecase.GetUserProfileCommunityComments;
import com.quertimizer.user.application.usecase.GetUserProfileCommunityPosts;
import com.quertimizer.user.application.usecase.GetUserProfileLikedComments;
import com.quertimizer.user.application.usecase.GetUserProfileLikedPosts;
import com.quertimizer.user.application.usecase.GetUserProfileSolvedProblems;
import com.quertimizer.user.application.usecase.GetUserProfileSolvedRecords;
import com.quertimizer.user.application.usecase.GetUserProfileSummary;
import com.quertimizer.user.application.usecase.UpdateUserProfile;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final GetUserProfileSummary getUserProfileSummary;
    private final GetUserProfileSolvedProblems getUserProfileSolvedProblems;
    private final GetUserProfileSolvedRecords getUserProfileSolvedRecords;
    private final GetUserProfileCommunityPosts getUserProfileCommunityPosts;
    private final GetUserProfileLikedPosts getUserProfileLikedPosts;
    private final GetUserProfileCommunityComments getUserProfileCommunityComments;
    private final GetUserProfileLikedComments getUserProfileLikedComments;
    private final UpdateUserProfile updateUserProfile;

    private final UserProfileSupport userProfileSupport;

    @GetMapping("/profile/me")
    public ResponseEntity<UserProfileSummaryRes> getMyProfile(Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 내 프로필 기본 정보 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileSummary.execute(currentHandle, currentHandle).map(UserProfileSummaryRes::from));
    }

    @GetMapping("/profile/me/solved-problems")
    public ResponseEntity<UserProfileSolvedProblemsRes> getMySolvedProblems(Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 내 해결한 문제 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileSolvedProblems.execute(currentHandle, currentHandle).map(UserProfileSolvedProblemsRes::from));
    }

    @GetMapping("/profile/me/solved-records")
    public ResponseEntity<UserProfileSolvedRecordsRes> getMySolvedRecords(Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 내 해결 기록 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileSolvedRecords.execute(currentHandle, currentHandle).map(UserProfileSolvedRecordsRes::from));
    }

    @GetMapping("/profile/me/community/posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getMyCommunityPosts(Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 내가 작성한 게시글 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileCommunityPosts.execute(currentHandle).map(UserProfileCommunityPostsRes::from));
    }

    @GetMapping("/profile/me/community/liked-posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getMyLikedPosts(Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 내가 좋아요한 게시글 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileLikedPosts.execute(currentHandle).map(UserProfileCommunityPostsRes::from));
    }

    @GetMapping("/profile/me/community/comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getMyCommunityComments(Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 내가 작성한 댓글 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileCommunityComments.execute(currentHandle).map(UserProfileCommunityCommentsRes::from));
    }

    @GetMapping("/profile/me/community/liked-comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getMyLikedComments(Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 내가 좋아요한 댓글 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(getUserProfileLikedComments.execute(currentHandle).map(UserProfileCommunityCommentsRes::from));
    }

    @PutMapping("/profile/me")
    public ResponseEntity<UserProfileSummaryRes> updateMyProfile(@Valid @RequestBody UserProfileUpdateReq request,
                                                                 Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 내 프로필 수정
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(updateUserProfile.execute(currentHandle, request.toUserProfileUpdateInput())
                .map(UserProfileSummaryRes::from));
    }

    @GetMapping("/profiles/{handle}")
    public ResponseEntity<UserProfileSummaryRes> getProfile(@PathVariable String handle,
                                                            Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 공개 프로필 기본 정보 조회
        return ResponseEntity.of(getUserProfileSummary.execute(handle, currentHandle).map(UserProfileSummaryRes::from));
    }

    @GetMapping("/profiles/{handle}/solved-problems")
    public ResponseEntity<UserProfileSolvedProblemsRes> getSolvedProblems(@PathVariable String handle,
                                                                          Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 공개 해결한 문제 조회
        return ResponseEntity.of(getUserProfileSolvedProblems.execute(handle, currentHandle).map(UserProfileSolvedProblemsRes::from));
    }

    @GetMapping("/profiles/{handle}/solved-records")
    public ResponseEntity<UserProfileSolvedRecordsRes> getSolvedRecords(@PathVariable String handle,
                                                                        Authentication authentication) {
        // 현재 사용자 Handle을 해석
        String currentHandle = userProfileSupport.resolveCurrentHandle(authentication);

        // 공개 해결 기록 조회
        return ResponseEntity.of(getUserProfileSolvedRecords.execute(handle, currentHandle).map(UserProfileSolvedRecordsRes::from));
    }

    @GetMapping("/profiles/{handle}/community/posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getCommunityPosts(@PathVariable String handle) {
        // 공개 프로필 작성 게시글 조회
        return ResponseEntity.of(getUserProfileCommunityPosts.execute(handle).map(UserProfileCommunityPostsRes::from));
    }

    @GetMapping("/profiles/{handle}/community/liked-posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getLikedPosts(@PathVariable String handle) {
        // 공개 프로필 좋아요 게시글 조회
        return ResponseEntity.of(getUserProfileLikedPosts.execute(handle).map(UserProfileCommunityPostsRes::from));
    }

    @GetMapping("/profiles/{handle}/community/comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getCommunityComments(@PathVariable String handle) {
        // 공개 프로필 댓글 조회
        return ResponseEntity.of(getUserProfileCommunityComments.execute(handle).map(UserProfileCommunityCommentsRes::from));
    }

    @GetMapping("/profiles/{handle}/community/liked-comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getLikedComments(@PathVariable String handle) {
        // 공개 프로필 좋아요 댓글 조회
        return ResponseEntity.of(getUserProfileLikedComments.execute(handle).map(UserProfileCommunityCommentsRes::from));
    }
}

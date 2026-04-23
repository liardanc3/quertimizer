package com.quertimizer.user.presentation.controller;

import com.quertimizer.user.presentation.dto.request.UserProfileUpdateReq;
import com.quertimizer.user.presentation.dto.response.UserProfileCommunityCommentsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileCommunityPostsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSolvedProblemsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSolvedRecordsRes;
import com.quertimizer.user.presentation.dto.response.UserProfileSummaryRes;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.user.application.service.UserProfileService;
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

    private final UserProfileService userProfileService;
    private final AuthService authService;

    @GetMapping("/profile/me")
    public ResponseEntity<UserProfileSummaryRes> getMyProfile(Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 내 프로필 기본 정보 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getProfileSummary(currentHandle, currentHandle));
    }

    @GetMapping("/profile/me/solved-problems")
    public ResponseEntity<UserProfileSolvedProblemsRes> getMySolvedProblems(Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 내 해결한 문제 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getSolvedProblems(currentHandle, currentHandle));
    }

    @GetMapping("/profile/me/solved-records")
    public ResponseEntity<UserProfileSolvedRecordsRes> getMySolvedRecords(Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 내 해결 기록 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getSolvedRecords(currentHandle, currentHandle));
    }

    @GetMapping("/profile/me/community/posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getMyCommunityPosts(Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 내가 작성한 게시글 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getCommunityPosts(currentHandle));
    }

    @GetMapping("/profile/me/community/liked-posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getMyLikedPosts(Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 내가 좋아요한 게시글 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getLikedPosts(currentHandle));
    }

    @GetMapping("/profile/me/community/comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getMyCommunityComments(Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 내가 작성한 댓글 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getCommunityComments(currentHandle));
    }

    @GetMapping("/profile/me/community/liked-comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getMyLikedComments(Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 내가 좋아요한 댓글 조회
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getLikedComments(currentHandle));
    }

    @PutMapping("/profile/me")
    public ResponseEntity<UserProfileSummaryRes> updateMyProfile(@Valid @RequestBody UserProfileUpdateReq request,
                                                                 Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 내 프로필 수정
        if (currentHandle == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.updateProfile(currentHandle, request));
    }

    @GetMapping("/profiles/{handle}")
    public ResponseEntity<UserProfileSummaryRes> getProfile(@PathVariable String handle,
                                                            Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 공개 프로필 기본 정보 조회
        return ResponseEntity.of(userProfileService.getProfileSummary(handle, currentHandle));
    }

    @GetMapping("/profiles/{handle}/solved-problems")
    public ResponseEntity<UserProfileSolvedProblemsRes> getSolvedProblems(@PathVariable String handle,
                                                                          Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 공개 해결한 문제 조회
        return ResponseEntity.of(userProfileService.getSolvedProblems(handle, currentHandle));
    }

    @GetMapping("/profiles/{handle}/solved-records")
    public ResponseEntity<UserProfileSolvedRecordsRes> getSolvedRecords(@PathVariable String handle,
                                                                        Authentication authentication) {
        String currentHandle = resolveCurrentHandle(authentication);

        // 공개 해결 기록 조회
        return ResponseEntity.of(userProfileService.getSolvedRecords(handle, currentHandle));
    }

    @GetMapping("/profiles/{handle}/community/posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getCommunityPosts(@PathVariable String handle) {

        // 공개 프로필 작성 게시글 조회
        return ResponseEntity.of(userProfileService.getCommunityPosts(handle));
    }

    @GetMapping("/profiles/{handle}/community/liked-posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getLikedPosts(@PathVariable String handle) {

        // 공개 프로필 좋아요 게시글 조회
        return ResponseEntity.of(userProfileService.getLikedPosts(handle));
    }

    @GetMapping("/profiles/{handle}/community/comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getCommunityComments(@PathVariable String handle) {

        // 공개 프로필 댓글 조회
        return ResponseEntity.of(userProfileService.getCommunityComments(handle));
    }

    @GetMapping("/profiles/{handle}/community/liked-comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getLikedComments(@PathVariable String handle) {

        // 공개 프로필 좋아요 댓글 조회
        return ResponseEntity.of(userProfileService.getLikedComments(handle));
    }

    private String resolveCurrentHandle(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }

}

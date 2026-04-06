package com.quertimizer.endpoint.api.controller;

import com.quertimizer.endpoint.api.dto.request.UserProfileUpdateReq;
import com.quertimizer.endpoint.api.dto.response.UserProfileCommunityCommentsRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileCommunityPostsRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileSolvedProblemsRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileSolvedRecordsRes;
import com.quertimizer.endpoint.api.dto.response.UserProfileSummaryRes;
import com.quertimizer.service.UserProfileService;
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

    @GetMapping("/profile/me")
    public ResponseEntity<UserProfileSummaryRes> getMyProfile(Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 내 프로필 기본 정보 조회
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getProfileSummary(currentUserId, currentUserId));
    }

    @GetMapping("/profile/me/solved-problems")
    public ResponseEntity<UserProfileSolvedProblemsRes> getMySolvedProblems(Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 내 해결한 문제 조회
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getSolvedProblems(currentUserId, currentUserId));
    }

    @GetMapping("/profile/me/solved-records")
    public ResponseEntity<UserProfileSolvedRecordsRes> getMySolvedRecords(Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 내 해결 기록 조회
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getSolvedRecords(currentUserId, currentUserId));
    }

    @GetMapping("/profile/me/community/posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getMyCommunityPosts(Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 내가 작성한 게시글 조회
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getCommunityPosts(currentUserId));
    }

    @GetMapping("/profile/me/community/liked-posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getMyLikedPosts(Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 내가 좋아요한 게시글 조회
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getLikedPosts(currentUserId));
    }

    @GetMapping("/profile/me/community/comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getMyCommunityComments(Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 내가 작성한 댓글 조회
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.getCommunityComments(currentUserId));
    }

    @PutMapping("/profile/me")
    public ResponseEntity<UserProfileSummaryRes> updateMyProfile(@Valid @RequestBody UserProfileUpdateReq request,
                                                                 Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 내 프로필 수정
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.of(userProfileService.updateProfile(currentUserId, request));
    }

    @GetMapping("/profiles/{userId}")
    public ResponseEntity<UserProfileSummaryRes> getProfile(@PathVariable String userId,
                                                            Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 공개 프로필 기본 정보 조회
        return ResponseEntity.of(userProfileService.getProfileSummary(userId, currentUserId));
    }

    @GetMapping("/profiles/{userId}/solved-problems")
    public ResponseEntity<UserProfileSolvedProblemsRes> getSolvedProblems(@PathVariable String userId,
                                                                          Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 공개 해결한 문제 조회
        return ResponseEntity.of(userProfileService.getSolvedProblems(userId, currentUserId));
    }

    @GetMapping("/profiles/{userId}/solved-records")
    public ResponseEntity<UserProfileSolvedRecordsRes> getSolvedRecords(@PathVariable String userId,
                                                                        Authentication authentication) {
        String currentUserId = resolveCurrentUserId(authentication);

        // 공개 해결 기록 조회
        return ResponseEntity.of(userProfileService.getSolvedRecords(userId, currentUserId));
    }

    @GetMapping("/profiles/{userId}/community/posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getCommunityPosts(@PathVariable String userId) {

        // 공개 프로필 작성 게시글 조회
        return ResponseEntity.of(userProfileService.getCommunityPosts(userId));
    }

    @GetMapping("/profiles/{userId}/community/liked-posts")
    public ResponseEntity<UserProfileCommunityPostsRes> getLikedPosts(@PathVariable String userId) {

        // 공개 프로필 좋아요 게시글 조회
        return ResponseEntity.of(userProfileService.getLikedPosts(userId));
    }

    @GetMapping("/profiles/{userId}/community/comments")
    public ResponseEntity<UserProfileCommunityCommentsRes> getCommunityComments(@PathVariable String userId) {

        // 공개 프로필 댓글 조회
        return ResponseEntity.of(userProfileService.getCommunityComments(userId));
    }

    private String resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

}

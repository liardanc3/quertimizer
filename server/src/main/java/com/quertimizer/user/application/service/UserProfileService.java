package com.quertimizer.user.application.service;

import com.quertimizer.user.application.input.UserProfileLinkInput;
import com.quertimizer.user.application.input.UserProfileUpdateInput;
import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import com.quertimizer.user.application.output.UserProfileLinkOutput;
import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import com.quertimizer.user.application.output.UserProfileSummaryOutput;
import com.quertimizer.user.application.port.out.UserExternalLinkRepositoryPort;
import com.quertimizer.user.application.port.out.UserProfileCommunityPort;
import com.quertimizer.user.application.port.out.UserProfileProblemPort;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.user.domain.entity.UserExternalLink;
import com.quertimizer.user.domain.model.UserProfileCommunityCounts;
import com.quertimizer.user.domain.model.UserProfilePageConstant;
import com.quertimizer.user.domain.model.UserProfileProblemSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserExternalLinkRepositoryPort userExternalLinkRepository;
    private final UserProfileProblemPort userProfileProblemPort;
    private final UserProfileCommunityPort userProfileCommunityPort;
    private final UserRepositoryPort userRepository;

    public UserProfileSummaryOutput buildUserProfileSummary(User user, boolean isOwnProfile) {
        // 프로필 요약에 필요한 문제 기록과 외부 링크 조회
        UserProfileProblemSummary problemSummary = userProfileProblemPort.getProblemSummary(user.getHandle());
        List<UserExternalLink> externalLinks = userExternalLinkRepository.findAllByIdHandleOrderByIdTypeAscIdLinkAsc(user.getHandle());

        // 누적 해결 통계 동기화
        syncSolvedStatistics(user, problemSummary);

        // 공개 설정에 맞는 백분위와 커뮤니티 활동 수 결정
        boolean executionPercentileVisible = isOwnProfile || user.isExecutionPercentilePublicEnabled();
        boolean communityActivityVisible = isOwnProfile || user.isCommunityActivityPublicEnabled();
        UserProfileCommunityCounts communityCounts = communityActivityVisible
                ? userProfileCommunityPort.getCommunityCounts(user.getHandle())
                : UserProfileCommunityCounts.empty();

        return new UserProfileSummaryOutput(
                user.getHandle(), user.getResolvedBio(),
                user.getResolvedProfileImageUrl(), user.getResolvedBackgroundImageUrl(),
                user.getSignupAt(), createProfileLinkResponses(externalLinks),
                user.getResolvedDefaultDbms().getValue(),
                user.isSqlPublicEnabled(), user.isExecutionPercentilePublicEnabled(),
                user.isSolvedRecordsPublicEnabled(), user.isSolvedProblemCountPublicEnabled(),
                user.isCommunityActivityPublicEnabled(),
                executionPercentileVisible ? problemSummary.getPostgresqlExecutionPercentile() : null,
                executionPercentileVisible ? problemSummary.getMysqlExecutionPercentile() : null,
                communityCounts.getAuthoredPostCount(), communityCounts.getLikedPostCount(), communityCounts.getCommentCount()
        );
    }

    public UserProfileSolvedProblemsOutput buildSolvedProblems(User user, boolean isOwnProfile) {
        // 해결한 문제 공개 여부 확인
        if (!isOwnProfile && !user.isSolvedProblemCountPublicEnabled()) {
            return new UserProfileSolvedProblemsOutput(0, List.of());
        }

        return userProfileProblemPort.getSolvedProblems(user.getHandle());
    }

    public UserProfileSolvedRecordsOutput buildSolvedRecords(User user, boolean isOwnProfile) {
        // 해결 기록 공개 여부 확인
        if (!isOwnProfile && !user.isSolvedRecordsPublicEnabled()) {
            return new UserProfileSolvedRecordsOutput(List.of());
        }

        return userProfileProblemPort.getSolvedRecords(user.getHandle());
    }

    public UserProfileSubmissionSummaryOutput buildSubmissionSummary(User user, boolean isOwnProfile) {
        // 프로필 본문에 필요한 제출 요약 정보를 구성
        return userProfileProblemPort.getSubmissionSummary(user.getHandle());
    }

    public UserProfileCommunityPostsOutput buildCommunityPosts(User user, String currentHandle) {
        // 커뮤니티 활동 공개 여부에 따른 작성 게시글 목록 생성
        return canShowCommunityActivity(user, currentHandle)
                ? userProfileCommunityPort.getAuthoredPosts(user.getHandle())
                : new UserProfileCommunityPostsOutput(List.of());
    }

    public UserProfileCommunityPostsOutput buildLikedPosts(User user, String currentHandle) {
        // 커뮤니티 활동 공개 여부에 따른 좋아요 게시글 목록 생성
        return canShowCommunityActivity(user, currentHandle)
                ? userProfileCommunityPort.getLikedPosts(user.getHandle())
                : new UserProfileCommunityPostsOutput(List.of());
    }

    public UserProfileCommunityCommentsOutput buildCommunityComments(User user, String currentHandle) {
        // 커뮤니티 활동 공개 여부에 따른 작성 댓글 목록 생성
        return canShowCommunityActivity(user, currentHandle)
                ? userProfileCommunityPort.getAuthoredComments(user.getHandle())
                : new UserProfileCommunityCommentsOutput(List.of());
    }

    public UserProfileCommunityCommentsOutput buildLikedComments(User user, String currentHandle) {
        // 커뮤니티 활동 공개 여부에 따른 좋아요 댓글 목록 생성
        return canShowCommunityActivity(user, currentHandle)
                ? userProfileCommunityPort.getLikedComments(user.getHandle())
                : new UserProfileCommunityCommentsOutput(List.of());
    }

    public UserProfileCommunityActivitiesOutput buildCommunityActivities(User user,
                                                                        String currentHandle,
                                                                        int requestedPage,
                                                                        Integer requestedPageSize) {
        // 공개 여부를 반영한 커뮤니티 활동 페이지 생성
        return canShowCommunityActivity(user, currentHandle)
                ? userProfileCommunityPort.getActivities(user.getHandle(), requestedPage, requestedPageSize)
                : createEmptyCommunityActivitiesPage(requestedPage, requestedPageSize);
    }

    public UserProfileSummaryOutput updateProfile(User user, UserProfileUpdateInput input) {
        // 소개글, 기본 설정 수정
        user.changeProfile(
                normalizeBio(input.getBio()),
                normalizeProfileImageUrl(input.getProfileImageUrl()),
                normalizeBackgroundImageUrl(input.getBackgroundImageUrl()),
                input.getDefaultDbms(),
                input.isSqlPublic(),
                input.isExecutionPercentilePublic(),
                input.isSolvedRecordsPublic(),
                input.isSolvedProblemCountPublic(),
                input.isCommunityActivityPublic()
        );
        User savedUser = userRepository.save(user);

        // 프로필 링크를 입력값으로 교체 후 요약 응답 생성
        replaceExternalLinks(savedUser.getHandle(), input.getLinks());
        return buildUserProfileSummary(savedUser, true);
    }

    private void syncSolvedStatistics(User user, UserProfileProblemSummary problemSummary) {
        // 해결 통계가 최신이면 동기화 생략
        if (user.getResolvedSolvedProblemCount() == problemSummary.getSolvedProblemCount()
                && user.getResolvedSolvedExecutionTimeSumMs() == problemSummary.getSolvedExecutionTimeSumMs()) {
            return;
        }

        // 해결 통계 변경 후 저장
        user.changeSolvedStatistics(problemSummary.getSolvedProblemCount(), problemSummary.getSolvedExecutionTimeSumMs());
        userRepository.save(user);
    }

    private boolean canShowCommunityActivity(User user, String currentHandle) {
        // 본인 또는 공개 설정된 사용자만 커뮤니티 활동 조회 허용
        return user.getHandle().equals(currentHandle) || user.isCommunityActivityPublicEnabled();
    }

    private UserProfileCommunityActivitiesOutput createEmptyCommunityActivitiesPage(int requestedPage, Integer requestedPageSize) {
        // 비공개 커뮤니티 활동에 대한 빈 페이지 생성
        int pageSize = requestedPageSize == null
                ? UserProfilePageConstant.DEFAULT_COMMUNITY_ACTIVITY_PAGE_SIZE
                : Math.min(UserProfilePageConstant.MAX_COMMUNITY_ACTIVITY_PAGE_SIZE, Math.max(1, requestedPageSize));
        return new UserProfileCommunityActivitiesOutput(Math.max(1, requestedPage), pageSize, 0, 1, List.of());
    }

    private List<UserProfileLinkOutput> createProfileLinkResponses(List<UserExternalLink> externalLinks) {
        // 프로필 링크 응답 목록 생성
        return externalLinks.stream()
                .map(link -> new UserProfileLinkOutput(link.getType(), link.getLink()))
                .toList();
    }

    private void replaceExternalLinks(String handle, List<UserProfileLinkInput> links) {
        // 외부 링크 목록 교체
        List<UserExternalLink> normalizedExternalLinks = normalizeExternalLinks(handle, links);
        userExternalLinkRepository.deleteAllByIdHandle(handle);

        // 저장할 외부 링크가 없으면 종료
        if (normalizedExternalLinks.isEmpty()) {
            return;
        }

        userExternalLinkRepository.saveAll(normalizedExternalLinks);
    }

    private List<UserExternalLink> normalizeExternalLinks(String handle, List<UserProfileLinkInput> links) {
        // 외부 링크 목록 정규화
        Map<String, UserExternalLink> externalLinkByKey = new LinkedHashMap<>();

        // 공백, 중복 링크를 제거하고 저장용 엔티티 구성
        for (UserProfileLinkInput link : links) {
            String type = link.getType().trim();
            String value = link.getValue().trim();

            if (type.isEmpty() || value.isEmpty()) {
                continue;
            }

            externalLinkByKey.putIfAbsent(type + "|" + value, UserExternalLink.create(handle, type, value));
        }

        return new ArrayList<>(externalLinkByKey.values());
    }

    private String normalizeBio(String bio) {
        // Bio 정규화
        return bio != null ? bio.trim() : "";
    }

    private String normalizeProfileImageUrl(String profileImageUrl) {
        // 프로필 이미지 URL 정규화
        return profileImageUrl != null ? profileImageUrl.trim() : "";
    }

    private String normalizeBackgroundImageUrl(String backgroundImageUrl) {
        // 프로필 배경 이미지 URL 정규화
        return backgroundImageUrl != null ? backgroundImageUrl.trim() : "";
    }

}

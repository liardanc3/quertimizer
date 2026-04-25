package com.quertimizer.community.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.community.application.output.CommunityPostPageOutput;
import com.quertimizer.community.application.output.CommunityPostSummaryOutput;
import com.quertimizer.community.domain.entity.CommunityPost;
import com.quertimizer.community.domain.entity.CommunityPostTag;
import com.quertimizer.community.domain.policy.CommunityPostIdPolicy;
import com.quertimizer.community.application.port.CommunityPostRepository;
import com.quertimizer.community.application.port.CommunityPostTagRepository;
import com.quertimizer.community.infrastructure.search.CommunityPostDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommunitySearchService {

    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;
    private final ObjectMapper objectMapper;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostTagRepository communityPostTagRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void syncAllPosts() {
        // 서버 기동 시 게시글 검색 인덱스를 동기화
        ElasticsearchOperations elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();

        if (elasticsearchOperations == null) {
            return;
        }

        List<CommunityPost> posts = communityPostRepository.findAll();
        Map<Long, List<String>> tagsByPostId = createTagsByPostId(posts.stream().map(CommunityPost::getPostId).toList());

        // 서버 시작 시 기존 게시글 검색 인덱스 동기화
        for (CommunityPost post : posts) {
            syncPost(post, tagsByPostId.getOrDefault(post.getPostId(), List.of()));
        }
    }

    public CommunityPostPageOutput searchPosts(int requestedPage,
                                               int pageSize,
                                               String searchKeyword,
                                               String tag,
                                               String category,
                                               String sortKey,
                                               List<CommunityPost> posts,
                                               Map<Long, List<String>> tagsByPostId) {
        ElasticsearchOperations elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();

        if (elasticsearchOperations == null) {
            return searchPostsInMemory(requestedPage, pageSize, searchKeyword, tag, category, sortKey, posts, tagsByPostId);
        }

        try {
            return searchPostsInElasticsearch(
                    elasticsearchOperations,
                    requestedPage,
                    pageSize,
                    searchKeyword,
                    tag,
                    category,
                    sortKey,
                    posts,
                    tagsByPostId
            );
        } catch (RuntimeException ignored) {
            return searchPostsInMemory(requestedPage, pageSize, searchKeyword, tag, category, sortKey, posts, tagsByPostId);
        }
    }

    public void syncPost(CommunityPost post, List<String> tags) {
        // 단일 게시글 검색 인덱스를 동기화
        ElasticsearchOperations elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();

        if (elasticsearchOperations == null) {
            return;
        }

        // 게시글 검색 문서 최신화
        try {
            elasticsearchOperations.save(CommunityPostDocument.create(
                    CommunityPostIdPolicy.format(post.getPostId()),
                    post.getTitle(),
                    post.getHandle(),
                    post.getPlainTextSummary(),
                    tags,
                    resolveCategory(post),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getViewCount(),
                    post.getCreatedAt()
            ));
        } catch (RuntimeException ignored) {
            // 검색 인덱스 동기화 실패는 목록 조회 fallback으로 보완
        }
    }

    public void deletePost(Long postId) {
        // 삭제된 게시글 검색 인덱스를 정리
        ElasticsearchOperations elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();

        if (elasticsearchOperations == null) {
            return;
        }

        // 삭제된 게시글 검색 문서 제거
        try {
            elasticsearchOperations.delete(CommunityPostIdPolicy.format(postId), CommunityPostDocument.class);
        } catch (RuntimeException ignored) {
            // 검색 인덱스 정리 실패는 후속 동기화에서 다시 맞춘다
        }
    }

    private Map<Long, List<String>> createTagsByPostId(List<Long> postIds) {
        // 게시글 번호별 태그 목록 생성
        Map<Long, List<String>> tagsByPostId = new HashMap<>();

        if (postIds.isEmpty()) {
            return tagsByPostId;
        }

        for (CommunityPostTag postTag : communityPostTagRepository.findAllByPostIdInOrderByPostIdAscTagOrderAsc(postIds)) {
            tagsByPostId.computeIfAbsent(postTag.getPostId(), key -> new ArrayList<>())
                    .add(postTag.getTag());
        }

        return tagsByPostId;
    }

    private CommunityPostPageOutput searchPostsInElasticsearch(ElasticsearchOperations elasticsearchOperations,
                                                               int requestedPage,
                                                               int pageSize,
                                                               String searchKeyword,
                                                               String tag,
                                                               String category,
                                                               String sortKey,
                                                               List<CommunityPost> posts,
                                                               Map<Long, List<String>> tagsByPostId) {
        StringQuery query = new StringQuery(createSearchQuery(searchKeyword, tag, category, sortKey));
        query.setPageable(PageRequest.of(Math.max(requestedPage - 1, 0), pageSize));

        SearchHits<CommunityPostDocument> hits = elasticsearchOperations.search(query, CommunityPostDocument.class);
        List<String> orderedPostIds = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(CommunityPostDocument::getPostId)
                .toList();
        Map<String, CommunityPost> postById = new HashMap<>();

        // 검색 결과 순서를 유지하려고 게시글 id 기준 map 구성
        for (CommunityPost post : posts) {
            postById.put(CommunityPostIdPolicy.format(post.getPostId()), post);
        }

        List<CommunityPostSummaryOutput> postResponses = orderedPostIds.stream()
                .map(postById::get)
                .filter(java.util.Objects::nonNull)
                .map(post -> toCommunityPostSummary(post, tagsByPostId.getOrDefault(post.getPostId(), List.of())))
                .toList();
        int totalPages = Math.max(1, (int) Math.ceil(hits.getTotalHits() / (double) pageSize));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);

        return new CommunityPostPageOutput(
                currentPage,
                pageSize,
                hits.getTotalHits(),
                totalPages,
                postResponses
        );
    }

    private String createSearchQuery(String searchKeyword, String tag, String category, String sortKey) {
        // 검색 쿼리 생성
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> functionScore = new LinkedHashMap<>();
        Map<String, Object> bool = new LinkedHashMap<>();
        List<Object> must = new ArrayList<>();
        List<Object> filter = new ArrayList<>();
        List<Object> functions = new ArrayList<>();

        // 제목, 태그, 작성자, 본문 가중치 검색
        if (StringUtils.hasText(searchKeyword)) {
            must.add(Map.of(
                    "simple_query_string",
                    Map.of(
                            "query", searchKeyword.trim(),
                            "fields", List.of("title^8", "tags^6", "authorId^4", "contentText^2"),
                            "default_operator", "and"
                    )
            ));
        } else {
            must.add(Map.of("match_all", Map.of()));
        }

        // 선택한 태그 필터 적용
        if (StringUtils.hasText(tag)) {
            filter.add(Map.of("term", Map.of("tags.keyword", tag.trim())));
        }

        if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category.trim())) {
            filter.add(Map.of("term", Map.of("category.keyword", category.trim().toLowerCase(Locale.ROOT))));
        }

        bool.put("must", must);
        if (!filter.isEmpty()) {
            bool.put("filter", filter);
        }

        // 좋아요, 댓글수, 최신순 가중치 반영
        functions.add(Map.of("field_value_factor", Map.of(
                "field", "likeCount",
                "factor", 0.25,
                "modifier", "sqrt",
                "missing", 0
        )));
        functions.add(Map.of("field_value_factor", Map.of(
                "field", "commentCount",
                "factor", 0.35,
                "modifier", "sqrt",
                "missing", 0
        )));
        functions.add(Map.of("gauss", Map.of(
                "createdAt",
                Map.of(
                        "origin", "now",
                        "scale", "30d",
                        "offset", "7d",
                        "decay", 0.4
                )
        )));

        functionScore.put("query", Map.of("bool", bool));
        functionScore.put("functions", functions);
        functionScore.put("score_mode", "sum");
        functionScore.put("boost_mode", "sum");
        root.put("query", Map.of("function_score", functionScore));

        // 명시 정렬이 있으면 검색 점수 대신 정렬 기준 적용
        if ("latest".equalsIgnoreCase(sortKey) || ("default".equalsIgnoreCase(sortKey) && !StringUtils.hasText(searchKeyword))) {
            root.put("sort", List.of(Map.of("createdAt", Map.of("order", "desc"))));
        } else if ("oldest".equalsIgnoreCase(sortKey)) {
            root.put("sort", List.of(Map.of("createdAt", Map.of("order", "asc"))));
        } else if ("likes".equalsIgnoreCase(sortKey)) {
            root.put("sort", List.of(
                    Map.of("likeCount", Map.of("order", "desc")),
                    Map.of("createdAt", Map.of("order", "desc"))
            ));
        } else if ("likesAsc".equalsIgnoreCase(sortKey)) {
            root.put("sort", List.of(
                    Map.of("likeCount", Map.of("order", "asc")),
                    Map.of("createdAt", Map.of("order", "desc"))
            ));
        } else if ("comments".equalsIgnoreCase(sortKey)) {
            root.put("sort", List.of(
                    Map.of("commentCount", Map.of("order", "desc")),
                    Map.of("createdAt", Map.of("order", "desc"))
            ));
        } else if ("commentsAsc".equalsIgnoreCase(sortKey)) {
            root.put("sort", List.of(
                    Map.of("commentCount", Map.of("order", "asc")),
                    Map.of("createdAt", Map.of("order", "desc"))
            ));
        } else if ("views".equalsIgnoreCase(sortKey)) {
            root.put("sort", List.of(
                    Map.of("viewCount", Map.of("order", "desc")),
                    Map.of("createdAt", Map.of("order", "desc"))
            ));
        } else if ("viewsAsc".equalsIgnoreCase(sortKey)) {
            root.put("sort", List.of(
                    Map.of("viewCount", Map.of("order", "asc")),
                    Map.of("createdAt", Map.of("order", "desc"))
            ));
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private CommunityPostPageOutput searchPostsInMemory(int requestedPage,
                                                        int pageSize,
                                                        String searchKeyword,
                                                        String tag,
                                                        String category,
                                                        String sortKey,
                                                        List<CommunityPost> posts,
                                                        Map<Long, List<String>> tagsByPostId) {
        String normalizedSearchKeyword = normalizeKeyword(searchKeyword);
        String normalizedTag = normalizeKeyword(tag);
        String normalizedCategory = normalizeCategory(category);
        List<RankedPost> rankedPosts = posts.stream()
                .filter(post -> matchesCategory(post, normalizedCategory))
                .filter(post -> matchesTag(tagsByPostId.getOrDefault(post.getPostId(), List.of()), normalizedTag))
                .map(post -> new RankedPost(
                        post,
                        tagsByPostId.getOrDefault(post.getPostId(), List.of()),
                        calculateScore(post, tagsByPostId.getOrDefault(post.getPostId(), List.of()), normalizedSearchKeyword)
                ))
                .filter(rankedPost -> normalizedSearchKeyword.isBlank() || rankedPost.score() > 0)
                .sorted(createComparator(sortKey, !normalizedSearchKeyword.isBlank()))
                .toList();

        int totalCount = rankedPosts.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalCount);
        int toIndex = Math.min(fromIndex + pageSize, totalCount);

        return new CommunityPostPageOutput(
                currentPage,
                pageSize,
                totalCount,
                totalPages,
                rankedPosts.subList(fromIndex, toIndex).stream()
                        .map(rankedPost -> toCommunityPostSummary(rankedPost.post(), rankedPost.tags()))
                        .toList()
        );
    }

    private Comparator<RankedPost> createComparator(String sortKey, boolean hasSearchKeyword) {
        // 비교 기준 생성
        if ("latest".equalsIgnoreCase(sortKey) || ("default".equalsIgnoreCase(sortKey) && !hasSearchKeyword)) {
            return Comparator.comparing((RankedPost rankedPost) -> rankedPost.post().getCreatedAt(), Comparator.reverseOrder())
                    .thenComparing(rankedPost -> rankedPost.post().getPostId());
        }

        if ("oldest".equalsIgnoreCase(sortKey)) {
            return Comparator.comparing((RankedPost rankedPost) -> rankedPost.post().getCreatedAt())
                    .thenComparing(rankedPost -> rankedPost.post().getPostId());
        }

        if ("views".equalsIgnoreCase(sortKey)) {
            return Comparator.comparingInt((RankedPost rankedPost) -> rankedPost.post().getViewCount())
                    .reversed()
                    .thenComparing(rankedPost -> rankedPost.post().getCreatedAt(), Comparator.reverseOrder());
        }

        if ("viewsAsc".equalsIgnoreCase(sortKey)) {
            return Comparator.comparingInt((RankedPost rankedPost) -> rankedPost.post().getViewCount())
                    .thenComparing(rankedPost -> rankedPost.post().getCreatedAt(), Comparator.reverseOrder());
        }

        if ("likes".equalsIgnoreCase(sortKey)) {
            return Comparator.comparingInt((RankedPost rankedPost) -> rankedPost.post().getLikeCount())
                    .reversed()
                    .thenComparing(rankedPost -> rankedPost.post().getCreatedAt(), Comparator.reverseOrder());
        }

        if ("likesAsc".equalsIgnoreCase(sortKey)) {
            return Comparator.comparingInt((RankedPost rankedPost) -> rankedPost.post().getLikeCount())
                    .thenComparing(rankedPost -> rankedPost.post().getCreatedAt(), Comparator.reverseOrder());
        }

        if ("comments".equalsIgnoreCase(sortKey)) {
            return Comparator.comparingInt((RankedPost rankedPost) -> rankedPost.post().getCommentCount())
                    .reversed()
                    .thenComparing(rankedPost -> rankedPost.post().getCreatedAt(), Comparator.reverseOrder());
        }

        if ("commentsAsc".equalsIgnoreCase(sortKey)) {
            return Comparator.comparingInt((RankedPost rankedPost) -> rankedPost.post().getCommentCount())
                    .thenComparing(rankedPost -> rankedPost.post().getCreatedAt(), Comparator.reverseOrder());
        }

        return Comparator.comparingDouble(RankedPost::score)
                .reversed()
                .thenComparing(rankedPost -> rankedPost.post().getCreatedAt(), Comparator.reverseOrder());
    }

    private boolean matchesTag(List<String> tags, String normalizedTag) {
        // 태그 일치 여부 확인
        if (normalizedTag.isBlank()) {
            return true;
        }

        return tags.stream().map(this::normalizeKeyword).anyMatch(normalizedTag::equals);
    }

    private double calculateScore(CommunityPost post, List<String> tags, String normalizedSearchKeyword) {
        // 점수 계산
        if (normalizedSearchKeyword.isBlank()) {
            return post.getLikeCount() * 0.4
                    + post.getCommentCount() * 0.6
                    + post.getViewCount() * 0.1
                    + Math.max(0, 1000 - Math.abs(Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours()));
        }

        double titleScore = calculateTextScore(post.getTitle(), normalizedSearchKeyword, 8d);
        double authorScore = calculateTextScore(post.getHandle(), normalizedSearchKeyword, 4d);
        double contentScore = calculateTextScore(post.getPlainTextSummary(), normalizedSearchKeyword, 2d);
        double tagScore = tags.stream()
                .mapToDouble(tag -> calculateTextScore(tag, normalizedSearchKeyword, 6d))
                .sum();

        return titleScore
                + authorScore
                + contentScore
                + tagScore
                + post.getLikeCount() * 0.4
                + post.getCommentCount() * 0.6
                + post.getViewCount() * 0.1;
    }

    private double calculateTextScore(String value, String normalizedSearchKeyword, double weight) {
        // 텍스트 점수 계산
        String normalizedValue = normalizeKeyword(value);

        if (normalizedValue.isBlank()) {
            return 0d;
        }

        if (normalizedValue.equals(normalizedSearchKeyword)) {
            return 100d * weight;
        }

        if (normalizedValue.startsWith(normalizedSearchKeyword)) {
            return 70d * weight;
        }

        if (normalizedValue.contains(normalizedSearchKeyword)) {
            return 40d * weight;
        }

        return 0d;
    }


    private String normalizeCategory(String value) {
        // 구분 정규화
        if (!StringUtils.hasText(value) || "all".equalsIgnoreCase(value.trim())) {
            return "all";
        }

        String normalizedCategory = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedCategory) {
            case "notice", "question" -> normalizedCategory;
            default -> "discussion";
        };
    }

    private boolean matchesCategory(CommunityPost post, String normalizedCategory) {
        // 구분 일치 여부 확인
        return "all".equals(normalizedCategory) || resolveCategory(post).equals(normalizedCategory);
    }

    private String resolveCategory(CommunityPost post) {
        // 구분 결정
        if (StringUtils.hasText(post.getCategory())) {
            return normalizeCategory(post.getCategory());
        }

        int postNumber = CommunityPostIdPolicy.resolveSeedPostNumber(post.getPostId()).orElse(0);

        if (postNumber > 0) {
            if (postNumber % 10 == 0) {
                return "notice";
            }

            return postNumber % 2 == 0 ? "discussion" : "question";
        }

        return "discussion";
    }

    private String normalizeKeyword(String value) {
        // 키워드 정규화
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private CommunityPostSummaryOutput toCommunityPostSummary(CommunityPost post, List<String> tags) {
        // 커뮤니티 게시글 요약 변환
        return new CommunityPostSummaryOutput(
                CommunityPostIdPolicy.format(post.getPostId()),
                post.getTitle(),
                post.getHandle(),
                createExcerpt(post.getPlainTextSummary()),
                tags,
                resolveCategory(post),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount()
        );
    }

    private String createExcerpt(String contentText) {
        // 요약문 생성
        if (!StringUtils.hasText(contentText)) {
            return "";
        }

        String normalizedContentText = contentText.trim();
        return normalizedContentText.length() > 120
                ? normalizedContentText.substring(0, 120).trim() + "..."
                : normalizedContentText;
    }

    private record RankedPost(CommunityPost post, List<String> tags, double score) {
        // Ranked 게시글 처리
    }

}

package com.quertimizer.community.adapter.out.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunitySearchIndexManager {

    private static final String INDEX_NAME = "community-post-v2";
    private static final String SYNONYM_SET_ID = "community-search-synonyms";
    private static final String INDEX_RESOURCE = "classpath:elasticsearch/community-post-index-v2.json";
    private static final String SYNONYM_RESOURCE = "classpath:elasticsearch/community-search-synonyms.json";
    private static final long RETRY_INTERVAL_MILLIS = 60_000L;

    private final ObjectProvider<RestClient> restClientProvider;
    private final ResourceLoader resourceLoader;

    private volatile boolean ready;
    private volatile long nextAttemptMillis;

    public void ensureReady() {
        // 이미 준비됐거나 재시도 대기 중이면 생략
        long now = System.currentTimeMillis();
        if (ready || now < nextAttemptMillis) {
            return;
        }

        synchronized (this) {
            if (ready || System.currentTimeMillis() < nextAttemptMillis) {
                return;
            }

            // Elasticsearch low-level client 없으면 fallback 검색에 맡김
            RestClient restClient = restClientProvider.getIfAvailable();
            if (restClient == null) {
                nextAttemptMillis = System.currentTimeMillis() + RETRY_INTERVAL_MILLIS;
                return;
            }

            try {
                putSynonymSet(restClient);
                createIndexIfAbsent(restClient);
                ready = true;
            } catch (IOException | RuntimeException exception) {
                nextAttemptMillis = System.currentTimeMillis() + RETRY_INTERVAL_MILLIS;
                log.warn("커뮤니티 검색 인덱스 준비 실패 index={}", INDEX_NAME, exception);
            }
        }
    }

    private void putSynonymSet(RestClient restClient) throws IOException {
        // 검색 동의어 set 최신화
        Request request = new Request("PUT", "/_synonyms/" + SYNONYM_SET_ID);
        request.setJsonEntity(readResource(SYNONYM_RESOURCE));
        restClient.performRequest(request);
    }

    private void createIndexIfAbsent(RestClient restClient) throws IOException {
        // 커뮤니티 검색 인덱스 없으면 생성
        if (exists(restClient, "/" + INDEX_NAME)) {
            return;
        }

        Request request = new Request("PUT", "/" + INDEX_NAME);
        request.setJsonEntity(readResource(INDEX_RESOURCE));
        restClient.performRequest(request);
    }

    private boolean exists(RestClient restClient, String endpoint) throws IOException {
        // HEAD 요청으로 ES 리소스 존재 여부 확인
        try {
            restClient.performRequest(new Request("HEAD", endpoint));
            return true;
        } catch (ResponseException exception) {
            if (exception.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            }

            throw exception;
        }
    }

    private String readResource(String location) throws IOException {
        // classpath JSON 리소스 읽기
        try (var inputStream = resourceLoader.getResource(location).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

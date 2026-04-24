package com.quertimizer.favorite.domain.model;

public enum FavoriteFailReason {

    SNAPSHOT_SERIALIZE_FAILED("즐겨찾기 스냅샷 직렬화에 실패했습니다.");

    private final String message;

    FavoriteFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 메시지 조회
        return message;
    }

}

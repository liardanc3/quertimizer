package com.quertimizer.favorite.domain.model;

public final class FavoriteValidationMessage {

    public static final String TAB_LIMIT_EXCEEDED = "즐겨찾기는 최대 10개까지 저장할 수 있습니다.";
    public static final String LABEL_REQUIRED = "즐겨찾기 이름은 비어 있을 수 없습니다.";
    public static final String LABEL_LENGTH_EXCEEDED = "즐겨찾기 이름은 최대 200자까지 가능합니다.";
    public static final String PATH_REQUIRED = "즐겨찾기 경로는 비어 있을 수 없습니다.";
    public static final String PATH_LENGTH_EXCEEDED = "즐겨찾기 경로는 최대 2048자까지 가능합니다.";

    private FavoriteValidationMessage() {
    }

}

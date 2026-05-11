package com.quertimizer.favorite.domain.entity;

import lombok.Data;

@Data
public class FavoriteTab {

    private Long favoriteTabId;
    private String userEmail;
    private int displayOrder;
    private String label;
    private String path;
    private String snapshotJson;

    public static FavoriteTab create(String userEmail,
                                     int displayOrder,
                                     String label,
                                     String path,
                                     String snapshotJson) {
        return new FavoriteTab(userEmail, displayOrder, label, path, snapshotJson);
    }

    public static FavoriteTab restore(Long favoriteTabId, String userEmail,
                                      int displayOrder, String label,
                                      String path, String snapshotJson) {
        // 저장된 즐겨찾기 탭 상태 복원
        FavoriteTab favoriteTab = new FavoriteTab(userEmail, displayOrder, label, path, snapshotJson);
        favoriteTab.favoriteTabId = favoriteTabId;
        return favoriteTab;
    }

    private FavoriteTab(String userEmail,
                        int displayOrder,
                        String label,
                        String path,
                        String snapshotJson) {
        this.userEmail = userEmail;
        this.displayOrder = displayOrder;
        this.label = label;
        this.path = path;
        this.snapshotJson = snapshotJson;
    }

}

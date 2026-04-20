package com.quertimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favorite_tab")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteTab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_tab_id", nullable = false)
    private Long favoriteTabId;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(nullable = false, length = 2048)
    private String path;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    public static FavoriteTab create(String userEmail,
                                     int displayOrder,
                                     String label,
                                     String path,
                                     String snapshotJson) {
        return new FavoriteTab(userEmail, displayOrder, label, path, snapshotJson);
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

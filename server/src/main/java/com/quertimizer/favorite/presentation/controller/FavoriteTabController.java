package com.quertimizer.favorite.presentation.controller;

import com.quertimizer.favorite.presentation.dto.request.FavoriteTabsUpdateReq;
import com.quertimizer.favorite.presentation.dto.response.FavoriteTabsRes;
import com.quertimizer.favorite.application.usecase.GetFavoriteTabs;
import com.quertimizer.favorite.application.usecase.ReplaceFavoriteTabs;
import com.quertimizer.favorite.presentation.support.FavoriteTabSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FavoriteTabController {

    private final GetFavoriteTabs getFavoriteTabs;
    private final ReplaceFavoriteTabs replaceFavoriteTabs;

    private final FavoriteTabSupport favoriteTabSupport;

    @GetMapping("/profile/me/favorites")
    public ResponseEntity<FavoriteTabsRes> getMyFavoriteTabs(Authentication authentication) {
        // 현재 사용자 이메일을 해석
        String currentUserEmail = favoriteTabSupport.resolveCurrentUserEmail(authentication);
        if (currentUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 내 즐겨찾기 탭 목록을 조회
        return ResponseEntity.ok(FavoriteTabsRes.from(getFavoriteTabs.execute(currentUserEmail)));
    }

    @PutMapping("/profile/me/favorites")
    public ResponseEntity<FavoriteTabsRes> updateMyFavoriteTabs(@Valid @RequestBody FavoriteTabsUpdateReq request,
                                                                Authentication authentication) {
        // 현재 사용자 이메일을 해석
        String currentUserEmail = favoriteTabSupport.resolveCurrentUserEmail(authentication);
        if (currentUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 내 즐겨찾기 탭 목록을 교체
        return ResponseEntity.ok(FavoriteTabsRes.from(
                replaceFavoriteTabs.execute(currentUserEmail, request.toFavoriteTabInputs())
        ));
    }
}

package com.quertimizer.favorite.presentation.controller;

import com.quertimizer.favorite.application.input.FavoriteTabsReplaceInput;
import com.quertimizer.favorite.application.usecase.GetFavoriteTabs;
import com.quertimizer.favorite.application.usecase.ReplaceFavoriteTabs;
import com.quertimizer.favorite.presentation.controller.dto.request.FavoriteTabsUpdateReq;
import com.quertimizer.favorite.presentation.controller.dto.response.FavoriteTabsRes;
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

    /**
     * 현재 사용자의 즐겨찾기 탭 목록을 반환한다.
     *
     * <ol>
     *   <li>현재 사용자 이메일 확인
     *   <li>즐겨찾기 탭 목록 응답 생성
     * </ol>
     *
     * @param authentication 현재 요청의 인증 정보
     */
    @GetMapping("/profile/me/favorites")
    public ResponseEntity<FavoriteTabsRes> getMyFavoriteTabs(Authentication authentication) {
        String currentUserEmail = favoriteTabSupport.resolveCurrentUserEmail(authentication);
        if (currentUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(FavoriteTabsRes.from(getFavoriteTabs.execute(currentUserEmail)));
    }

    /**
     * 현재 사용자의 즐겨찾기 탭 목록을 교체한다.
     *
     * <ol>
     *   <li>현재 사용자 이메일 확인
     *   <li>즐겨찾기 탭 교체 후 응답 생성
     * </ol>
     *
     * @param request 교체할 즐겨찾기 탭 목록 요청
     * @param authentication 현재 요청의 인증 정보
     */
    @PutMapping("/profile/me/favorites")
    public ResponseEntity<FavoriteTabsRes> updateMyFavoriteTabs(@Valid @RequestBody FavoriteTabsUpdateReq request,
                                                                Authentication authentication) {
        String currentUserEmail = favoriteTabSupport.resolveCurrentUserEmail(authentication);
        if (currentUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FavoriteTabsReplaceInput input = new FavoriteTabsReplaceInput(currentUserEmail, request.toFavoriteTabInputs());
        return ResponseEntity.ok(FavoriteTabsRes.from(
                replaceFavoriteTabs.execute(input)
        ));
    }
}

package com.quertimizer.favorite.presentation.controller;

import com.quertimizer.favorite.presentation.dto.request.FavoriteTabsUpdateReq;
import com.quertimizer.favorite.presentation.dto.response.FavoriteTabsRes;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.favorite.application.service.FavoriteTabService;
import com.quertimizer.auth.application.service.AuthService;
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

    private final FavoriteTabService favoriteTabService;
    private final AuthService authService;

    @GetMapping("/profile/me/favorites")
    public ResponseEntity<FavoriteTabsRes> getMyFavoriteTabs(Authentication authentication) {
        String currentUserEmail = resolveCurrentUserEmail(authentication);
        if (currentUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(favoriteTabService.getFavoriteTabs(currentUserEmail));
    }

    @PutMapping("/profile/me/favorites")
    public ResponseEntity<FavoriteTabsRes> updateMyFavoriteTabs(@Valid @RequestBody FavoriteTabsUpdateReq request,
                                                                Authentication authentication) {
        String currentUserEmail = resolveCurrentUserEmail(authentication);
        if (currentUserEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(favoriteTabService.replaceFavoriteTabs(currentUserEmail, request));
    }

    private String resolveCurrentUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.findAuthenticatedUser(authentication.getName())
                .map(User::getEmail)
                .orElse(null);
    }
}

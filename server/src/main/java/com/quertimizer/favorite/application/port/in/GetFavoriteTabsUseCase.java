package com.quertimizer.favorite.application.port.in;

import com.quertimizer.favorite.application.output.FavoriteTabOutput;
import com.quertimizer.favorite.application.output.FavoriteTabsOutput;

public interface GetFavoriteTabsUseCase {

    FavoriteTabsOutput execute(String userEmail);
}

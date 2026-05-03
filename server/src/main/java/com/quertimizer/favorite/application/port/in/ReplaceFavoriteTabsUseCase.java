package com.quertimizer.favorite.application.port.in;

import com.quertimizer.favorite.application.input.FavoriteTabInput;
import com.quertimizer.favorite.application.input.FavoriteTabsReplaceInput;
import com.quertimizer.favorite.application.output.FavoriteTabOutput;
import com.quertimizer.favorite.application.output.FavoriteTabsOutput;
import com.quertimizer.favorite.domain.entity.FavoriteTab;

public interface ReplaceFavoriteTabsUseCase {

    FavoriteTabsOutput execute(FavoriteTabsReplaceInput input);
}

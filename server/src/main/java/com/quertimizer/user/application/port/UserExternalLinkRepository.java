package com.quertimizer.user.application.port;

import com.quertimizer.user.domain.entity.UserExternalLink;

import java.util.List;

public interface UserExternalLinkRepository {

    List<UserExternalLink> findAllByIdHandleOrderByIdTypeAscIdLinkAsc(String handle);

    void deleteAllByIdHandle(String handle);

    <S extends UserExternalLink> List<S> saveAll(Iterable<S> userExternalLinks);
}

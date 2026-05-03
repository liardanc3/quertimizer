package com.quertimizer.user.application.port.out;

import com.quertimizer.user.domain.entity.UserExternalLink;

import java.util.List;

public interface UserExternalLinkRepositoryPort {

    List<UserExternalLink> findAllByIdHandleOrderByIdTypeAscIdLinkAsc(String handle);

    void deleteAllByIdHandle(String handle);

    List<UserExternalLink> saveAll(Iterable<UserExternalLink> userExternalLinks);
}

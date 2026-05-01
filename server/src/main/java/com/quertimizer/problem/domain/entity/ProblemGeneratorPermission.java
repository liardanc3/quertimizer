package com.quertimizer.problem.domain.entity;

import com.quertimizer.problem.domain.entity.ids.ProblemGeneratorPermissionId;
import com.quertimizer.user.domain.entity.User;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problem_generator_permission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemGeneratorPermission {

    @EmbeddedId
    private ProblemGeneratorPermissionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handle", referencedColumnName = "handle", insertable = false, updatable = false)
    private User user;

    public static ProblemGeneratorPermission create(String handle, String problemId) {
        // 문제 생성 권한 항목 생성
        return new ProblemGeneratorPermission(ProblemGeneratorPermissionId.create(handle, problemId));
    }

    public String getHandle() {
        // 사용자 handle 조회
        return id.getHandle();
    }

    public String getProblemId() {
        // 문제 번호 조회
        return id.getProblemId();
    }

    private ProblemGeneratorPermission(ProblemGeneratorPermissionId id) {
        this.id = id;
    }

}

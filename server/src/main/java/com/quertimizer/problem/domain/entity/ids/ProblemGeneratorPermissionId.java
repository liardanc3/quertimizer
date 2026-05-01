package com.quertimizer.problem.domain.entity.ids;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProblemGeneratorPermissionId implements Serializable {

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @Column(name = "problem_id", nullable = false, length = 12)
    private String problemId;

    public static ProblemGeneratorPermissionId create(String handle, String problemId) {
        // 문제 생성 권한 식별자 생성
        return new ProblemGeneratorPermissionId(handle, problemId);
    }

}

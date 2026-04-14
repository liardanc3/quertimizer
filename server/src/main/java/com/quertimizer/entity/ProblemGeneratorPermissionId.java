package com.quertimizer.entity;

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

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "problem_id", nullable = false, length = 11)
    private String problemId;

    public static ProblemGeneratorPermissionId create(String userId, String problemId) {
        return new ProblemGeneratorPermissionId(userId, problemId);
    }

}

package com.quertimizer.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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

    public static ProblemGeneratorPermission create(String userId, String problemId) {
        return new ProblemGeneratorPermission(ProblemGeneratorPermissionId.create(userId, problemId));
    }

    public String getUserId() {
        return id.getUserId();
    }

    public String getProblemId() {
        return id.getProblemId();
    }

    private ProblemGeneratorPermission(ProblemGeneratorPermissionId id) {
        this.id = id;
    }

}

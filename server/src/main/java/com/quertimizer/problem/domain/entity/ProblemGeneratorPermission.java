package com.quertimizer.problem.domain.entity;

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

    public static ProblemGeneratorPermission create(String handle, String problemId) {
        return new ProblemGeneratorPermission(ProblemGeneratorPermissionId.create(handle, problemId));
    }

    public String getHandle() {
        return id.getHandle();
    }

    public String getProblemId() {
        return id.getProblemId();
    }

    private ProblemGeneratorPermission(ProblemGeneratorPermissionId id) {
        this.id = id;
    }

}

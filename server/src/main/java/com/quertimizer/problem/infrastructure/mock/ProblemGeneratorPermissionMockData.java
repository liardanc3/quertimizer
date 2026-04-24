package com.quertimizer.problem.infrastructure.mock;

import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("problemGeneratorPermissionMockData")
@DependsOn({"userMockData", "problemMockData"})
@RequiredArgsConstructor
public class ProblemGeneratorPermissionMockData {

    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;

    @PostConstruct
    public void seed() {
        // 기본 문제 생성 권한 Mock 데이터 적재
        problemGeneratorPermissionRepository.saveAll(List.of(
                ProblemGeneratorPermission.create("problemgen01", "NEW"),
                ProblemGeneratorPermission.create("problemgen01", "P00001"),
                ProblemGeneratorPermission.create("problemgen02", "P00001-00001")
        ));
    }

}

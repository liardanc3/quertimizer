package com.quertimizer.mock;

import com.quertimizer.entity.ProblemGeneratorPermission;
import com.quertimizer.repository.ProblemGeneratorPermissionRepository;
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
        problemGeneratorPermissionRepository.saveAll(List.of(
                ProblemGeneratorPermission.create("problemgen01", "NEW"),
                ProblemGeneratorPermission.create("problemgen01", "P00001"),
                ProblemGeneratorPermission.create("problemgen02", "P00001-00001")
        ));
    }

}

package com.speedql.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class HomeController {

    @GetMapping("/test")
    public Map<String, String> test() {
        log.info("프로젝트 연결 테스트");
        return Map.of("result", "프로젝트 연결 테스트");
    }
}
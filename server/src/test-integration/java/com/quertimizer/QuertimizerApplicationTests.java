package com.quertimizer;

import com.quertimizer.user.infrastructure.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class QuertimizerApplicationTests {

	@MockitoBean
	private UserJpaRepository userRepository;

	@Test
	void contextLoads() {
	}

}

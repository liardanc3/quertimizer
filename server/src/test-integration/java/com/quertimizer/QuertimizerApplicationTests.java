package com.quertimizer;

import com.quertimizer.user.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class QuertimizerApplicationTests {

	@MockitoBean
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

}

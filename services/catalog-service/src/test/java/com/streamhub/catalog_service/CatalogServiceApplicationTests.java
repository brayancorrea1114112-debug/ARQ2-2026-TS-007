package com.streamhub.catalog_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Real wiring with the in-memory adapters; they are replaced by the Redis and MongoDB adapters later.
@SpringBootTest(properties = {"jwt.secret=test-only-secret-never-used-in-prod", "jwt.issuer=streamhub-auth"})
@ActiveProfiles("memory")
class CatalogServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}

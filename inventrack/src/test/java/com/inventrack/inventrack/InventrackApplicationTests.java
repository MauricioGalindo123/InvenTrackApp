package com.inventrack.inventrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "mobile.enabled=true", "mobile.cookie-secure=false",
    "spring.datasource.url=jdbc:h2:mem:contexto;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false"
})
class InventrackApplicationTests {

	@Test
	void contextLoads() {
	}

}

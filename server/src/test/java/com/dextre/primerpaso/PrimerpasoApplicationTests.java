package com.dextre.primerpaso;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:postgresql://localhost:1/prueba",
        "spring.datasource.username=prueba",
        "spring.datasource.password=prueba",
        "primerpaso.bd.comprobar-al-iniciar=false"
})
class PrimerpasoApplicationTests {

	@Test
	void cargaElContextoSinDependerDePostgresql() {
	}

}

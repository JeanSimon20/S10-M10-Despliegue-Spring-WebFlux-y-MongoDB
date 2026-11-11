package pe.edu.vallegrande.webfluxk8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Clase principal del microservicio reactivo con Spring WebFlux y MongoDB.
 * Ejecuta sobre un servidor embebido Netty utilizando el modelo Event Loop no bloqueante.
 */
@SpringBootApplication
@EnableReactiveMongoRepositories
public class WebfluxMongoK8sApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebfluxMongoK8sApplication.class, args);
    }
}


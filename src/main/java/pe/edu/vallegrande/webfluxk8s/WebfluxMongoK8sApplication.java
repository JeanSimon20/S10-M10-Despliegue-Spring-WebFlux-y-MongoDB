package pe.edu.vallegrande.webfluxk8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class WebfluxMongoK8sApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebfluxMongoK8sApplication.class, args);
    }
}

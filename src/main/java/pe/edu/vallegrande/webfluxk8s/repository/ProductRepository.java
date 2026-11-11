package pe.edu.vallegrande.webfluxk8s.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.webfluxk8s.model.Product;
import reactor.core.publisher.Flux;

@Repository
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {

    Flux<Product> findByCategoryIgnoreCase(String category);

    Flux<Product> findByActiveTrue();
}

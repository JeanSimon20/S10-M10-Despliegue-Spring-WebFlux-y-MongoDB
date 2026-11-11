package pe.edu.vallegrande.webfluxk8s.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import pe.edu.vallegrande.webfluxk8s.model.Product;
import reactor.core.publisher.Flux;

/**
 * Repositorio Reactivo para MongoDB.
 * Hereda de ReactiveMongoRepository para ejecutar operaciones de E/S asíncronas
 * y no bloqueantes sobre el driver reactivo de MongoDB (MongoDB Reactive Streams Driver).
 */
@Repository
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {

    /**
     * Retorna un flujo reactivo (0..N elementos) filtrando por categoría sin importar mayúsculas/minúsculas.
     */
    Flux<Product> findByCategoryIgnoreCase(String category);

    /**
     * Retorna únicamente los productos con estado activo = true.
     */
    Flux<Product> findByActiveTrue();
}


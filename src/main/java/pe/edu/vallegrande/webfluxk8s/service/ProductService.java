package pe.edu.vallegrande.webfluxk8s.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.webfluxk8s.model.Product;
import pe.edu.vallegrande.webfluxk8s.repository.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initDemoData() {
        productRepository.count()
                .filter(count -> count == 0)
                .flatMapMany(count -> {
                    log.info("Coleccion 'products' vacia. Registrando datos iniciales en MongoDB...");
                    List<Product> initialProducts = List.of(
                            Product.builder()
                                    .name("Cluster Kubernetes Empresarial (EKS)")
                                    .category("Cloud Infrastructure")
                                    .price(450.00)
                                    .stock(15)
                                    .active(true)
                                    .createdAt(LocalDateTime.now())
                                    .build(),
                            Product.builder()
                                    .name("Licencia Spring Cloud Gateway Reactivo")
                                    .category("Microservices")
                                    .price(220.50)
                                    .stock(40)
                                    .active(true)
                                    .createdAt(LocalDateTime.now())
                                    .build(),
                            Product.builder()
                                    .name("Nodo MongoDB Atlas Replica Set")
                                    .category("Database NoSQL")
                                    .price(310.00)
                                    .stock(25)
                                    .active(true)
                                    .createdAt(LocalDateTime.now())
                                    .build(),
                            Product.builder()
                                    .name("Scanner DevSecOps Trivy & SonarQube")
                                    .category("DevSecOps")
                                    .price(180.00)
                                    .stock(30)
                                    .active(true)
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    );
                    return productRepository.saveAll(initialProducts);
                })
                .subscribe(
                        product -> log.info("Producto registrado: {} (ID: {})", product.getName(), product.getId()),
                        error -> log.warn("No se pudo inicializar el catalogo inicial: {}", error.getMessage())
                );
    }

    public Flux<Product> findAll() {
        return productRepository.findAll();
    }

    public Flux<Product> findAllActive() {
        return productRepository.findByActiveTrue();
    }

    public Mono<Product> findById(String id) {
        return productRepository.findById(id);
    }

    public Flux<Product> findByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category);
    }

    public Mono<Product> create(Product product) {
        product.setId(null);
        if (product.getActive() == null) {
            product.setActive(true);
        }
        product.setCreatedAt(LocalDateTime.now());
        return productRepository.save(product)
                .doOnSuccess(saved -> log.info("Producto creado en MongoDB con ID: {}", saved.getId()));
    }

    public Mono<Product> update(String id, Product updatedProduct) {
        return productRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(updatedProduct.getName());
                    existing.setCategory(updatedProduct.getCategory());
                    existing.setPrice(updatedProduct.getPrice());
                    existing.setStock(updatedProduct.getStock());
                    if (updatedProduct.getActive() != null) {
                        existing.setActive(updatedProduct.getActive());
                    }
                    return productRepository.save(existing);
                });
    }

    public Mono<Boolean> deleteById(String id) {
        return productRepository.findById(id)
                .flatMap(existing -> productRepository.delete(existing).thenReturn(true))
                .defaultIfEmpty(false);
    }
}

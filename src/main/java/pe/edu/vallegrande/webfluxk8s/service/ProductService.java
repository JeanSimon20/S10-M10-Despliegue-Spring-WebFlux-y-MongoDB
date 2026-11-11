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

/**
 * Capa de Servicio Reactiva.
 * Orquesta los flujos de datos no bloqueantes utilizando tipos reactivos de Project Reactor:
 * - Mono<T>: Representa un flujo asíncrono de 0 o 1 elemento.
 * - Flux<T>: Representa un flujo asíncrono de 0 a N elementos con soporte de Backpressure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Inicializa datos de demostración en MongoDB de forma reactiva si la colección está vacía.
     * Ideal para que durante la demostración en Kubernetes el endpoint GET ya retorne datos reales.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initDemoData() {
        productRepository.count()
                .filter(count -> count == 0)
                .flatMapMany(count -> {
                    log.info("🌱 Colección 'products' vacía. Insertando catálogo inicial reactivo en MongoDB...");
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
                        product -> log.info("✅ Producto inicializado en MongoDB: {} (ID: {})", product.getName(), product.getId()),
                        error -> log.warn("⚠️ No se pudo inicializar el catálogo inicial (verificar conexión a MongoDB): {}", error.getMessage())
                );
    }

    /**
     * Obtiene todos los productos en un flujo no bloqueante Flux<Product>.
     */
    public Flux<Product> findAll() {
        log.debug("Consultando todos los productos de forma reactiva en hilo: {}", Thread.currentThread().getName());
        return productRepository.findAll();
    }

    /**
     * Obtiene únicamente los productos activos.
     */
    public Flux<Product> findAllActive() {
        return productRepository.findByActiveTrue();
    }

    /**
     * Busca un producto por su ID en MongoDB retornando un Mono<Product>.
     */
    public Mono<Product> findById(String id) {
        return productRepository.findById(id);
    }

    /**
     * Filtra productos por categoría de forma reactiva.
     */
    public Flux<Product> findByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category);
    }

    /**
     * Registra un nuevo producto asignando metadatos por defecto.
     */
    public Mono<Product> create(Product product) {
        product.setId(null);
        if (product.getActive() == null) {
            product.setActive(true);
        }
        product.setCreatedAt(LocalDateTime.now());
        return productRepository.save(product)
                .doOnSuccess(saved -> log.info("📦 Producto creado exitosamente en MongoDB con ID: {}", saved.getId()));
    }

    /**
     * Actualiza un producto existente combinando operadores reactivos (flatMap).
     */
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

    /**
     * Eliminación lógica (soft delete) cambiando el estado a inactivo.
     */
    public Mono<Boolean> deleteById(String id) {
        return productRepository.findById(id)
                .flatMap(existing -> productRepository.delete(existing).thenReturn(true))
                .defaultIfEmpty(false);
    }
}


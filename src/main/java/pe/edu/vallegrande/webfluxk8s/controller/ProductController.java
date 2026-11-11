package pe.edu.vallegrande.webfluxk8s.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.webfluxk8s.model.Product;
import pe.edu.vallegrande.webfluxk8s.service.ProductService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador REST Reactivo (Spring WebFlux).
 * Atiende peticiones HTTP sobre Netty utilizando hilos de Event Loop no bloqueantes.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_NAME:vallegrande_reactive_db}")
    private String dbName;

    @Value("${APP_PORT:8080}")
    private String appPort;

    /**
     * Endpoint de diagnóstico para demostrar en vivo la inyección del ConfigMap
     * y el hilo Event Loop de Netty que procesa la solicitud sin bloqueo.
     */
    @GetMapping("/info")
    public Mono<Map<String, Object>> getClusterAndRuntimeInfo() {
        return Mono.fromCallable(() -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("service", "Spring WebFlux + MongoDB Reactive Microservice");
            info.put("podHostname", System.getenv().getOrDefault("HOSTNAME", "local-dev-machine"));
            info.put("reactiveEventLoopThread", Thread.currentThread().getName());
            info.put("injectedConfigMap", Map.of(
                    "DB_HOST", dbHost,
                    "DB_NAME", dbName,
                    "APP_PORT", appPort
            ));
            info.put("timestamp", LocalDateTime.now().toString());
            return info;
        });
    }

    /**
     * Lista todos los productos almacenados en MongoDB de manera no bloqueante.
     */
    @GetMapping
    public Flux<Product> getAllProducts(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return productService.findByCategory(category);
        }
        return productService.findAll();
    }

    /**
     * Demostración de Server-Sent Events (SSE) con Project Reactor.
     * Emite cada producto como un evento en tiempo real cada 1 segundo sin bloquear el hilo de Netty.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Product> streamProductsReactive() {
        return productService.findAll()
                .delayElements(Duration.ofSeconds(1));
    }

    /**
     * Obtiene un producto por su identificador único de MongoDB.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Product>> getProductById(@PathVariable String id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Crea un nuevo documento de producto en MongoDB de forma reactiva.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> createProduct(@Valid @RequestBody Product product) {
        return productService.create(product);
    }

    /**
     * Actualiza un producto existente por su ID.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Product>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody Product product) {
        return productService.update(id, product)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Elimina un producto por su ID.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return productService.deleteById(id)
                .map(deleted -> deleted
                        ? ResponseEntity.noContent().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }
}


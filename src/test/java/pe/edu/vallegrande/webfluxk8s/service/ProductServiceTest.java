package pe.edu.vallegrande.webfluxk8s.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.vallegrande.webfluxk8s.model.Product;
import pe.edu.vallegrande.webfluxk8s.repository.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias reactivas con StepVerifier (Project Reactor Test).
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Debe retornar un flujo reactivo (Flux) con todos los productos")
    void shouldReturnAllProductsReactively() {
        Product p1 = Product.builder().id("1").name("Cluster K8s").category("Cloud").price(450.0).stock(10).active(true).build();
        Product p2 = Product.builder().id("2").name("MongoDB Atlas").category("NoSQL").price(310.0).stock(5).active(true).build();

        when(productRepository.findAll()).thenReturn(Flux.just(p1, p2));

        StepVerifier.create(productService.findAll())
                .expectSubscription()
                .expectNext(p1)
                .expectNext(p2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Debe crear un producto de forma reactiva retornando un Mono")
    void shouldCreateProductReactively() {
        Product input = Product.builder().name("Spring WebFlux Pro").category("Backend").price(199.99).stock(20).build();
        Product saved = Product.builder().id("abc-123").name("Spring WebFlux Pro").category("Backend").price(199.99).stock(20).active(true).build();

        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(productService.create(input))
                .expectSubscription()
                .expectNextMatches(p -> "abc-123".equals(p.getId()) && Boolean.TRUE.equals(p.getActive()))
                .verifyComplete();
    }
}


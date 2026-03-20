package com.cyna.modules.product.application.query.getbyid;

import com.cyna.modules.product.domain.model.Product;
import com.cyna.modules.product.domain.model.ProductCategory;
import com.cyna.modules.product.domain.model.ProductPriority;
import com.cyna.modules.product.domain.repository.ProductRepository;
import com.cyna.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductByIdQueryHandlerTest {

    @Mock
    private ProductRepository productRepository;

    private GetProductByIdQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetProductByIdQueryHandler(productRepository);
    }

    @Test
    void should_return_product_read_model() {
        Product product = Product.create(
                "EDR Pro",
                ProductCategory.EDR,
                ProductPriority.MOYENNE,
                "Endpoint detection service",
                "Behavioral analysis",
                Money.of(199.99, "EUR"),
                Money.of(1999.99, "EUR")
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        ProductReadModel result = handler.handle(new GetProductByIdQuery(product.getId()));

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(product.getId());
        assertThat(result.name()).isEqualTo("EDR Pro");
        assertThat(result.category()).isEqualTo("EDR");
        assertThat(result.priority()).isEqualTo("MOYENNE");
    }

    @Test
    void should_return_null_when_not_found() {
        var id = java.util.UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        ProductReadModel result = handler.handle(new GetProductByIdQuery(id));

        assertThat(result).isNull();
    }
}

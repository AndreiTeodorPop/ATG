package com.castorama.atg.pipeline;

import com.castorama.atg.domain.enums.ProductCategory;
import com.castorama.atg.domain.model.CartItem;
import com.castorama.atg.domain.model.Product;
import com.castorama.atg.domain.model.User;
import com.castorama.atg.pipeline.processor.*;
import com.castorama.atg.repository.OrderRepository;
import com.castorama.atg.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the checkout pipeline.
 * ATG analogy: pipeline processor unit tests using mocked Nucleus components.
 */
class CheckoutPipelineTest {

    private User testUser;
    private Product testProduct;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("jean.dupont@example.fr")
                .login("jean.dupont")
                .passwordHash("$2a$12$hash")
                .role("ROLE_CUSTOMER")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .skuCode("CAST-10001")
                .name("Perceuse BOSCH")
                .listPrice(new BigDecimal("149.99"))
                .category(ProductCategory.OUTILLAGE)
                .stockQuantity(10)
                .active(true)
                .build();

        cartItem = CartItem.builder()
                .id(1L)
                .user(testUser)
                .product(testProduct)
                .quantity(2)
                .unitPrice(testProduct.getListPrice())
                .build();
    }

    @Test
    @DisplayName("ValidateCartProcessor halts pipeline when cart is empty")
    void validateCart_empty_shouldStopChain() {
        PipelineContext ctx = PipelineContext.builder()
                .user(testUser)
                .cartItems(List.of())
                .shippingAddress("12 rue de la Paix, Paris")
                .paymentMethod("CARTE_BANCAIRE")
                .build();

        new ValidateCartProcessor().process(ctx);

        assertThat(ctx.isStopChain()).isTrue();
        assertThat(ctx.getErrors()).hasSize(1);
        assertThat(ctx.getErrors().get(0)).contains("panier est vide");
    }

    @Test
    @DisplayName("InventoryCheckProcessor halts pipeline when stock insufficient")
    void inventoryCheck_insufficientStock_shouldStopChain() {
        testProduct.setStockQuantity(1);  // only 1 in stock
        cartItem = CartItem.builder()
                .id(1L).user(testUser).product(testProduct)
                .quantity(5)             // requesting 5
                .unitPrice(testProduct.getListPrice())
                .build();

        ProductRepository mockRepo = mock(ProductRepository.class);
        when(mockRepo.findBySkuCode("CAST-10001")).thenReturn(Optional.of(testProduct));

        PipelineContext ctx = PipelineContext.builder()
                .user(testUser)
                .cartItems(List.of(cartItem))
                .shippingAddress("12 rue de la Paix, Paris")
                .paymentMethod("CARTE_BANCAIRE")
                .build();

        new InventoryCheckProcessor(mockRepo).process(ctx);

        assertThat(ctx.isStopChain()).isTrue();
        assertThat(ctx.getErrors().get(0)).contains("Stock insuffisant");
    }

    @Test
    @DisplayName("PaymentAuthProcessor declines payment for REFUSE_TEST trigger")
    void paymentAuth_refuseTest_shouldStopChain() {
        PipelineContext ctx = PipelineContext.builder()
                .user(testUser)
                .cartItems(List.of(cartItem))
                .shippingAddress("12 rue de la Paix, Paris")
                .paymentMethod("REFUSE_TEST")
                .build();

        // Must have an order already created by PriceCalculationProcessor
        new PriceCalculationProcessor().process(ctx);
        assertThat(ctx.isStopChain()).isFalse();  // price calc should succeed

        ctx.setStopChain(false);  // reset to test payment in isolation
        new PaymentAuthProcessor().process(ctx);

        assertThat(ctx.isStopChain()).isTrue();
        assertThat(ctx.getErrors().get(0)).contains("Paiement refusé");
    }

    @Test
    @DisplayName("Full pipeline succeeds with valid cart and payment")
    void fullPipeline_happyPath_shouldSucceed() {
        ProductRepository mockProductRepo = mock(ProductRepository.class);
        when(mockProductRepo.save(any())).thenReturn(testProduct);

        OrderRepository mockOrderRepo = mock(OrderRepository.class);
        when(mockOrderRepo.save(any())).thenAnswer(inv -> {
            com.castorama.atg.domain.model.Order order = inv.getArgument(0);
            order.setId(100L);
            return order;
        });

        CheckoutPipeline pipeline = new CheckoutPipeline(List.of(
                new ValidateCartProcessor(),
                new InventoryCheckProcessor(mockProductRepo),
                new PriceCalculationProcessor(),
                new PaymentAuthProcessor(),
                new ReserveInventoryProcessor(mockProductRepo),
                new FinaliseOrderProcessor(mockOrderRepo)
        ));

        PipelineContext ctx = PipelineContext.builder()
                .user(testUser)
                .cartItems(List.of(cartItem))
                .shippingAddress("12 rue de la Paix, 75001 Paris")
                .paymentMethod("CARTE_BANCAIRE")
                .build();

        pipeline.execute(ctx);

        assertThat(ctx.isSuccess()).isTrue();
        assertThat(ctx.hasErrors()).isFalse();
        assertThat(ctx.getOrder()).isNotNull();
        assertThat(ctx.getOrder().getOrderNumber()).startsWith("CAST-");
        assertThat(ctx.getOrder().getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
    }
}

package com.castorama.atg.domain.model;

import com.castorama.atg.domain.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ATG analogy: the {@code order} item descriptor in the
 * {@code /atg/commerce/order/OrderRepository}.
 *
 * <p>In ATG, an Order transitions through a state machine managed by
 * {@code OrderManager} and driven by the checkout pipeline.  That lifecycle
 * is replicated here via {@link com.castorama.atg.pipeline.CheckoutPipeline}
 * and {@link com.castorama.atg.service.OrderService}.</p>
 *
 * <p>ATG table: {@code atg_commerce_order} / {@code atg_commerce_item}.</p>
 */
@Entity
@Table(name = "atg_commerce_order",
       indexes = {
           @Index(name = "idx_order_user",   columnList = "user_id"),
           @Index(name = "idx_order_number", columnList = "order_number")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ATG: order.id — human-readable order reference sent in confirmation emails.
     * Format: CAST-YYYYMMDD-{seq}
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    /** ATG: order.profileId reference. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_user"))
    private User user;

    /**
     * ATG: order.stateAsString — driven by OrderManager.processOrder().
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /** ATG: order.priceInfo.total — grand total including tax. */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** ATG: order.priceInfo.tax (French TVA 20%). */
    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** ATG: order.priceInfo.shipping. */
    @Column(name = "shipping_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    /**
     * ATG: order.shippingGroups[0].shippingAddress.
     * Simplified to a single formatted address string.
     */
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    /**
     * ATG: order.paymentGroups[0].creditCardNumber (masked).
     * Simplified to a payment method label for this demo.
     */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /** ATG: order.submittedDate. */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}

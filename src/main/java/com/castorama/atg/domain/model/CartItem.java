package com.castorama.atg.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ATG analogy: a {@code CommerceItem} within the {@code Order} repository item.
 *
 * <p>In ATG, commerce items are children of the Order object, stored in the
 * {@code atg_commerce_order} / {@code atg_commerce_item} tables.  Because we persist
 * the cart independently of a submitted order in this demo, CartItem exists as its own
 * entity linked to the user profile — equivalent to ATG's session-scoped
 * {@code ShoppingCart} component holding un-submitted commerce items.</p>
 *
 * <p>On checkout the items are transferred into an {@link Order} via the
 * {@link com.castorama.atg.pipeline.CheckoutPipeline}.</p>
 */
@Entity
@Table(name = "atg_cart_item",
       indexes = {
           @Index(name = "idx_cart_user",    columnList = "user_id"),
           @Index(name = "idx_cart_product", columnList = "product_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ATG: CommerceItem.auxiliaryData.profileRef — link back to the owning profile.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cart_user"))
    private User user;

    /** ATG: CommerceItem.catalogRefId / SKU reference. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cart_product"))
    private Product product;

    /**
     * ATG: CommerceItem.quantity.
     * Must be at least 1 — ATG enforces this in {@code CartModifierFormHandler}.
     */
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    /**
     * ATG: CommerceItem.priceInfo.listPrice — price at the time of adding to cart.
     * Snapshotting prevents price drift if the catalogue price changes mid-session.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }

    /**
     * ATG analogy: CommerceItem.priceInfo.amount — computed line total.
     */
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

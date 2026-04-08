package com.castorama.atg.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * ATG analogy: a submitted {@code CommerceItem} within a confirmed Order.
 *
 * <p>Unlike {@link CartItem} (which references the live Product entity),
 * OrderItem snapshots the product name and price at submission time.  This
 * matches ATG's behaviour: once an order is submitted the commerce item holds
 * the priceInfo values independently of any subsequent catalogue price changes.</p>
 */
@Entity
@Table(name = "atg_order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_order_item_order"))
    private Order order;

    /** Soft reference — product may be delisted but order history must survive. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",
                foreignKey = @ForeignKey(name = "fk_order_item_product"))
    private Product product;

    /** Snapshot: ATG commerceItem.auxiliaryData.productRef.displayName. */
    @NotBlank
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    /** Snapshot: ATG commerceItem.auxiliaryData.catalogRefId (sku code). */
    @NotBlank
    @Column(name = "sku_code", nullable = false, length = 30)
    private String skuCode;

    /** ATG: commerceItem.quantity. */
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    /** ATG: commerceItem.priceInfo.listPrice — snapshot at order submission. */
    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

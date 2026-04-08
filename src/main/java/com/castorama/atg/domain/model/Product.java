package com.castorama.atg.domain.model;

import com.castorama.atg.domain.enums.ProductCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ATG analogy: a {@code product} item descriptor in the ATG catalog repository
 * (typically defined in {@code /atg/commerce/catalog/ProductCatalog.xml}).
 *
 * <p>In a real ATG deployment, products live in the {@code atg_catalog} database schema
 * with item descriptors for {@code product}, {@code sku}, and {@code category}.
 * This entity conflates product + SKU for simplicity — a single purchasable unit
 * as Castorama sells most DIY items as single reference codes (codes articles).</p>
 *
 * <p>ATG pricing would be managed by the PricingEngine and price lists; here price
 * is a direct property on the product, with a separate {@code salePrice} to simulate
 * promotional pricing.</p>
 */
@Entity
@Table(name = "atg_catalog_product",
       indexes = {
           @Index(name = "idx_product_sku",      columnList = "sku_code"),
           @Index(name = "idx_product_category", columnList = "category")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /** ATG: product.repositoryId — internal numeric surrogate. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ATG: sku.repositoryId visible to the customer — e.g. "CAST-12345".
     * Castorama's real product codes follow this pattern.
     */
    @NotBlank
    @Column(name = "sku_code", nullable = false, unique = true, length = 30)
    private String skuCode;

    /** ATG: product.displayName. */
    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    /** ATG: product.longDescription. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** ATG: sku.listPrice (in EUR). */
    @NotNull
    @DecimalMin("0.00")
    @Column(name = "list_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal listPrice;

    /**
     * ATG: promotional price set by PricingEngine / PromotionManager.
     * Null when no promotion is active.
     */
    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    /** ATG: product.thumbnailImage URL or content repository path. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * ATG: product.parentCategory — simplified to a single enum value.
     * A real ATG catalog supports a multi-level category hierarchy.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ProductCategory category;

    /** ATG: sku.availabilityStatus mapped to a stock count. */
    @Min(0)
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    /** ATG: product.brand — custom property on the item descriptor. */
    @Column(length = 100)
    private String brand;

    /** ATG: product.active — controls visibility in catalog queries. */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

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

    /**
     * ATG PricingEngine analogy: return the effective price for display,
     * preferring the promotional sale price when set.
     */
    public BigDecimal getEffectivePrice() {
        return (salePrice != null) ? salePrice : listPrice;
    }

    public boolean isOnSale() {
        return salePrice != null && salePrice.compareTo(listPrice) < 0;
    }

    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }
}

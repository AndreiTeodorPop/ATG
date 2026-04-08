package com.castorama.atg.dto.response;

import com.castorama.atg.domain.enums.ProductCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ATG analogy: a catalog product view projected from the product/sku
 * item descriptors, as would be rendered by a Droplet or REST handler.
 */
@Data
@Builder
public class ProductResponse {
    private Long id;
    private String skuCode;
    private String name;
    private String description;
    private BigDecimal listPrice;
    private BigDecimal salePrice;
    private BigDecimal effectivePrice;
    private boolean onSale;
    private boolean inStock;
    private Integer stockQuantity;
    private String imageUrl;
    private ProductCategory category;
    private String categoryDisplayName;
    private String brand;
}

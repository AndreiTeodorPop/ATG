package com.castorama.atg.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ATG analogy: CommerceItem rendered in a cart summary Droplet.
 */
@Data
@Builder
public class CartItemResponse {
    private Long id;
    private String skuCode;
    private String productName;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private LocalDateTime addedAt;
}

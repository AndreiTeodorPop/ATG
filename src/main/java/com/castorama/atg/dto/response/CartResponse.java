package com.castorama.atg.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full cart summary — ATG analogy: the ShoppingCart component's
 * rendered state including priceInfo totals.
 */
@Data
@Builder
public class CartResponse {
    private List<CartItemResponse> items;
    private int itemCount;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;      // TVA 20%
    private BigDecimal total;
}

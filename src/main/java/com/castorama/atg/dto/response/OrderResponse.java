package com.castorama.atg.dto.response;

import com.castorama.atg.domain.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ATG analogy: Order item descriptor rendered in the order confirmation
 * or order history page Droplet.
 */
@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private String shippingAddress;
    private String paymentMethod;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}

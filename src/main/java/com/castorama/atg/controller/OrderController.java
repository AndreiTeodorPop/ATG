package com.castorama.atg.controller;

import com.castorama.atg.dto.request.CheckoutRequest;
import com.castorama.atg.dto.response.OrderResponse;
import com.castorama.atg.dto.response.PagedResponse;
import com.castorama.atg.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Order and checkout endpoints — all require authentication.
 *
 * <p>ATG analogy:
 * <ul>
 *   <li>POST /checkout → CheckoutFormHandler.checkout() triggering the pipeline</li>
 *   <li>GET  /         → OrderLookupDroplet / order history page</li>
 *   <li>GET  /{number} → OrderDetailsDroplet / order detail page</li>
 *   <li>POST /{number}/cancel → OrderManager.updateOrderState(REMOVED)</li>
 * </ul>
 * </p>
 *
 * <p>Base path: {@code /api/v1/orders}</p>
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Submit checkout — triggers the full checkout pipeline.
     *
     * <pre>
     * POST /api/v1/orders/checkout
     * Authorization: Bearer {token}
     * {
     *   "shippingAddress": "12 rue de la Paix, 75001 Paris",
     *   "paymentMethod": "CARTE_BANCAIRE"
     * }
     * </pre>
     *
     * @return 201 Created with confirmed order
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse order = orderService.checkout(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Get the authenticated user's order history.
     *
     * <pre>
     * GET /api/v1/orders?page=0&size=10
     * Authorization: Bearer {token}
     * </pre>
     */
    @GetMapping
    public ResponseEntity<PagedResponse<OrderResponse>> getOrderHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                orderService.getOrderHistory(userDetails.getUsername(), page, size));
    }

    /**
     * Get a specific order by order number.
     *
     * <pre>
     * GET /api/v1/orders/CAST-20260408-1001
     * Authorization: Bearer {token}
     * </pre>
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(
                orderService.getOrder(userDetails.getUsername(), orderNumber));
    }

    /**
     * Cancel a pending or confirmed order.
     *
     * <pre>
     * POST /api/v1/orders/CAST-20260408-1001/cancel
     * Authorization: Bearer {token}
     * </pre>
     */
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(
                orderService.cancelOrder(userDetails.getUsername(), orderNumber));
    }
}

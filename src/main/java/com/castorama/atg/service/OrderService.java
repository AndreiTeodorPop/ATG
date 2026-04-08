package com.castorama.atg.service;

import com.castorama.atg.domain.enums.OrderStatus;
import com.castorama.atg.domain.model.CartItem;
import com.castorama.atg.domain.model.Order;
import com.castorama.atg.domain.model.User;
import com.castorama.atg.dto.request.CheckoutRequest;
import com.castorama.atg.dto.response.OrderItemResponse;
import com.castorama.atg.dto.response.OrderResponse;
import com.castorama.atg.dto.response.PagedResponse;
import com.castorama.atg.exception.BusinessException;
import com.castorama.atg.exception.ResourceNotFoundException;
import com.castorama.atg.pipeline.CheckoutPipeline;
import com.castorama.atg.pipeline.PipelineContext;
import com.castorama.atg.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Nucleus-style component: Order Management Service.
 *
 * <p>ATG analogy: a global-scope Nucleus component wrapping
 * {@code /atg/commerce/order/OrderManager} and
 * {@code /atg/commerce/checkout/CheckoutFormHandler}.
 * The checkout pipeline call mirrors ATG's
 * {@code CheckoutFormHandler.checkout()} triggering
 * {@code PipelineManager.runProcess("checkout", pipelineArgs)}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final UserService userService;
    private final CheckoutPipeline checkoutPipeline;

    /**
     * Execute checkout — the primary order submission flow.
     *
     * <p>ATG analogy: CheckoutFormHandler.checkout() which populates the pipeline
     * HashMap and calls PipelineManager.runProcess("checkoutPipeline", args).</p>
     *
     * @throws BusinessException if the pipeline returns errors (empty cart,
     *                           insufficient stock, payment declined, etc.)
     */
    @Transactional
    public OrderResponse checkout(String userEmail, CheckoutRequest request) {
        User user = userService.findByEmail(userEmail);
        List<CartItem> cartItems = cartService.getCartItems(user);

        // Build the pipeline context — ATG: populate the pipeline HashMap
        PipelineContext ctx = PipelineContext.builder()
                .user(user)
                .cartItems(cartItems)
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(request.getPaymentMethod())
                .build();

        // Run the pipeline — ATG: PipelineManager.runProcess()
        checkoutPipeline.execute(ctx);

        if (!ctx.isSuccess()) {
            // Aggregate all pipeline errors into a single exception message
            String errorMsg = String.join(" | ", ctx.getErrors());
            throw new BusinessException("CHECKOUT_FAILED", errorMsg);
        }

        // Clear the cart after successful order submission
        // ATG: CartModifierFormHandler.removeAllItemsFromOrder() post-checkout
        cartService.clearCart(user);

        return toResponse(ctx.getOrder());
    }

    /**
     * Get a user's order history.
     * ATG: OrderRepository.getOrderIdsForProfile().
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrderHistory(String userEmail, int page, int size) {
        User user = userService.findByEmail(userEmail);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return PagedResponse.<OrderResponse>builder()
                .content(orderPage.getContent().stream().map(OrderService::toResponse).toList())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .last(orderPage.isLast())
                .build();
    }

    /**
     * Get a specific order by order number, verifying it belongs to the requesting user.
     * ATG: OrderRepository.getOrder(orderId) with profile ownership check.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String userEmail, String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "orderNumber", orderNumber));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException("ACCESS_DENIED",
                    "Vous n'êtes pas autorisé à accéder à cette commande.");
        }
        return toResponse(order);
    }

    /**
     * Cancel an order — only allowed for PENDING/CONFIRMED orders.
     * ATG: OrderManager.updateOrderState() → REMOVED.
     */
    @Transactional
    public OrderResponse cancelOrder(String userEmail, String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "orderNumber", orderNumber));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException("ACCESS_DENIED",
                    "Vous n'êtes pas autorisé à annuler cette commande.");
        }

        if (order.getStatus() == OrderStatus.SHIPPED ||
            order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("CANNOT_CANCEL",
                    "Les commandes expédiées ou livrées ne peuvent pas être annulées.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("ALREADY_CANCELLED", "Cette commande est déjà annulée.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        log.info("Order {} cancelled by user {}", orderNumber, userEmail);
        return toResponse(order);
    }

    // ---- Mapping ----

    public static OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .items(order.getItems().stream().map(item ->
                        OrderItemResponse.builder()
                                .id(item.getId())
                                .skuCode(item.getSkuCode())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .lineTotal(item.getLineTotal())
                                .build()).toList())
                .totalAmount(order.getTotalAmount())
                .taxAmount(order.getTaxAmount())
                .shippingAmount(order.getShippingAmount())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .submittedAt(order.getSubmittedAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}

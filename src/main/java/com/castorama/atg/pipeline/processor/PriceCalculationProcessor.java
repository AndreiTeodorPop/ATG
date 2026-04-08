package com.castorama.atg.pipeline.processor;

import com.castorama.atg.domain.model.CartItem;
import com.castorama.atg.domain.model.Order;
import com.castorama.atg.domain.model.OrderItem;
import com.castorama.atg.domain.enums.OrderStatus;
import com.castorama.atg.pipeline.PipelineContext;
import com.castorama.atg.pipeline.PipelineProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pipeline Step 3 — Calculate order totals and construct the Order entity.
 *
 * <p>ATG analogy: the ATG PricingEngine and its sub-calculators
 * ({@code ItemPriceCalculator}, {@code OrderPriceCalculator},
 * {@code TaxCalculator}, {@code ShippingPriceCalculator}).
 * This single processor covers all price calculation for demo purposes;
 * a production system would split these into separate processors.</p>
 *
 * <p>French TVA (VAT) rate: 20% standard rate applied to all DIY goods.
 * Shipping: free above €75 (Castorama real-world threshold), €9.99 below.</p>
 */
@Component
@org.springframework.core.annotation.Order(30)
@Slf4j
public class PriceCalculationProcessor implements PipelineProcessor {

    private static final BigDecimal TVA_RATE       = new BigDecimal("0.20");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("75.00");
    private static final BigDecimal SHIPPING_COST  = new BigDecimal("9.99");
    private static final AtomicLong ORDER_SEQ       = new AtomicLong(1000);

    @Override
    public void process(PipelineContext ctx) {
        if (ctx.isStopChain()) return;
        log.debug("[Pipeline] {} executing", getName());

        // Build order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(ctx.getUser())
                .status(OrderStatus.PENDING)
                .shippingAddress(ctx.getShippingAddress())
                .paymentMethod(ctx.getPaymentMethod())
                .submittedAt(LocalDateTime.now())
                .build();

        // Convert cart items to order items (snapshot prices)
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : ctx.getCartItems()) {
            BigDecimal unitPrice = cartItem.getProduct().getEffectivePrice();
            OrderItem orderItem = OrderItem.builder()
                    .product(cartItem.getProduct())
                    .productName(cartItem.getProduct().getName())
                    .skuCode(cartItem.getProduct().getSkuCode())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .build();
            order.addItem(orderItem);
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // Shipping: free above threshold
        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : SHIPPING_COST;

        // TVA on subtotal + shipping
        BigDecimal taxBase = subtotal.add(shipping);
        BigDecimal tax = taxBase.multiply(TVA_RATE).setScale(2, RoundingMode.HALF_UP);

        // Grand total (prices are HT — adding TVA makes it TTC)
        BigDecimal total = taxBase.add(tax).setScale(2, RoundingMode.HALF_UP);

        order.setShippingAmount(shipping);
        order.setTaxAmount(tax);
        order.setTotalAmount(total);

        ctx.setOrder(order);
        log.info("[Pipeline] {} — order {} calculated: subtotal={} shipping={} tax={} total={}",
                getName(), order.getOrderNumber(), subtotal, shipping, tax, total);
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("CAST-%s-%04d", date, ORDER_SEQ.getAndIncrement());
    }
}

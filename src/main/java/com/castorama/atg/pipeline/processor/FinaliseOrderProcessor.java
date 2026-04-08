package com.castorama.atg.pipeline.processor;

import com.castorama.atg.domain.enums.OrderStatus;
import com.castorama.atg.domain.model.Order;
import com.castorama.atg.pipeline.PipelineContext;
import com.castorama.atg.pipeline.PipelineProcessor;
import com.castorama.atg.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Pipeline Step 6 — Persist the order and transition it to CONFIRMED.
 *
 * <p>ATG analogy: {@code /atg/commerce/checkout/processor/ProcessOrder}
 * which calls {@code OrderManager.processOrder()} to persist the order
 * and advance its state machine to SUBMITTED.</p>
 *
 * <p>This is the final processor; the calling service then clears the cart.
 * In ATG, the pipeline result is checked by the CheckoutFormHandler before
 * redirecting to the order confirmation page.</p>
 */
@Component
@org.springframework.core.annotation.Order(60)
@RequiredArgsConstructor
@Slf4j
public class FinaliseOrderProcessor implements PipelineProcessor {

    private final OrderRepository orderRepository;

    @Override
    public void process(PipelineContext ctx) {
        if (ctx.isStopChain()) return;
        log.debug("[Pipeline] {} executing", getName());

        ctx.getOrder().setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(ctx.getOrder());
        ctx.setOrder(saved);
        ctx.setSuccess(true);

        log.info("[Pipeline] {} — order {} CONFIRMED and persisted (id={})",
                 getName(), saved.getOrderNumber(), saved.getId());
    }
}

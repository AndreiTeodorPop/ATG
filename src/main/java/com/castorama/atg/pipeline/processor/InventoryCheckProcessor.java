package com.castorama.atg.pipeline.processor;

import com.castorama.atg.domain.model.CartItem;
import com.castorama.atg.pipeline.PipelineContext;
import com.castorama.atg.pipeline.PipelineProcessor;
import com.castorama.atg.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Pipeline Step 2 — Verify sufficient stock for every cart item.
 *
 * <p>ATG analogy: {@code /atg/commerce/inventory/InventoryCheckProcessor}
 * which calls the InventoryManager to check availabilityStatus and quantity
 * for each SKU.  In ATG this can talk to an external WMS via an InventoryManager
 * implementation; here we check the local stock_quantity column.</p>
 *
 * <p>Important: does NOT decrement stock — that happens in
 * {@link ReserveInventoryProcessor} after payment authorisation succeeds,
 * matching ATG's two-phase inventory commit pattern.</p>
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class InventoryCheckProcessor implements PipelineProcessor {

    private final ProductRepository productRepository;

    @Override
    public void process(PipelineContext ctx) {
        if (ctx.isStopChain()) return;
        log.debug("[Pipeline] {} executing", getName());

        for (CartItem item : ctx.getCartItems()) {
            int available = item.getProduct().getStockQuantity();
            if (available < item.getQuantity()) {
                ctx.addError(String.format(
                        "Stock insuffisant pour '%s' (SKU: %s). Disponible: %d, demandé: %d.",
                        item.getProduct().getName(),
                        item.getProduct().getSkuCode(),
                        available,
                        item.getQuantity()));
                log.warn("[Pipeline] {} STOPPED — insufficient stock for SKU {}",
                         getName(), item.getProduct().getSkuCode());
                return;
            }
        }
        log.debug("[Pipeline] {} — all items in stock", getName());
    }
}

package com.castorama.atg.pipeline.processor;

import com.castorama.atg.domain.model.CartItem;
import com.castorama.atg.domain.model.Product;
import com.castorama.atg.pipeline.PipelineContext;
import com.castorama.atg.pipeline.PipelineProcessor;
import com.castorama.atg.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Pipeline Step 5 — Decrement stock after payment authorisation.
 *
 * <p>ATG analogy: {@code /atg/commerce/inventory/InventoryReservationProcessor}
 * — called after payment auth succeeds to atomically reduce available
 * inventory.  In ATG this may call an external WMS or update the local
 * inventory repository.</p>
 *
 * <p>Compensating transaction: if a later processor fails, stock should
 * theoretically be restored.  In this demo we accept the simplification;
 * a production system would use a saga or a transactional outbox pattern.</p>
 */
@Component
@Order(50)
@RequiredArgsConstructor
@Slf4j
public class ReserveInventoryProcessor implements PipelineProcessor {

    private final ProductRepository productRepository;

    @Override
    public void process(PipelineContext ctx) {
        if (ctx.isStopChain()) return;
        log.debug("[Pipeline] {} executing", getName());

        for (CartItem cartItem : ctx.getCartItems()) {
            Product product = cartItem.getProduct();
            int newStock = product.getStockQuantity() - cartItem.getQuantity();
            product.setStockQuantity(Math.max(newStock, 0));
            productRepository.save(product);
            log.debug("[Pipeline] {} — SKU {} stock: {} -> {}",
                      getName(), product.getSkuCode(),
                      product.getStockQuantity() + cartItem.getQuantity(), newStock);
        }
    }
}

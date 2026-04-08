package com.castorama.atg.pipeline.processor;

import com.castorama.atg.pipeline.PipelineContext;
import com.castorama.atg.pipeline.PipelineProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Pipeline Step 1 — Validate that the cart has items before proceeding.
 *
 * <p>ATG analogy: {@code /atg/commerce/checkout/processor/ValidateCartContents}
 * — the first processor in the ATG checkout pipeline that verifies the cart
 * is not empty and all items have a valid catalogRefId.</p>
 */
@Component
@Order(10)
@Slf4j
public class ValidateCartProcessor implements PipelineProcessor {

    @Override
    public void process(PipelineContext ctx) {
        log.debug("[Pipeline] {} executing", getName());

        if (ctx.getCartItems() == null || ctx.getCartItems().isEmpty()) {
            ctx.addError("Le panier est vide. Ajoutez des articles avant de passer commande.");
            log.warn("[Pipeline] {} STOPPED — cart is empty for user {}",
                     getName(), ctx.getUser().getEmail());
            return;
        }

        // Validate each item still has a live product reference
        boolean invalidItem = ctx.getCartItems().stream()
                .anyMatch(item -> item.getProduct() == null);
        if (invalidItem) {
            ctx.addError("Un ou plusieurs articles du panier font référence à des produits supprimés.");
            log.warn("[Pipeline] {} STOPPED — orphaned cart items detected", getName());
        }
    }
}

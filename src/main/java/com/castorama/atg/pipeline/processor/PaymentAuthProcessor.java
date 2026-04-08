package com.castorama.atg.pipeline.processor;

import com.castorama.atg.pipeline.PipelineContext;
import com.castorama.atg.pipeline.PipelineProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Pipeline Step 4 — Authorise payment with the payment gateway.
 *
 * <p>ATG analogy: {@code /atg/commerce/payment/processor/AuthorizePaymentProcessor}
 * which delegates to a PaymentManager implementation (CyberSource, Adyen, etc.).
 * In ATG this processor receives the CreditCard/PaymentGroup from the Order and
 * calls the payment gateway's authorise() method.</p>
 *
 * <p>This demo simulates the gateway call: "REFUSE_TEST" triggers a decline,
 * everything else authorises successfully.  Replace the stub with an Adyen,
 * Stripe, or Worldline SDK call for production use.</p>
 */
@Component
@Order(40)
@Slf4j
public class PaymentAuthProcessor implements PipelineProcessor {

    /** Magic value that simulates a declined payment — useful for testing. */
    private static final String DECLINE_TRIGGER = "REFUSE_TEST";

    @Override
    public void process(PipelineContext ctx) {
        if (ctx.isStopChain()) return;
        log.debug("[Pipeline] {} executing for payment method: {}",
                  getName(), ctx.getPaymentMethod());

        if (DECLINE_TRIGGER.equalsIgnoreCase(ctx.getPaymentMethod())) {
            ctx.addError("Paiement refusé par la banque émettrice. Veuillez utiliser un autre moyen de paiement.");
            log.warn("[Pipeline] {} STOPPED — payment declined (test trigger)", getName());
            return;
        }

        // Simulate a successful authorisation
        log.info("[Pipeline] {} — payment authorised for order {} amount={}",
                 getName(),
                 ctx.getOrder().getOrderNumber(),
                 ctx.getOrder().getTotalAmount());
    }
}

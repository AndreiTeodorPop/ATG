package com.castorama.atg.pipeline;

/**
 * Contract for a single step in a checkout pipeline chain.
 *
 * <p>ATG analogy: {@code atg.service.pipeline.PipelineProcessor} interface.
 * Each implementation performs one atomic unit of work (validation,
 * inventory reservation, order creation, payment authorisation, etc.)
 * and writes its results back into the {@link PipelineContext}.</p>
 *
 * <p>Processors must:</p>
 * <ul>
 *   <li>Be stateless — all state flows through {@link PipelineContext}.</li>
 *   <li>Call {@link PipelineContext#addError} and return early on failure
 *       rather than throwing (mirrors ATG's STOP_CHAIN_EXECUTION pattern).</li>
 *   <li>Be registered as Spring beans so they can be dependency-injected
 *       (ATG: Nucleus component with {@code $class} property).</li>
 * </ul>
 */
public interface PipelineProcessor {

    /**
     * Execute this processor's logic.
     *
     * @param ctx shared pipeline context — read inputs, write outputs
     */
    void process(PipelineContext ctx);

    /**
     * Human-readable name for logging — ATG: component path like
     * {@code /atg/commerce/checkout/processor/ValidateCart}.
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}

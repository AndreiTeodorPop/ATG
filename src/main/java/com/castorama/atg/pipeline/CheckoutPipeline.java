package com.castorama.atg.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ordered chain of {@link PipelineProcessor} instances — the checkout pipeline.
 *
 * <p>ATG analogy: a {@code PipelineChain} defined in a Nucleus properties file,
 * e.g. {@code /atg/commerce/checkout/CheckoutPipeline.properties}:
 * <pre>
 *   $class=atg.service.pipeline.PipelineManager
 *   pipelineChainName=checkoutPipeline
 *   chainedProcessors=\
 *     /atg/commerce/checkout/processor/ValidateCart,\
 *     /atg/commerce/inventory/InventoryCheckProcessor,\
 *     ...
 * </pre>
 * In ATG the ordering is defined by the properties file; here Spring's
 * {@code @Order} annotation on each {@link PipelineProcessor} bean drives
 * the injection order into this list.</p>
 *
 * <p>Each processor is called in order until either all succeed or one sets
 * {@link PipelineContext#isStopChain()} to {@code true}.  This mirrors ATG's
 * {@code STOP_CHAIN_EXECUTION} return value.</p>
 */
@Component
@Slf4j
public class CheckoutPipeline {

    private final List<PipelineProcessor> processors;

    /**
     * Spring injects all {@link PipelineProcessor} beans in {@code @Order} sequence.
     * ATG equivalent: Nucleus resolving all components in the chainedProcessors list.
     */
    public CheckoutPipeline(List<PipelineProcessor> processors) {
        this.processors = processors;
        log.info("CheckoutPipeline initialised with {} processors: {}",
                 processors.size(),
                 processors.stream().map(PipelineProcessor::getName).toList());
    }

    /**
     * Execute the pipeline.
     *
     * @param ctx populated checkout context
     * @return the same context, mutated by processors
     */
    public PipelineContext execute(PipelineContext ctx) {
        log.info("CheckoutPipeline starting for user '{}'", ctx.getUser().getEmail());

        for (PipelineProcessor processor : processors) {
            if (ctx.isStopChain()) {
                log.info("CheckoutPipeline stopped before '{}' due to earlier error",
                         processor.getName());
                break;
            }
            try {
                processor.process(ctx);
            } catch (Exception ex) {
                log.error("Unexpected exception in pipeline processor '{}': {}",
                          processor.getName(), ex.getMessage(), ex);
                ctx.addError("Erreur interne lors du traitement de la commande. " +
                             "Veuillez réessayer ou contacter le support.");
            }
        }

        if (ctx.isSuccess()) {
            log.info("CheckoutPipeline completed SUCCESSFULLY — order {}",
                     ctx.getOrder().getOrderNumber());
        } else {
            log.warn("CheckoutPipeline completed with errors: {}", ctx.getErrors());
        }
        return ctx;
    }
}

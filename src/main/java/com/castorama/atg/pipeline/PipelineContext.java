package com.castorama.atg.pipeline;

import com.castorama.atg.domain.model.CartItem;
import com.castorama.atg.domain.model.Order;
import com.castorama.atg.domain.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared mutable context object passed through all pipeline processors.
 *
 * <p>ATG analogy: the {@code HashMap} parameter object passed into
 * {@code PipelineManager.runProcess()} which every {@code PipelineProcessor}
 * reads from and writes to.  ATG pipeline processors communicate exclusively
 * via this map — no direct service calls between processors.</p>
 *
 * <p>Using a typed POJO here rather than a raw Map improves type safety
 * while preserving the same decoupled communication contract.</p>
 */
@Data
@Builder
public class PipelineContext {

    // ---- Input parameters (set before pipeline starts) ----
    private User user;
    private List<CartItem> cartItems;
    private String shippingAddress;
    private String paymentMethod;

    // ---- Output / intermediate state (set by processors) ----
    private Order order;
    private boolean success;

    /** Accumulates errors from individual processors — ATG: pipelineResult errors. */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * Halt flag — set by any processor to stop chain execution.
     * ATG: returning {@code PipelineConstants.STOP_CHAIN_EXECUTION} from
     * {@code runProcess()} causes the PipelineManager to abort the chain.
     */
    private boolean stopChain;

    public void addError(String error) {
        errors.add(error);
        stopChain = true;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

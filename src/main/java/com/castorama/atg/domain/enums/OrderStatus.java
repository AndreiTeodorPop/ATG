package com.castorama.atg.domain.enums;

/**
 * Mirrors the ATG Commerce order state machine states defined in
 * {@code /atg/commerce/order/OrderStates.properties}.
 *
 * <p>ATG's order state machine uses string constants; here we use an enum
 * for compile-time safety while keeping the same semantic lifecycle.</p>
 */
public enum OrderStatus {

    /**
     * Order object created but not yet submitted — equivalent to ATG's
     * {@code INCOMPLETE} state while the user is still in the cart.
     */
    INCOMPLETE,

    /**
     * User has submitted the order; payment and inventory checks are running
     * through the checkout pipeline.  ATG equivalent: {@code PENDING_MERCHANT_ACTION}.
     */
    PENDING,

    /**
     * Payment authorised, inventory confirmed.  ATG: {@code SUBMITTED}.
     */
    CONFIRMED,

    /**
     * Order has been dispatched from the depot / click-and-collect ready.
     * ATG: {@code PROCESSING}.
     */
    SHIPPED,

    /**
     * Delivered to customer or collected in store.  ATG: {@code NO_PENDING_ACTION}.
     */
    DELIVERED,

    /**
     * Order cancelled at any pre-delivery stage.  ATG: {@code REMOVED}.
     */
    CANCELLED
}

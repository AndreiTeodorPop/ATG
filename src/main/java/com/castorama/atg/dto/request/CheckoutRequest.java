package com.castorama.atg.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ATG analogy: the shipping + payment form fields submitted to
 * CheckoutFormHandler.checkout() which triggers the checkout pipeline.
 */
@Data
public class CheckoutRequest {

    @NotBlank(message = "L'adresse de livraison est obligatoire")
    private String shippingAddress;

    /**
     * Payment method label — e.g. "CARTE_BANCAIRE", "PAYPAL", "VIREMENT".
     * In ATG this would be a PaymentGroup type on the Order.
     */
    @NotBlank(message = "Le mode de paiement est obligatoire")
    private String paymentMethod;
}

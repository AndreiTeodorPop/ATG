package com.castorama.atg.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ATG analogy: parameters submitted to CartModifierFormHandler.addItemToOrder().
 */
@Data
public class CartItemRequest {

    /** ATG: catalogRefId / SKU code of the item to add. */
    @NotBlank(message = "Le code SKU est obligatoire")
    private String skuCode;

    @Min(value = 1, message = "La quantité doit être d'au moins 1")
    private int quantity = 1;
}

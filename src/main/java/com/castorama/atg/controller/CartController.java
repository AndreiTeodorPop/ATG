package com.castorama.atg.controller;

import com.castorama.atg.domain.model.User;
import com.castorama.atg.dto.request.CartItemRequest;
import com.castorama.atg.dto.response.CartResponse;
import com.castorama.atg.service.CartService;
import com.castorama.atg.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Shopping cart management endpoints — all require authentication.
 *
 * <p>ATG analogy: CartModifierFormHandler endpoints exposed over REST.
 * The cart is user-scoped (profile-bound) matching ATG's session/profile
 * cart association.</p>
 *
 * <p>Base path: {@code /api/v1/cart}</p>
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    /**
     * Get the current cart contents with totals.
     *
     * <pre>
     * GET /api/v1/cart
     * Authorization: Bearer {token}
     * </pre>
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(cartService.getCart(user));
    }

    /**
     * Add a product to the cart or increment existing quantity.
     *
     * <pre>
     * POST /api/v1/cart/items
     * Authorization: Bearer {token}
     * {
     *   "skuCode": "CAST-10001",
     *   "quantity": 2
     * }
     * </pre>
     *
     * @return 201 Created with updated cart
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        User user = userService.findByEmail(userDetails.getUsername());
        CartResponse cart = cartService.addItem(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    /**
     * Update the quantity of an existing cart item.
     * Setting quantity to 0 removes the item.
     *
     * <pre>
     * PUT /api/v1/cart/items/5
     * Authorization: Bearer {token}
     * { "quantity": 3 }
     * </pre>
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @RequestParam int quantity) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(cartService.updateItem(user, itemId, quantity));
    }

    /**
     * Remove a specific item from the cart.
     *
     * <pre>
     * DELETE /api/v1/cart/items/5
     * Authorization: Bearer {token}
     * </pre>
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(cartService.removeItem(user, itemId));
    }

    /**
     * Clear all items from the cart.
     *
     * <pre>
     * DELETE /api/v1/cart
     * Authorization: Bearer {token}
     * </pre>
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}

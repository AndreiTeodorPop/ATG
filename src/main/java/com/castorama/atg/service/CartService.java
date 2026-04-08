package com.castorama.atg.service;

import com.castorama.atg.domain.model.CartItem;
import com.castorama.atg.domain.model.Product;
import com.castorama.atg.domain.model.User;
import com.castorama.atg.dto.request.CartItemRequest;
import com.castorama.atg.dto.response.CartItemResponse;
import com.castorama.atg.dto.response.CartResponse;
import com.castorama.atg.exception.BusinessException;
import com.castorama.atg.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Nucleus-style component: Shopping Cart Service.
 *
 * <p>ATG analogy: the session-scoped {@code /atg/commerce/ShoppingCart} component
 * combined with {@code CartModifierFormHandler} logic.  In ATG the ShoppingCart
 * wraps an Order in INCOMPLETE state; here CartItems are persisted independently
 * and merged into an Order on checkout.</p>
 *
 * <p>All cart mutation methods are {@code @Transactional} — matching ATG's
 * requirement that cart modifications occur within a single repository
 * transaction to avoid partial updates.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private static final BigDecimal TVA_RATE = new BigDecimal("0.20");

    private final CartItemRepository cartItemRepository;
    private final CatalogService catalogService;

    /**
     * Get the current cart contents.
     * ATG: ShoppingCart.getCurrentOrder().getCommerceItems().
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        List<CartItem> items = cartItemRepository.findByUser(user);
        return buildCartResponse(items);
    }

    /**
     * Add an item to the cart or increment its quantity if already present.
     * ATG: CartModifierFormHandler.addItemToOrder().
     *
     * @throws BusinessException if the product is out of stock
     */
    @Transactional
    public CartResponse addItem(User user, CartItemRequest request) {
        Product product = catalogService.findActiveProductBySkuCode(request.getSkuCode());

        if (!product.isInStock()) {
            throw new BusinessException("OUT_OF_STOCK",
                    String.format("'%s' (SKU: %s) est actuellement en rupture de stock.",
                                  product.getName(), product.getSkuCode()));
        }

        int existingQty = 0;
        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product).orElse(null);

        if (cartItem != null) {
            existingQty = cartItem.getQuantity();
            int newQty = existingQty + request.getQuantity();
            if (newQty > product.getStockQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK",
                        String.format("Quantité demandée (%d) supérieure au stock disponible (%d).",
                                      newQty, product.getStockQuantity()));
            }
            cartItem.setQuantity(newQty);
            cartItemRepository.save(cartItem);
            log.debug("Cart: updated qty for SKU {} to {} for user {}",
                      product.getSkuCode(), newQty, user.getEmail());
        } else {
            if (request.getQuantity() > product.getStockQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK",
                        String.format("Quantité demandée (%d) supérieure au stock disponible (%d).",
                                      request.getQuantity(), product.getStockQuantity()));
            }
            cartItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getEffectivePrice())
                    .build();
            cartItemRepository.save(cartItem);
            log.debug("Cart: added SKU {} x{} for user {}",
                      product.getSkuCode(), request.getQuantity(), user.getEmail());
        }

        return getCart(user);
    }

    /**
     * Update the quantity of an existing cart item.
     * ATG: CartModifierFormHandler.setQuantity().
     *
     * @throws BusinessException if quantity is 0 (use removeItem instead)
     */
    @Transactional
    public CartResponse updateItem(User user, Long cartItemId, int quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .filter(ci -> ci.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND",
                        "Article panier introuvable: " + cartItemId));

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
            log.debug("Cart: removed item id={} for user {}", cartItemId, user.getEmail());
        } else {
            if (quantity > cartItem.getProduct().getStockQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK",
                        String.format("Quantité demandée (%d) supérieure au stock disponible (%d).",
                                      quantity, cartItem.getProduct().getStockQuantity()));
            }
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }
        return getCart(user);
    }

    /**
     * Remove a specific item from the cart.
     * ATG: CartModifierFormHandler.removeItemFromOrder().
     */
    @Transactional
    public CartResponse removeItem(User user, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .filter(ci -> ci.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new BusinessException("CART_ITEM_NOT_FOUND",
                        "Article panier introuvable: " + cartItemId));
        cartItemRepository.delete(cartItem);
        log.debug("Cart: removed item id={} for user {}", cartItemId, user.getEmail());
        return getCart(user);
    }

    /**
     * Clear all items from the cart.
     * ATG: CartModifierFormHandler.removeAllItemsFromOrder().
     */
    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteAllByUser(user);
        log.debug("Cart: cleared for user {}", user.getEmail());
    }

    /**
     * Retrieve raw cart items for pipeline processing.
     * Called by OrderService before triggering the checkout pipeline.
     */
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }

    // ---- Mapping ----

    private CartResponse buildCartResponse(List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(CartService::toItemResponse)
                .toList();

        BigDecimal subtotal = items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(TVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        return CartResponse.builder()
                .items(itemResponses)
                .itemCount(items.size())
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .taxAmount(tax)
                .total(total)
                .build();
    }

    public static CartItemResponse toItemResponse(CartItem ci) {
        return CartItemResponse.builder()
                .id(ci.getId())
                .skuCode(ci.getProduct().getSkuCode())
                .productName(ci.getProduct().getName())
                .imageUrl(ci.getProduct().getImageUrl())
                .quantity(ci.getQuantity())
                .unitPrice(ci.getUnitPrice())
                .lineTotal(ci.getLineTotal())
                .addedAt(ci.getAddedAt())
                .build();
    }
}

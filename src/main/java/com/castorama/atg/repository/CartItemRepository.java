package com.castorama.atg.repository;

import com.castorama.atg.domain.model.CartItem;
import com.castorama.atg.domain.model.Product;
import com.castorama.atg.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ATG analogy: the session-scoped ShoppingCart component's persistence layer.
 *
 * <p>In ATG, the cart is a session component ({@code /atg/commerce/ShoppingCart})
 * backed by the Order repository. Un-submitted orders in state INCOMPLETE
 * effectively represent carts.  Here we use a dedicated CartItem table for
 * simplicity, which is merged into an Order on checkout.</p>
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    /** Bulk clear — ATG: CartModifierFormHandler.removeAllItemsFromOrder(). */
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user = :user")
    void deleteAllByUser(@Param("user") User user);

    long countByUser(User user);
}

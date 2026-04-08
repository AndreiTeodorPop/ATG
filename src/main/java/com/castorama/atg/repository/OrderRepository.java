package com.castorama.atg.repository;

import com.castorama.atg.domain.enums.OrderStatus;
import com.castorama.atg.domain.model.Order;
import com.castorama.atg.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ATG analogy: the {@code /atg/commerce/order/OrderRepository} GSA repository.
 *
 * <p>In ATG you would call:
 * <pre>
 *   orderRepository.getOrder(orderId);
 *   orderRepository.getOrderIdsForProfile(profileId, queryOptions);
 * </pre>
 * The Spring Data methods here mirror those ATG OrderRepository methods.</p>
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    /** ATG: OrderRepository.getOrderIdsForProfile — all orders for a profile. */
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Order> findByUserAndStatus(User user, OrderStatus status);

    /**
     * Eager-fetch items in one query to avoid N+1 when rendering order history.
     * ATG: RepositoryView with item-descriptor join.
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
}

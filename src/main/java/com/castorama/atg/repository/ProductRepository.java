package com.castorama.atg.repository;

import com.castorama.atg.domain.enums.ProductCategory;
import com.castorama.atg.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * ATG analogy: the {@code /atg/commerce/catalog/ProductCatalog} GSA repository.
 *
 * <p>In ATG, catalog queries are expressed in RQL and executed against the
 * {@code atg_catalog} schema.  Faceted search at scale would go through
 * ATG Endeca (MDEX Engine).  For this demo, JPQL covers the same query surface
 * without Endeca infrastructure.</p>
 *
 * <p>Pagination maps to ATG's {@code QueryOptions} with {@code maxResults}
 * and {@code startIndex} — here provided by Spring Data's {@link Pageable}.</p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuCode(String skuCode);

    /** Browse active products in a category — ATG: RQL category navigation query. */
    Page<Product> findByCategoryAndActiveTrue(ProductCategory category, Pageable pageable);

    /** Full catalogue browse (active only). */
    Page<Product> findByActiveTrue(Pageable pageable);

    /**
     * Full-text search across name and description.
     * ATG Endeca equivalent: keyword search via MDEX query with field weights.
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Price-range browse.
     * ATG equivalent: RQL range predicate on sku.listPrice.
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "p.listPrice BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                   @Param("maxPrice") BigDecimal maxPrice,
                                   Pageable pageable);

    /**
     * On-sale products (promotional pricing active).
     * ATG: PriceList / PromotionManager query.
     */
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.salePrice IS NOT NULL " +
           "AND p.salePrice < p.listPrice")
    Page<Product> findOnSaleProducts(Pageable pageable);
}

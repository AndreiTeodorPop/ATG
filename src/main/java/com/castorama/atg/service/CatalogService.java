package com.castorama.atg.service;

import com.castorama.atg.domain.enums.ProductCategory;
import com.castorama.atg.domain.model.Product;
import com.castorama.atg.dto.response.PagedResponse;
import com.castorama.atg.dto.response.ProductResponse;
import com.castorama.atg.exception.ResourceNotFoundException;
import com.castorama.atg.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Nucleus-style component: Product Catalog Service.
 *
 * <p>ATG analogy: a global-scope Nucleus component wrapping
 * {@code /atg/commerce/catalog/ProductCatalog} repository queries.
 * In a full ATG deployment, catalog navigation and search would go through
 * ATG Endeca (MDEX Engine) with Experience Manager driving the page layout.
 * Here we use Spring Data JPA queries as a direct equivalent.</p>
 *
 * <p>Caching strategy: in ATG, catalog items are cached by the repository's
 * cache configuration (item-cache-size, expiry).  For this demo Spring's
 * default first-level JPA cache provides equivalent session-level caching.
 * A production system would add {@code @Cacheable} on hot catalog reads with
 * Redis as the distributed cache backing store.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository productRepository;

    /**
     * Browse all active products with pagination.
     * ATG: CatalogTools.findAllProducts() with RQL maxResults/startIndex.
     */
    public PagedResponse<ProductResponse> getAllProducts(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);
        return toPagedResponse(productPage);
    }

    /**
     * Browse products in a specific category.
     * ATG: category.childProducts query against the catalog hierarchy.
     */
    public PagedResponse<ProductResponse> getByCategory(ProductCategory category,
                                                         int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<Product> productPage = productRepository.findByCategoryAndActiveTrue(
                category, pageable);
        return toPagedResponse(productPage);
    }

    /**
     * Full-text keyword search.
     * ATG Endeca equivalent: keyword search via MDEX query.
     */
    public PagedResponse<ProductResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<Product> productPage = productRepository.searchByKeyword(keyword.trim(), pageable);
        log.debug("Catalog search '{}' returned {} results", keyword, productPage.getTotalElements());
        return toPagedResponse(productPage);
    }

    /**
     * Get a single product by its SKU code.
     * ATG: catalogRepository.getItemForSku(skuCode).
     */
    public ProductResponse getBySkuCode(String skuCode) {
        Product product = productRepository.findBySkuCode(skuCode)
                .filter(Product::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "skuCode", skuCode));
        return toResponse(product);
    }

    /**
     * Get a single product by its internal ID.
     * ATG: catalogRepository.getItem(repositoryId).
     */
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "id", id));
        return toResponse(product);
    }

    /**
     * Browse products within a price range.
     * ATG: RQL range predicate on sku.listPrice.
     */
    public PagedResponse<ProductResponse> getByPriceRange(BigDecimal min, BigDecimal max,
                                                           int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("listPrice"));
        return toPagedResponse(productRepository.findByPriceRange(min, max, pageable));
    }

    /**
     * Browse currently on-sale products.
     * ATG: PromotionManager active promotions query.
     */
    public PagedResponse<ProductResponse> getOnSaleProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("salePrice"));
        return toPagedResponse(productRepository.findOnSaleProducts(pageable));
    }

    // ---- Internal helpers ----

    Product findActiveProductBySkuCode(String skuCode) {
        return productRepository.findBySkuCode(skuCode)
                .filter(Product::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", "skuCode", skuCode));
    }

    private PagedResponse<ProductResponse> toPagedResponse(Page<Product> page) {
        return PagedResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(CatalogService::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public static ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .skuCode(p.getSkuCode())
                .name(p.getName())
                .description(p.getDescription())
                .listPrice(p.getListPrice())
                .salePrice(p.getSalePrice())
                .effectivePrice(p.getEffectivePrice())
                .onSale(p.isOnSale())
                .inStock(p.isInStock())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .category(p.getCategory())
                .categoryDisplayName(p.getCategory().getDisplayName())
                .brand(p.getBrand())
                .build();
    }
}

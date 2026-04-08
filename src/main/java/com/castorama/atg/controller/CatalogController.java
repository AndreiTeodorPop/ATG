package com.castorama.atg.controller;

import com.castorama.atg.domain.enums.ProductCategory;
import com.castorama.atg.dto.response.PagedResponse;
import com.castorama.atg.dto.response.ProductResponse;
import com.castorama.atg.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Product catalogue browsing endpoints — all public (no authentication required).
 *
 * <p>ATG analogy: the CatalogTargeter Droplet / REST layer endpoints that serve
 * the Castorama category navigation, search, and product detail pages.
 * In ATG, these would be rendered by Endeca Experience Manager pages with
 * ATG Droplets populating the catalogue data.</p>
 *
 * <p>Base path: {@code /api/v1/catalog}</p>
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    /**
     * Browse all active products.
     *
     * <pre>
     * GET /api/v1/catalog/products?page=0&size=20&sortBy=name
     * </pre>
     */
    @GetMapping("/products")
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        return ResponseEntity.ok(catalogService.getAllProducts(page, size, sortBy));
    }

    /**
     * Get a single product by internal ID.
     *
     * <pre>
     * GET /api/v1/catalog/products/42
     * </pre>
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getById(id));
    }

    /**
     * Get a single product by SKU code.
     *
     * <pre>
     * GET /api/v1/catalog/products/sku/CAST-10001
     * </pre>
     */
    @GetMapping("/products/sku/{skuCode}")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String skuCode) {
        return ResponseEntity.ok(catalogService.getBySkuCode(skuCode));
    }

    /**
     * Browse products in a given category.
     * Category values: OUTILLAGE, PEINTURE, SOL, PLOMBERIE, ELECTRICITE,
     *                  JARDIN, MENUISERIE, QUINCAILLERIE, CHAUFFAGE, CUISINE
     *
     * <pre>
     * GET /api/v1/catalog/categories/PEINTURE/products?page=0&size=10
     * </pre>
     */
    @GetMapping("/categories/{category}/products")
    public ResponseEntity<PagedResponse<ProductResponse>> getByCategory(
            @PathVariable ProductCategory category,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogService.getByCategory(category, page, size));
    }

    /**
     * Full-text keyword search across name, description, and brand.
     *
     * <pre>
     * GET /api/v1/catalog/search?q=perceuse&page=0&size=10
     * </pre>
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductResponse>> search(
            @RequestParam("q")                 String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogService.search(keyword, page, size));
    }

    /**
     * Browse products within a price range (HT — hors taxe).
     *
     * <pre>
     * GET /api/v1/catalog/products/price-range?min=10.00&max=50.00
     * </pre>
     */
    @GetMapping("/products/price-range")
    public ResponseEntity<PagedResponse<ProductResponse>> getByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogService.getByPriceRange(min, max, page, size));
    }

    /**
     * Browse currently on-sale / promotional products.
     *
     * <pre>
     * GET /api/v1/catalog/products/on-sale
     * </pre>
     */
    @GetMapping("/products/on-sale")
    public ResponseEntity<PagedResponse<ProductResponse>> getOnSaleProducts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogService.getOnSaleProducts(page, size));
    }

    /**
     * List all available product categories with display names.
     *
     * <pre>
     * GET /api/v1/catalog/categories
     * </pre>
     */
    @GetMapping("/categories")
    public ResponseEntity<ProductCategory[]> getCategories() {
        return ResponseEntity.ok(ProductCategory.values());
    }
}

package com.castorama.atg.admin;

import com.castorama.atg.domain.model.Order;
import com.castorama.atg.domain.model.Product;
import com.castorama.atg.domain.model.User;
import com.castorama.atg.pipeline.PipelineProcessor;
import com.castorama.atg.repository.CartItemRepository;
import com.castorama.atg.repository.OrderRepository;
import com.castorama.atg.repository.ProductRepository;
import com.castorama.atg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dyn/Admin web interface — a faithful Spring Boot recreation of the classic
 * Oracle ATG Dynamo Administration UI available at {@code /dyn/admin}.
 *
 * <p>ATG analogy: the built-in ATG Dynamo Admin servlet reachable at
 * {@code http://host:port/dyn/admin/}.  It provides real-time inspection of the
 * Nucleus component tree, repository item browsers, and JVM diagnostics.  This
 * controller provides equivalent capabilities for the Spring Boot "Nucleus-style"
 * application context.</p>
 *
 * <p>All pages are server-rendered HTML strings — no template engine required.
 * A private {@link #layout(String, String, String)} method wraps every page in
 * the consistent ATG-style chrome (dark header, left-nav sidebar, content area).</p>
 *
 * <p>Security: all {@code /dyn/admin/**} paths are protected by HTTP Basic Auth
 * via the {@code adminSecurityFilterChain} bean defined in {@code SecurityConfig}.
 * The chain is completely separate from the JWT API chain.</p>
 */
@Controller
@RequestMapping("/dyn/admin")
@RequiredArgsConstructor
public class DynAdminController {

    // ------------------------------------------------------------------
    // Dependencies — mirrors the ATG Nucleus component injections that the
    // real dyn/admin uses to inspect repositories and services at runtime.
    // ------------------------------------------------------------------

    private final ConfigurableApplicationContext applicationContext;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;

    /**
     * All {@link PipelineProcessor} beans, injected in {@code @Order} sequence.
     * ATG analogy: the ordered list of processors registered in a pipeline chain
     * component such as {@code /atg/commerce/checkout/processor/}.
     */
    private final List<PipelineProcessor> pipelineProcessors;

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    private static final int PRODUCTS_PAGE_SIZE = 15;
    private static final String APP_VERSION      = "3.2.4";
    private static final String APP_NAME         = "Castorama ATG Commerce";

    // ------------------------------------------------------------------
    // Dashboard
    // ------------------------------------------------------------------

    /**
     * Main dashboard — live counts and JVM health.
     * ATG analogy: the root {@code /dyn/admin/} page showing component counts.
     */
    @GetMapping({"", "/"})
    public ResponseEntity<String> dashboard() {
        long userCount    = userRepository.count();
        long productCount = productRepository.count();
        long orderCount   = orderRepository.count();
        long cartCount    = cartItemRepository.count();

        RuntimeMXBean  runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean   mem     = ManagementFactory.getMemoryMXBean();
        long heapUsed  = mem.getHeapMemoryUsage().getUsed()  / (1024 * 1024);
        long heapMax   = mem.getHeapMemoryUsage().getMax()   / (1024 * 1024);
        long uptimeMs  = runtime.getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);
        String uptimeStr = String.format("%dd %02dh %02dm %02ds",
                uptime.toDays(), uptime.toHoursPart(), uptime.toMinutesPart(), uptime.toSecondsPart());

        String content = "<h2 class='section-title'>Dashboard</h2>"
            + "<p class='breadcrumb'>/ dyn / admin /</p>"

            // Repository counts
            + "<h3 class='sub-title'>Repository Item Counts</h3>"
            + "<table class='data-table'>"
            + "<thead><tr><th>Repository</th><th>Item Descriptor</th><th>Count</th></tr></thead>"
            + "<tbody>"
            + tr("dps_user / UserRepository", "user", String.valueOf(userCount), false)
            + tr("atg_catalog_product / ProductRepository", "product", String.valueOf(productCount), true)
            + tr("atg_commerce_order / OrderRepository", "order", String.valueOf(orderCount), false)
            + tr("cart_item / CartItemRepository", "cartItem", String.valueOf(cartCount), true)
            + "</tbody></table>"

            // System info
            + "<h3 class='sub-title' style='margin-top:20px'>JVM &amp; Runtime</h3>"
            + "<table class='data-table'>"
            + "<thead><tr><th>Property</th><th>Value</th></tr></thead>"
            + "<tbody>"
            + tr2("Java Version",       System.getProperty("java.version"), false)
            + tr2("JVM Heap Used",      heapUsed + " MB / " + heapMax + " MB", true)
            + tr2("Server Uptime",      uptimeStr, false)
            + tr2("Spring Boot",        APP_VERSION, true)
            + tr2("Application",        APP_NAME, false)
            + tr2("Started at",         LocalDateTime.now().minus(uptime).format(
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), true)
            + tr2("Active Processors",  String.valueOf(Runtime.getRuntime().availableProcessors()), false)
            + tr2("OS",                 System.getProperty("os.name") + " " + System.getProperty("os.version"), true)
            + "</tbody></table>";

        return html(layout("Dashboard", "/ dyn / admin /", content));
    }

    // ------------------------------------------------------------------
    // Nucleus Component Browser
    // ------------------------------------------------------------------

    /**
     * Nucleus component browser — lists all Spring beans grouped by stereotype.
     * ATG analogy: the {@code /dyn/admin/atg/} component tree that lets you
     * navigate and inspect every Nucleus singleton component at runtime.
     */
    @GetMapping("/nucleus")
    public ResponseEntity<String> nucleus() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        // Group beans by conceptual category (mimicking ATG's /atg/ tree layout)
        Map<String, List<String[]>> groups = new LinkedHashMap<>();
        groups.put("Services",            Arrays.stream(beanNames)
            .filter(n -> isOfType(n, "com.castorama.atg.service"))
            .map(n -> beanRow(n)).collect(Collectors.toList()));
        groups.put("Repositories",        Arrays.stream(beanNames)
            .filter(n -> isOfType(n, "com.castorama.atg.repository"))
            .map(n -> beanRow(n)).collect(Collectors.toList()));
        groups.put("Controllers",         Arrays.stream(beanNames)
            .filter(n -> isOfType(n, "com.castorama.atg.controller"))
            .map(n -> beanRow(n)).collect(Collectors.toList()));
        groups.put("Pipeline Processors", Arrays.stream(beanNames)
            .filter(n -> isOfType(n, "com.castorama.atg.pipeline"))
            .map(n -> beanRow(n)).collect(Collectors.toList()));
        groups.put("Security",            Arrays.stream(beanNames)
            .filter(n -> isOfType(n, "com.castorama.atg.security")
                      || isOfType(n, "com.castorama.atg.admin"))
            .map(n -> beanRow(n)).collect(Collectors.toList()));
        groups.put("Configuration",       Arrays.stream(beanNames)
            .filter(n -> isOfType(n, "com.castorama.atg.config"))
            .map(n -> beanRow(n)).collect(Collectors.toList()));

        long totalBeans = beanNames.length;

        StringBuilder sb = new StringBuilder();
        sb.append("<h2 class='section-title'>Nucleus Component Browser</h2>");
        sb.append("<p class='breadcrumb'>/ dyn / admin / nucleus /</p>");
        sb.append("<p style='margin-bottom:14px;color:#555'>Total Spring beans in ApplicationContext: <strong>")
          .append(totalBeans).append("</strong></p>");

        for (Map.Entry<String, List<String[]>> entry : groups.entrySet()) {
            List<String[]> rows = entry.getValue();
            if (rows.isEmpty()) continue;

            sb.append("<h3 class='sub-title'>").append(entry.getKey())
              .append(" <span style='font-weight:normal;font-size:12px'>(")
              .append(rows.size()).append(" component").append(rows.size() == 1 ? "" : "s")
              .append(")</span></h3>");
            sb.append("<table class='data-table'>");
            sb.append("<thead><tr><th>Bean Name</th><th>Class</th><th>Scope</th></tr></thead><tbody>");

            boolean alt = false;
            for (String[] row : rows) {
                String bg = alt ? " class='alt'" : "";
                sb.append("<tr").append(bg).append(">")
                  .append("<td><code>").append(esc(row[0])).append("</code></td>")
                  .append("<td><code style='font-size:11px'>").append(esc(row[1])).append("</code></td>")
                  .append("<td>").append(esc(row[2])).append("</td>")
                  .append("</tr>");
                alt = !alt;
            }
            sb.append("</tbody></table>");
        }

        return html(layout("Nucleus Component Browser", "/ dyn / admin / nucleus /", sb.toString()));
    }

    // ------------------------------------------------------------------
    // Repository browsers
    // ------------------------------------------------------------------

    /**
     * User Repository — table of all profile items.
     * ATG analogy: browsing {@code /atg/userprofiling/ProfileAdapterRepository}
     * in dyn/admin, showing all profile items with their key properties.
     */
    @GetMapping("/repo/users")
    public ResponseEntity<String> repoUsers() {
        List<User> users = userRepository.findAll(Sort.by("id"));

        StringBuilder sb = new StringBuilder();
        sb.append("<h2 class='section-title'>User Repository</h2>");
        sb.append("<p class='breadcrumb'>/ dyn / admin / repo / users /</p>");
        sb.append("<p style='margin-bottom:14px;color:#555'>Item descriptor: <code>user</code> &mdash; ")
          .append(users.size()).append(" item").append(users.size() == 1 ? "" : "s").append("</p>");

        sb.append("<table class='data-table'>");
        sb.append("<thead><tr><th>ID</th><th>Login</th><th>Email</th><th>Role</th>"
                + "<th>First Name</th><th>Last Name</th><th>Created At</th><th>Action</th></tr></thead>");
        sb.append("<tbody>");

        boolean alt = false;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (User u : users) {
            String bg = alt ? " class='alt'" : "";
            sb.append("<tr").append(bg).append(">")
              .append("<td>").append(u.getId()).append("</td>")
              .append("<td><strong>").append(esc(u.getLogin())).append("</strong></td>")
              .append("<td>").append(esc(u.getEmail())).append("</td>")
              .append("<td><span class='badge'>").append(esc(u.getRole())).append("</span></td>")
              .append("<td>").append(esc(nvl(u.getFirstName()))).append("</td>")
              .append("<td>").append(esc(nvl(u.getLastName()))).append("</td>")
              .append("<td>").append(u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : "—").append("</td>")
              .append("<td>")
              .append("<form method='post' action='/dyn/admin/repo/users/").append(u.getId()).append("/delete' ")
              .append("onsubmit=\"return confirm('Delete user ").append(esc(u.getLogin())).append("?');\">")
              .append("<button type='submit' class='btn-danger'>Delete</button>")
              .append("</form>")
              .append("</td>")
              .append("</tr>");
            alt = !alt;
        }
        sb.append("</tbody></table>");

        return html(layout("User Repository", "/ dyn / admin / repo / users /", sb.toString()));
    }

    /**
     * Deletes a user profile item.
     * ATG analogy: calling {@code MutableRepository.removeItem()} on a profile item
     * through the dyn/admin repository editor.
     */
    @PostMapping("/repo/users/{id}/delete")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.status(302)
            .header("Location", "/dyn/admin/repo/users")
            .build();
    }

    /**
     * Product Repository — paginated SKU/product browser.
     * ATG analogy: browsing {@code /atg/commerce/catalog/ProductCatalog}
     * in dyn/admin — the product item descriptor table.
     */
    @GetMapping("/repo/products")
    public ResponseEntity<String> repoProducts(
            @RequestParam(defaultValue = "0") int page) {

        Page<Product> productPage = productRepository.findAll(
                PageRequest.of(page, PRODUCTS_PAGE_SIZE, Sort.by("id")));
        List<Product> products = productPage.getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("<h2 class='section-title'>Product Repository</h2>");
        sb.append("<p class='breadcrumb'>/ dyn / admin / repo / products /</p>");
        sb.append("<p style='margin-bottom:14px;color:#555'>Item descriptor: <code>product</code> &mdash; ")
          .append(productPage.getTotalElements()).append(" items total, page ")
          .append(page + 1).append(" of ").append(productPage.getTotalPages()).append("</p>");

        sb.append("<table class='data-table'>");
        sb.append("<thead><tr><th>SKU Code</th><th>Name</th><th>Brand</th><th>Category</th>"
                + "<th>List Price</th><th>Sale Price</th><th>Stock</th><th>Active</th></tr></thead>");
        sb.append("<tbody>");

        boolean alt = false;
        for (Product p : products) {
            String bg = alt ? " class='alt'" : "";
            sb.append("<tr").append(bg).append(">")
              .append("<td><code>").append(esc(p.getSkuCode())).append("</code></td>")
              .append("<td>").append(esc(p.getName())).append("</td>")
              .append("<td>").append(esc(nvl(p.getBrand()))).append("</td>")
              .append("<td>").append(p.getCategory() != null ? esc(p.getCategory().name()) : "—").append("</td>")
              .append("<td>").append(p.getListPrice() != null ? "&euro;" + p.getListPrice() : "—").append("</td>")
              .append("<td>").append(p.getSalePrice() != null ? "&euro;" + p.getSalePrice() : "—").append("</td>")
              .append("<td>").append(p.getStockQuantity() != null ? p.getStockQuantity() : 0).append("</td>")
              .append("<td>").append(Boolean.TRUE.equals(p.getActive()) ? "&#10003;" : "&#10007;").append("</td>")
              .append("</tr>");
            alt = !alt;
        }
        sb.append("</tbody></table>");

        // Pagination controls
        sb.append("<div class='pagination'>");
        if (page > 0) {
            sb.append("<a href='/dyn/admin/repo/products?page=").append(page - 1)
              .append("' class='page-link'>&laquo; Previous</a> ");
        }
        for (int i = 0; i < productPage.getTotalPages(); i++) {
            if (i == page) {
                sb.append("<span class='page-current'>").append(i + 1).append("</span> ");
            } else {
                sb.append("<a href='/dyn/admin/repo/products?page=").append(i)
                  .append("' class='page-link'>").append(i + 1).append("</a> ");
            }
        }
        if (page < productPage.getTotalPages() - 1) {
            sb.append("<a href='/dyn/admin/repo/products?page=").append(page + 1)
              .append("' class='page-link'>Next &raquo;</a>");
        }
        sb.append("</div>");

        return html(layout("Product Repository", "/ dyn / admin / repo / products /", sb.toString()));
    }

    /**
     * Order Repository — all commerce orders with status and totals.
     * ATG analogy: browsing {@code /atg/commerce/order/OrderRepository} in
     * dyn/admin — viewing order item descriptors with their state machine status.
     */
    @GetMapping("/repo/orders")
    public ResponseEntity<String> repoOrders() {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        StringBuilder sb = new StringBuilder();
        sb.append("<h2 class='section-title'>Order Repository</h2>");
        sb.append("<p class='breadcrumb'>/ dyn / admin / repo / orders /</p>");
        sb.append("<p style='margin-bottom:14px;color:#555'>Item descriptor: <code>order</code> &mdash; ")
          .append(orders.size()).append(" order").append(orders.size() == 1 ? "" : "s").append("</p>");

        sb.append("<table class='data-table'>");
        sb.append("<thead><tr><th>Order Number</th><th>Customer Email</th><th>Status</th>"
                + "<th>Total (TTC)</th><th>Tax (TVA)</th><th>Shipping</th><th>Submitted At</th></tr></thead>");
        sb.append("<tbody>");

        boolean alt = false;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Order o : orders) {
            String bg = alt ? " class='alt'" : "";
            String statusClass = statusBadgeClass(o.getStatus() != null ? o.getStatus().name() : "");
            sb.append("<tr").append(bg).append(">")
              .append("<td><strong>").append(esc(o.getOrderNumber())).append("</strong></td>")
              .append("<td>").append(o.getUser() != null ? esc(o.getUser().getEmail()) : "—").append("</td>")
              .append("<td><span class='badge ").append(statusClass).append("'>")
                .append(o.getStatus() != null ? o.getStatus().name() : "—").append("</span></td>")
              .append("<td><strong>&euro;").append(o.getTotalAmount()).append("</strong></td>")
              .append("<td>&euro;").append(o.getTaxAmount()).append("</td>")
              .append("<td>&euro;").append(o.getShippingAmount()).append("</td>")
              .append("<td>").append(o.getSubmittedAt() != null ? o.getSubmittedAt().format(fmt) : "—").append("</td>")
              .append("</tr>");
            alt = !alt;
        }
        sb.append("</tbody></table>");

        return html(layout("Order Repository", "/ dyn / admin / repo / orders /", sb.toString()));
    }

    // ------------------------------------------------------------------
    // Pipeline Viewer
    // ------------------------------------------------------------------

    /**
     * Pipeline chain viewer — shows the checkout pipeline in processor order.
     * ATG analogy: the dyn/admin view of a pipeline chain component such as
     * {@code /atg/commerce/checkout/processor/CheckoutPipeline} which lists
     * every {@code PipelineProcessor} in chain sequence order.
     */
    @GetMapping("/pipeline")
    public ResponseEntity<String> pipeline() {
        // Sort by @Order value — same ordering the CheckoutPipeline uses at runtime
        List<PipelineProcessor> sorted = pipelineProcessors.stream()
            .sorted(Comparator.comparingInt(p -> {
                org.springframework.core.annotation.Order orderAnn =
                    p.getClass().getAnnotation(org.springframework.core.annotation.Order.class);
                return orderAnn != null ? orderAnn.value() : Integer.MAX_VALUE;
            }))
            .collect(Collectors.toList());

        // Static descriptions matching the Javadoc of each processor
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("validateCartProcessor",
            "Verifies the cart is non-empty and all cart items reference live products. "
            + "ATG: /atg/commerce/checkout/processor/ValidateCartContents");
        descriptions.put("inventoryCheckProcessor",
            "Checks available stock for every cart item without committing. "
            + "ATG: /atg/commerce/inventory/InventoryCheckProcessor");
        descriptions.put("priceCalculationProcessor",
            "Builds the Order entity, calculates subtotal, TVA (20%), and shipping (free &ge; &euro;75). "
            + "ATG: PricingEngine — ItemPriceCalculator + TaxCalculator + ShippingPriceCalculator");
        descriptions.put("paymentAuthProcessor",
            "Simulates payment gateway authorisation. Use REFUSE_TEST as payment method to trigger a decline. "
            + "ATG: /atg/commerce/payment/processor/AuthorizePaymentProcessor");
        descriptions.put("reserveInventoryProcessor",
            "Decrements stock_quantity after successful payment auth (two-phase inventory commit). "
            + "ATG: /atg/commerce/inventory/InventoryReservationProcessor");
        descriptions.put("finaliseOrderProcessor",
            "Persists the Order with status CONFIRMED and sets pipeline success flag. "
            + "ATG: /atg/commerce/checkout/processor/ProcessOrder");

        StringBuilder sb = new StringBuilder();
        sb.append("<h2 class='section-title'>Pipeline Chain Viewer</h2>");
        sb.append("<p class='breadcrumb'>/ dyn / admin / pipeline /</p>");
        sb.append("<p style='margin-bottom:14px;color:#555'>Pipeline: <code>checkoutPipeline</code> &mdash; ")
          .append(sorted.size()).append(" processor").append(sorted.size() == 1 ? "" : "s")
          .append(" registered</p>");

        sb.append("<table class='data-table'>");
        sb.append("<thead><tr><th>#</th><th>@Order</th><th>Bean Name</th><th>Class</th><th>Description</th></tr></thead>");
        sb.append("<tbody>");

        boolean alt = false;
        int seq = 1;
        for (PipelineProcessor proc : sorted) {
            org.springframework.core.annotation.Order orderAnn =
                proc.getClass().getAnnotation(org.springframework.core.annotation.Order.class);
            int orderVal   = orderAnn != null ? orderAnn.value() : 0;
            String beanName = toBeanName(proc.getClass().getSimpleName());
            String desc     = descriptions.getOrDefault(beanName, proc.getClass().getSimpleName());
            String bg       = alt ? " class='alt'" : "";

            sb.append("<tr").append(bg).append(">")
              .append("<td><strong>").append(seq++).append("</strong></td>")
              .append("<td><code>@Order(").append(orderVal).append(")</code></td>")
              .append("<td><code>").append(esc(beanName)).append("</code></td>")
              .append("<td><code style='font-size:11px'>")
                .append(esc(proc.getClass().getName())).append("</code></td>")
              .append("<td style='font-size:12px'>").append(desc).append("</td>")
              .append("</tr>");
            alt = !alt;
        }
        sb.append("</tbody></table>");

        // Pipeline execution diagram
        sb.append("<h3 class='sub-title' style='margin-top:20px'>Execution Flow</h3>");
        sb.append("<div class='pipeline-flow'>");
        for (int i = 0; i < sorted.size(); i++) {
            PipelineProcessor proc = sorted.get(i);
            sb.append("<div class='pipeline-step'>")
              .append("<div class='step-num'>").append(i + 1).append("</div>")
              .append("<div class='step-name'>").append(esc(toBeanName(proc.getClass().getSimpleName()))).append("</div>")
              .append("</div>");
            if (i < sorted.size() - 1) {
                sb.append("<div class='pipeline-arrow'>&rarr;</div>");
            }
        }
        sb.append("</div>");

        return html(layout("Pipeline Chain Viewer", "/ dyn / admin / pipeline /", sb.toString()));
    }

    // ------------------------------------------------------------------
    // System Properties
    // ------------------------------------------------------------------

    /**
     * JVM system properties dump.
     * ATG analogy: the dyn/admin System Properties page at
     * {@code /dyn/admin/system.jhtml} which exposes Java system properties
     * and Nucleus environment settings.
     */
    @GetMapping("/system")
    public ResponseEntity<String> systemProperties() {
        RuntimeMXBean  runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean   mem     = ManagementFactory.getMemoryMXBean();

        long heapUsed  = mem.getHeapMemoryUsage().getUsed()      / (1024 * 1024);
        long heapMax   = mem.getHeapMemoryUsage().getMax()       / (1024 * 1024);
        long heapCommit= mem.getHeapMemoryUsage().getCommitted() / (1024 * 1024);
        long nonHeapUsed = mem.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);

        StringBuilder sb = new StringBuilder();
        sb.append("<h2 class='section-title'>System Properties</h2>");
        sb.append("<p class='breadcrumb'>/ dyn / admin / system /</p>");

        // Runtime summary
        sb.append("<h3 class='sub-title'>Runtime Summary</h3>");
        sb.append("<table class='data-table'>");
        sb.append("<thead><tr><th>Property</th><th>Value</th></tr></thead><tbody>");
        sb.append(tr2("JVM Name",          runtime.getVmName(),           false));
        sb.append(tr2("JVM Vendor",        runtime.getVmVendor(),         true));
        sb.append(tr2("JVM Version",       runtime.getVmVersion(),        false));
        sb.append(tr2("Java Version",      System.getProperty("java.version"), true));
        sb.append(tr2("Heap Used",         heapUsed + " MB",              false));
        sb.append(tr2("Heap Committed",    heapCommit + " MB",            true));
        sb.append(tr2("Heap Max",          heapMax + " MB",               false));
        sb.append(tr2("Non-Heap Used",     nonHeapUsed + " MB",           true));
        sb.append(tr2("Available CPUs",    String.valueOf(Runtime.getRuntime().availableProcessors()), false));
        sb.append(tr2("System Uptime",     runtime.getUptime() / 1000 + " seconds", true));
        sb.append("</tbody></table>");

        // All JVM system properties (sorted)
        sb.append("<h3 class='sub-title' style='margin-top:20px'>JVM System Properties</h3>");
        sb.append("<p style='margin-bottom:8px;color:#666;font-size:12px'>")
          .append(System.getProperties().size()).append(" properties</p>");
        sb.append("<table class='data-table'>");
        sb.append("<thead><tr><th>Key</th><th>Value</th></tr></thead><tbody>");

        boolean alt = false;
        List<String> keys = System.getProperties().stringPropertyNames()
                                  .stream().sorted().collect(Collectors.toList());
        for (String key : keys) {
            String val  = System.getProperty(key, "");
            // Mask anything that looks like a credential
            if (key.toLowerCase().contains("password") || key.toLowerCase().contains("secret")) {
                val = "***";
            }
            String bg = alt ? " class='alt'" : "";
            sb.append("<tr").append(bg).append(">")
              .append("<td><code>").append(esc(key)).append("</code></td>")
              .append("<td style='word-break:break-all;font-size:12px'>").append(esc(val)).append("</td>")
              .append("</tr>");
            alt = !alt;
        }
        sb.append("</tbody></table>");

        return html(layout("System Properties", "/ dyn / admin / system /", sb.toString()));
    }

    // ------------------------------------------------------------------
    // HTML generation helpers
    // ------------------------------------------------------------------

    /**
     * Master layout — wraps any content string in the classic ATG Dyn/Admin chrome.
     * ATG analogy: the base layout JHTML template shared by all dyn/admin pages.
     *
     * @param title     page title shown in {@code <title>} and header
     * @param breadcrumb ATG-style path shown below the section heading
     * @param content   HTML body content for the main content area
     * @return complete HTML document as a string
     */
    private String layout(String title, String breadcrumb, String content) {
        return "<!DOCTYPE html>"
            + "<html lang='en'>"
            + "<head>"
            + "<meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
            + "<title>Dyn/Admin — " + esc(title) + " — " + APP_NAME + "</title>"
            + "<style>"
            + "* { box-sizing: border-box; margin: 0; padding: 0; }"
            + "body { font-family: Arial, Helvetica, sans-serif; font-size: 13px; "
            +        "background: #e8eaf0; color: #222; }"

            // Header bar — ATG dark navy
            + ".header { background: #1C3660; color: #fff; padding: 0; "
            +           "border-bottom: 3px solid #f0a500; display: flex; align-items: stretch; }"
            + ".header-brand { padding: 10px 18px; background: #132747; "
            +                 "font-size: 18px; font-weight: bold; letter-spacing: 1px; "
            +                 "border-right: 1px solid #2d4f8a; white-space: nowrap; }"
            + ".header-brand span { color: #f0a500; }"
            + ".header-app { padding: 10px 18px; font-size: 12px; color: #99b3d4; "
            +               "align-self: center; }"
            + ".header-version { margin-left: auto; padding: 10px 18px; font-size: 11px; "
            +                   "color: #6688aa; align-self: center; }"

            // Layout
            + ".layout { display: flex; min-height: calc(100vh - 44px); }"

            // Sidebar — mimics ATG's left-nav component tree
            + ".sidebar { width: 210px; background: #1e2d4a; flex-shrink: 0; "
            +            "padding-top: 10px; }"
            + ".sidebar-title { color: #8aa8cc; font-size: 10px; font-weight: bold; "
            +                  "text-transform: uppercase; padding: 8px 14px 4px; "
            +                  "letter-spacing: 1px; }"
            + ".sidebar a { display: block; padding: 7px 14px 7px 20px; color: #b8cce4; "
            +              "text-decoration: none; font-size: 12px; "
            +              "border-left: 3px solid transparent; }"
            + ".sidebar a:hover { background: #253d62; color: #fff; "
            +                    "border-left-color: #f0a500; }"
            + ".sidebar a.active { background: #2d4f80; color: #fff; "
            +                     "border-left-color: #f0a500; font-weight: bold; }"
            + ".sidebar-sep { border-top: 1px solid #2d3f5a; margin: 6px 0; }"

            // Content area
            + ".content { flex: 1; padding: 20px 24px; background: #fff; }"

            // Section title — ATG dark header band style
            + ".section-title { font-size: 16px; font-weight: bold; color: #1C3660; "
            +                  "padding-bottom: 6px; border-bottom: 2px solid #1C3660; "
            +                  "margin-bottom: 10px; }"
            + ".sub-title { font-size: 13px; font-weight: bold; background: #1C3660; "
            +              "color: #fff; padding: 5px 10px; margin-bottom: 0; }"
            + ".breadcrumb { font-size: 11px; color: #888; margin-bottom: 14px; "
            +               "font-family: monospace; }"

            // Tables
            + ".data-table { width: 100%; border-collapse: collapse; "
            +               "margin-bottom: 14px; font-size: 12px; }"
            + ".data-table th { background: #2c4a7a; color: #fff; padding: 6px 10px; "
            +                  "text-align: left; font-size: 11px; "
            +                  "text-transform: uppercase; letter-spacing: 0.5px; }"
            + ".data-table td { padding: 5px 10px; border-bottom: 1px solid #dde3f0; "
            +                  "vertical-align: top; }"
            + ".data-table tr.alt td { background: #f0f4ff; }"
            + ".data-table tr:hover td { background: #e6ecf8; }"
            + "code { background: #f0f4ff; padding: 1px 4px; border-radius: 2px; "
            +        "font-size: 11px; color: #1C3660; }"

            // Badges
            + ".badge { display: inline-block; padding: 2px 8px; border-radius: 10px; "
            +          "font-size: 10px; font-weight: bold; background: #1C3660; color: #fff; }"
            + ".badge-confirmed { background: #1a7a2e; }"
            + ".badge-pending   { background: #b07000; }"
            + ".badge-cancelled { background: #8a1a1a; }"

            // Buttons
            + ".btn-danger { background: #c0392b; color: #fff; border: none; "
            +               "padding: 3px 10px; cursor: pointer; font-size: 11px; "
            +               "border-radius: 3px; }"
            + ".btn-danger:hover { background: #922b21; }"

            // Pagination
            + ".pagination { padding: 10px 0; }"
            + ".page-link { display: inline-block; padding: 3px 9px; margin: 0 2px; "
            +              "background: #1C3660; color: #fff; text-decoration: none; "
            +              "border-radius: 3px; font-size: 12px; }"
            + ".page-link:hover { background: #2d5090; }"
            + ".page-current { display: inline-block; padding: 3px 9px; margin: 0 2px; "
            +                 "background: #f0a500; color: #fff; border-radius: 3px; "
            +                 "font-size: 12px; font-weight: bold; }"

            // Pipeline flow diagram
            + ".pipeline-flow { display: flex; align-items: center; flex-wrap: wrap; "
            +                  "gap: 4px; padding: 14px 0; }"
            + ".pipeline-step { background: #1C3660; color: #fff; padding: 8px 12px; "
            +                  "border-radius: 4px; text-align: center; min-width: 120px; }"
            + ".step-num { font-size: 10px; color: #f0a500; font-weight: bold; }"
            + ".step-name { font-size: 11px; font-family: monospace; margin-top: 2px; }"
            + ".pipeline-arrow { color: #1C3660; font-size: 20px; font-weight: bold; }"

            // Footer
            + ".footer { background: #1C3660; color: #6688aa; font-size: 10px; "
            +           "padding: 6px 18px; text-align: center; }"

            + "</style>"
            + "</head>"
            + "<body>"

            // Header
            + "<div class='header'>"
            + "<div class='header-brand'><span>Dyn</span>/Admin</div>"
            + "<div class='header-app'>" + esc(APP_NAME) + "</div>"
            + "<div class='header-version'>Spring Boot " + APP_VERSION + "</div>"
            + "</div>"

            // Body layout
            + "<div class='layout'>"

            // Sidebar
            + "<div class='sidebar'>"
            + "<div class='sidebar-title'>Nucleus</div>"
            + navLink("/dyn/admin",          "Dashboard",          breadcrumb.contains("admin /") && !breadcrumb.contains("nucleus") && !breadcrumb.contains("repo") && !breadcrumb.contains("pipeline") && !breadcrumb.contains("system"))
            + navLink("/dyn/admin/nucleus",  "Component Browser",  breadcrumb.contains("nucleus"))
            + "<div class='sidebar-sep'></div>"
            + "<div class='sidebar-title'>Repositories</div>"
            + navLink("/dyn/admin/repo/users",    "User Repository",    breadcrumb.contains("users"))
            + navLink("/dyn/admin/repo/products", "Product Repository", breadcrumb.contains("products"))
            + navLink("/dyn/admin/repo/orders",   "Order Repository",   breadcrumb.contains("orders"))
            + "<div class='sidebar-sep'></div>"
            + "<div class='sidebar-title'>Commerce</div>"
            + navLink("/dyn/admin/pipeline", "Pipeline Viewer",    breadcrumb.contains("pipeline"))
            + "<div class='sidebar-sep'></div>"
            + "<div class='sidebar-title'>System</div>"
            + navLink("/dyn/admin/system",   "System Properties",  breadcrumb.contains("system"))
            + "</div>"

            // Main content
            + "<div class='content'>" + content + "</div>"
            + "</div>"

            // Footer
            + "<div class='footer'>Oracle ATG Dynamo Administration &mdash; "
            + APP_NAME + " &mdash; "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            + "</div>"
            + "</body></html>";
    }

    // ------------------------------------------------------------------
    // Private utility methods
    // ------------------------------------------------------------------

    /** Wrap the given HTML document in a 200 text/html ResponseEntity. */
    private ResponseEntity<String> html(String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body);
    }

    /** Render a sidebar navigation link, marking it active when the breadcrumb matches. */
    private String navLink(String href, String label, boolean active) {
        return "<a href='" + href + "'" + (active ? " class='active'" : "") + ">"
             + esc(label) + "</a>";
    }

    /** Render a 3-column data-table row (for the repositories count table on the dashboard). */
    private String tr(String col1, String col2, String col3, boolean alt) {
        String bg = alt ? " class='alt'" : "";
        return "<tr" + bg + "><td>" + esc(col1) + "</td><td><code>" + esc(col2)
             + "</code></td><td><strong>" + esc(col3) + "</strong></td></tr>";
    }

    /** Render a 2-column key/value data-table row. */
    private String tr2(String key, String value, boolean alt) {
        String bg = alt ? " class='alt'" : "";
        return "<tr" + bg + "><td><strong>" + esc(key) + "</strong></td><td>" + esc(value) + "</td></tr>";
    }

    /** HTML-escape a string to prevent XSS. */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Null-safe string, returning empty string for null. */
    private String nvl(String s) {
        return s != null ? s : "";
    }

    /**
     * Check whether a bean is assignable to a package prefix.
     * We check the actual class package from the bean definition to avoid false
     * positives from Spring infrastructure beans that happen to share name fragments.
     */
    private boolean isOfType(String beanName, String packagePrefix) {
        try {
            Object bean = applicationContext.getBean(beanName);
            return bean.getClass().getName().startsWith(packagePrefix);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Build a [beanName, className, scope] array for the Nucleus component table.
     * Scope is resolved from the BeanDefinition — mirrors ATG's concept of
     * singleton (global-scoped) vs prototype (request/session-scoped) Nucleus components.
     */
    private String[] beanRow(String beanName) {
        String className = "—";
        String scope = "singleton";
        try {
            Object bean = applicationContext.getBean(beanName);
            className = bean.getClass().getName();
            // getBeanDefinition lives on ConfigurableListableBeanFactory, accessible
            // via the ConfigurableApplicationContext — mirrors ATG's Nucleus component
            // scope inspection (global / session / request).
            if (applicationContext.getBeanFactory().containsBeanDefinition(beanName)) {
                String defScope = applicationContext.getBeanFactory()
                                                    .getBeanDefinition(beanName).getScope();
                scope = (defScope == null || defScope.isEmpty()) ? "singleton" : defScope;
            }
        } catch (Exception ignored) {
            // Bean may be a factory or abstract; provide best-effort info
        }
        return new String[]{beanName, className, scope};
    }

    /**
     * Convert a class simple name to its Spring bean name (lowercase first letter).
     * e.g. {@code ValidateCartProcessor} -> {@code validateCartProcessor}
     */
    private String toBeanName(String className) {
        if (className == null || className.isEmpty()) return className;
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /** Return a CSS class name for an order status badge. */
    private String statusBadgeClass(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> "badge-confirmed";
            case "PENDING"   -> "badge-pending";
            case "CANCELLED" -> "badge-cancelled";
            default          -> "";
        };
    }
}

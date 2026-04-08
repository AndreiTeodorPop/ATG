package com.castorama.atg.domain.enums;

/**
 * ATG analogy: a constrained property type on a catalog item descriptor.
 * In a real GSA schema this would be an "enumerated" property on the
 * sku/product item descriptor backed by a lookup table.
 */
public enum ProductCategory {

    OUTILLAGE("Outillage"),           // Tools
    PEINTURE("Peinture & Revêtements"), // Paint & Coatings
    SOL("Sols & Murs"),               // Flooring & Wall coverings
    PLOMBERIE("Plomberie"),           // Plumbing
    ELECTRICITE("Électricité"),       // Electrical
    JARDIN("Jardin"),                 // Garden
    MENUISERIE("Menuiserie"),         // Carpentry / Joinery
    QUINCAILLERIE("Quincaillerie"),   // Hardware / Ironmongery
    CHAUFFAGE("Chauffage"),           // Heating
    CUISINE("Cuisine & Bain");        // Kitchen & Bathroom

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

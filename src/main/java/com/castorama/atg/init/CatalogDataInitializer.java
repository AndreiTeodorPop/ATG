package com.castorama.atg.init;

import com.castorama.atg.domain.enums.ProductCategory;
import com.castorama.atg.domain.model.Product;
import com.castorama.atg.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Loads sample catalogue data on startup.
 *
 * <p>ATG analogy: a {@code /atg/dynamo/service/Initial} Nucleus component or
 * a {@code GSATestUtils} data loader that populates the repository tables
 * with reference data before the application serves traffic.</p>
 *
 * <p>Products are modelled on real Castorama France catalogue items, with
 * French product names, brands, and pricing in EUR.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            log.info("Catalogue already initialised — skipping seed data load.");
            return;
        }

        List<Product> products = List.of(

            // ===================== OUTILLAGE (Tools) =====================
            Product.builder()
                .skuCode("CAST-10001")
                .name("Perceuse-visseuse sans fil BOSCH GSR 18V-55")
                .description("Perceuse-visseuse sans fil 18V avec mandrin auto-serrant 13mm. " +
                             "Couple max 55 Nm. Livré avec 2 batteries 2Ah et chargeur.")
                .listPrice(new BigDecimal("149.99"))
                .salePrice(new BigDecimal("119.99"))
                .brand("Bosch")
                .category(ProductCategory.OUTILLAGE)
                .stockQuantity(45)
                .imageUrl("https://media.castorama.fr/products/bosch-gsr18v55.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-10002")
                .name("Meuleuse d'angle DEWALT DWE4257 125mm 1100W")
                .description("Meuleuse d'angle filaire 1100W. Disque 125mm. " +
                             "Démarrage progressif et protection contre les redémarrages.")
                .listPrice(new BigDecimal("89.90"))
                .brand("DeWalt")
                .category(ProductCategory.OUTILLAGE)
                .stockQuantity(30)
                .imageUrl("https://media.castorama.fr/products/dewalt-dwe4257.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-10003")
                .name("Set de 100 embouts de vissage Stanley")
                .description("Assortiment 100 pièces : embouts PZ1, PZ2, PZ3, PH1, PH2, PH3, " +
                             "plats, Torx et douilles. Boîte de rangement incluse.")
                .listPrice(new BigDecimal("24.99"))
                .brand("Stanley")
                .category(ProductCategory.OUTILLAGE)
                .stockQuantity(200)
                .imageUrl("https://media.castorama.fr/products/stanley-embouts-100.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-10004")
                .name("Niveau laser rotatif LEICA Lino L2G")
                .description("Niveau laser vert 2 lignes croisées. Portée 20m, précision ±0,3mm/m. " +
                             "Idéal pour carrelage, pose de plaques, installation de mobilier.")
                .listPrice(new BigDecimal("219.00"))
                .brand("Leica")
                .category(ProductCategory.OUTILLAGE)
                .stockQuantity(12)
                .imageUrl("https://media.castorama.fr/products/leica-linol2g.jpg")
                .build(),

            // ===================== PEINTURE (Paint) =====================
            Product.builder()
                .skuCode("CAST-20001")
                .name("Peinture murale blanche mate DULUX Valentine 10L")
                .description("Peinture acrylique blanche mat lessivable. Couvre 100m² en 2 couches. " +
                             "Séchage rapide 2h. Faible odeur. Pour murs et plafonds intérieurs.")
                .listPrice(new BigDecimal("59.90"))
                .brand("Dulux Valentine")
                .category(ProductCategory.PEINTURE)
                .stockQuantity(80)
                .imageUrl("https://media.castorama.fr/products/dulux-blanche-mat-10l.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-20002")
                .name("Peinture façade TOLLENS Résistance+ gris clair 15L")
                .description("Peinture hydrofuge pour façades. Résistante aux UV et aux intempéries. " +
                             "Durée de vie garantie 10 ans. Teinte gris clair RAL 7035.")
                .listPrice(new BigDecimal("124.50"))
                .brand("Tollens")
                .category(ProductCategory.PEINTURE)
                .stockQuantity(25)
                .imageUrl("https://media.castorama.fr/products/tollens-facade-grisclair-15l.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-20003")
                .name("Rouleau peinture microfibre 23cm — lot de 3")
                .description("Rouleau microfibre pour peinture murale, supports lisses à semi-granuleux. " +
                             "Charge optimale, finition sans traces. Lot de 3 rouleaux.")
                .listPrice(new BigDecimal("12.99"))
                .salePrice(new BigDecimal("9.49"))
                .brand("Castorama")
                .category(ProductCategory.PEINTURE)
                .stockQuantity(150)
                .imageUrl("https://media.castorama.fr/products/rouleau-microfibre-lot3.jpg")
                .build(),

            // ===================== SOL & MURS (Flooring) =====================
            Product.builder()
                .skuCode("CAST-30001")
                .name("Carrelage grès cérame imitation béton 60x60cm — boîte 1,44m²")
                .description("Carrelage rectifié grès cérame pleine masse. Aspect béton ciré gris moyen. " +
                             "Antidérapant R9. Usage intérieur sol et mur. Boîte de 4 dalles.")
                .listPrice(new BigDecimal("34.90"))
                .brand("Castorama")
                .category(ProductCategory.SOL)
                .stockQuantity(300)
                .imageUrl("https://media.castorama.fr/products/carrelage-beton-60x60.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-30002")
                .name("Parquet stratifié chêne naturel 8mm AC4 — paquet 2,40m²")
                .description("Parquet stratifié chêne naturel. Résistance AC4 (usage intensif). " +
                             "Épaisseur 8mm, pose clipsée sans colle. Inclut sous-couche.")
                .listPrice(new BigDecimal("29.90"))
                .salePrice(new BigDecimal("24.90"))
                .brand("Quick-Step")
                .category(ProductCategory.SOL)
                .stockQuantity(120)
                .imageUrl("https://media.castorama.fr/products/quickstep-chene-8mm.jpg")
                .build(),

            // ===================== PLOMBERIE (Plumbing) =====================
            Product.builder()
                .skuCode("CAST-40001")
                .name("Robinet mitigeur cuisine GROHE Eurostyle 35cm bec haut")
                .description("Mitigeur évier chromé. Bec orientable 360°, hauteur 162mm. " +
                             "Cartouche céramique silencieuse. Débit régulé 5,7L/min.")
                .listPrice(new BigDecimal("99.00"))
                .brand("Grohe")
                .category(ProductCategory.PLOMBERIE)
                .stockQuantity(18)
                .imageUrl("https://media.castorama.fr/products/grohe-eurostyle-cuisine.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-40002")
                .name("Lot de 10 raccords multicouche à sertir 16mm")
                .description("Raccords à sertir pour tube multicouche 16mm. " +
                             "Corps laiton, joint EPDM. Norme NF. Convient eau froide et chaude jusqu'à 70°C.")
                .listPrice(new BigDecimal("18.50"))
                .brand("Comap")
                .category(ProductCategory.PLOMBERIE)
                .stockQuantity(200)
                .imageUrl("https://media.castorama.fr/products/comap-sertir-16mm-lot10.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-40003")
                .name("Chauffe-eau électrique 150L vertical THERMOR Duralis")
                .description("Chauffe-eau électrique stéatite 150L. Résistance blindée inox. " +
                             "Classe énergétique C. Garantie 5 ans cuve. " +
                             "Pression maximale 7 bars. Ø 595mm.")
                .listPrice(new BigDecimal("389.00"))
                .brand("Thermor")
                .category(ProductCategory.PLOMBERIE)
                .stockQuantity(8)
                .imageUrl("https://media.castorama.fr/products/thermor-duralis-150l.jpg")
                .build(),

            // ===================== ELECTRICITE (Electrical) =====================
            Product.builder()
                .skuCode("CAST-50001")
                .name("Ampoule LED E27 blanc chaud 9W=60W — lot de 10")
                .description("Ampoule LED standard E27. Flux lumineux 806 lm, 2700K blanc chaud. " +
                             "Durée de vie 15 000h. Dimmable non. Lot économique de 10.")
                .listPrice(new BigDecimal("19.90"))
                .salePrice(new BigDecimal("15.90"))
                .brand("Philips")
                .category(ProductCategory.ELECTRICITE)
                .stockQuantity(500)
                .imageUrl("https://media.castorama.fr/products/philips-led-e27-lot10.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-50002")
                .name("Tableau électrique 13 modules avec coffret LEGRAND")
                .description("Coffret de distribution encastré 1 rangée 13 modules. " +
                             "Porte opaque, bornes de connexion rapide. Norme IEC 60439-3.")
                .listPrice(new BigDecimal("44.90"))
                .brand("Legrand")
                .category(ProductCategory.ELECTRICITE)
                .stockQuantity(35)
                .imageUrl("https://media.castorama.fr/products/legrand-coffret-13m.jpg")
                .build(),

            // ===================== JARDIN (Garden) =====================
            Product.builder()
                .skuCode("CAST-60001")
                .name("Tondeuse à gazon électrique BOSCH ROTAK 37 1400W")
                .description("Tondeuse filaire 1400W. Largeur de coupe 37cm. " +
                             "Hauteur réglable 20-70mm (6 positions). Bac de ramassage 40L.")
                .listPrice(new BigDecimal("139.99"))
                .brand("Bosch")
                .category(ProductCategory.JARDIN)
                .stockQuantity(22)
                .imageUrl("https://media.castorama.fr/products/bosch-rotak37.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-60002")
                .name("Terreau universel COMPO 50L")
                .description("Terreau enrichi en engrais longue durée (3 mois). " +
                             "Pour toutes plantes d'intérieur et d'extérieur. pH équilibré. 50 litres.")
                .listPrice(new BigDecimal("11.99"))
                .brand("Compo")
                .category(ProductCategory.JARDIN)
                .stockQuantity(180)
                .imageUrl("https://media.castorama.fr/products/compo-terreau-50l.jpg")
                .build(),

            // ===================== MENUISERIE (Carpentry) =====================
            Product.builder()
                .skuCode("CAST-70001")
                .name("Porte intérieure isolante bois blanc laqué 204x73cm")
                .description("Porte en bois massif laqué blanc avec âme alvéolaire isolante. " +
                             "Dimensions 204x73cm (poignée non incluse). Réversible gauche/droite.")
                .listPrice(new BigDecimal("179.00"))
                .brand("Castorama")
                .category(ProductCategory.MENUISERIE)
                .stockQuantity(15)
                .imageUrl("https://media.castorama.fr/products/porte-bois-blanc-204x73.jpg")
                .build(),

            // ===================== QUINCAILLERIE (Hardware) =====================
            Product.builder()
                .skuCode("CAST-80001")
                .name("Cheville nylon universelle 8x40mm — boîte de 100")
                .description("Cheville universelle à expansion pour fixations dans béton, " +
                             "brique, béton cellulaire. Nylon haute densité. Boîte de 100 unités.")
                .listPrice(new BigDecimal("7.99"))
                .brand("Fischer")
                .category(ProductCategory.QUINCAILLERIE)
                .stockQuantity(1000)
                .imageUrl("https://media.castorama.fr/products/fischer-cheville-8x40-100.jpg")
                .build(),

            Product.builder()
                .skuCode("CAST-80002")
                .name("Serrure de sécurité VACHETTE 3 points A2P*")
                .description("Serrure multipoints 3 points certifiée A2P 1 étoile. " +
                             "Corps acier, 5 clés livrées. Entraxe 85mm. Compatible entrée blindée.")
                .listPrice(new BigDecimal("89.90"))
                .salePrice(new BigDecimal("74.90"))
                .brand("Vachette")
                .category(ProductCategory.QUINCAILLERIE)
                .stockQuantity(28)
                .imageUrl("https://media.castorama.fr/products/vachette-a2p-3pts.jpg")
                .build(),

            // ===================== CHAUFFAGE (Heating) =====================
            Product.builder()
                .skuCode("CAST-90001")
                .name("Radiateur électrique à inertie sèche ATLANTIC Galéa 1500W")
                .description("Radiateur électrique à inertie sèche 1500W. Programmation 24h/7j. " +
                             "Thermostat électronique ±0,5°C. Détection de fenêtre ouverte. " +
                             "Dimensions 93x55cm.")
                .listPrice(new BigDecimal("299.00"))
                .brand("Atlantic")
                .category(ProductCategory.CHAUFFAGE)
                .stockQuantity(10)
                .imageUrl("https://media.castorama.fr/products/atlantic-galea-1500w.jpg")
                .build(),

            // ===================== CUISINE & BAIN (Kitchen & Bath) =====================
            Product.builder()
                .skuCode("CAST-00001")
                .name("Évier inox 1 bac sous-meuble 60cm FRANKE")
                .description("Évier inox brossé 1 bac avec égouttoir. Dimensions totales 116x50cm. " +
                             "Bac 40x40cm profondeur 18cm. Inclut bonde et trop-plein.")
                .listPrice(new BigDecimal("159.00"))
                .brand("Franke")
                .category(ProductCategory.CUISINE)
                .stockQuantity(14)
                .imageUrl("https://media.castorama.fr/products/franke-evier-inox-1bac.jpg")
                .build()
        );

        productRepository.saveAll(products);
        log.info("Catalogue initialised with {} products across {} categories.",
                 products.size(), ProductCategory.values().length);
    }
}

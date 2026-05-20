package com.cyna.modules.product.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "product_translations", schema = "product_schema")
public class ProductTranslationJpaEntity {

    @EmbeddedId
    private ProductTranslationId id = new ProductTranslationId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private ProductJpaEntity product;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "service_description", nullable = false, columnDefinition = "text")
    private String serviceDescription;

    @Column(name = "technical_description", nullable = false, columnDefinition = "text")
    private String technicalDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "highlight_points", nullable = false, columnDefinition = "jsonb")
    private List<String> highlightPoints;

    public ProductTranslationJpaEntity() {}

    // ── Convenience factory ───────────────────────────────────────────────────

    public static ProductTranslationJpaEntity of(ProductJpaEntity product,
                                                  String locale,
                                                  String name,
                                                  String serviceDescription,
                                                  String technicalDescription,
                                                  List<String> highlightPoints) {
        var t = new ProductTranslationJpaEntity();
        t.id = new ProductTranslationId(product.getId(), locale);
        t.product             = product;
        t.name                = name;
        t.serviceDescription  = serviceDescription;
        t.technicalDescription = technicalDescription;
        t.highlightPoints     = highlightPoints;
        return t;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public ProductTranslationId getId()     { return id; }
    public void setId(ProductTranslationId id) { this.id = id; }

    public String getLocale() { return id.getLocale(); }

    public ProductJpaEntity getProduct()  { return product; }
    public void setProduct(ProductJpaEntity product) { this.product = product; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String sd) { this.serviceDescription = sd; }

    public String getTechnicalDescription() { return technicalDescription; }
    public void setTechnicalDescription(String td) { this.technicalDescription = td; }

    public List<String> getHighlightPoints() { return highlightPoints; }
    public void setHighlightPoints(List<String> hp) { this.highlightPoints = hp; }
}

package com.example.erpsystem.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String type; // "IN" or "OUT"

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    // Default constructor
    public StockTransaction() {}

    // Constructor
    public StockTransaction(Product product, Integer quantity, String type, LocalDateTime timestamp, BigDecimal pricePerUnit) {
        this.product = product;
        this.quantity = quantity;
        this.type = type;
        this.timestamp = timestamp;
        setPricePerUnit(pricePerUnit);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(BigDecimal pricePerUnit) {
        if (pricePerUnit == null || pricePerUnit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price per unit must be positive.");
        }
        this.pricePerUnit = pricePerUnit.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalValue() {
        return pricePerUnit.multiply(new BigDecimal(quantity));
    }
    
    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = true)
    private Invoice invoice;

   

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }
    
    public void validateTransaction() {
        if ("OUT".equals(type) && invoice == null) {
            throw new IllegalArgumentException("Sales (OUT) transactions must have an associated invoice");
        }

    }

}

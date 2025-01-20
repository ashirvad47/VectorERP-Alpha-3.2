package com.example.erpsystem.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class StockTransactionBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_transaction_id", nullable = false)
    private StockTransaction stockTransaction;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime billDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "bill_type", nullable = false)
    private String billType; // "SALE" or "PURCHASE"

    @Column(name = "bill_file_path")
    private String billFilePath;

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public StockTransaction getStockTransaction() {
		return stockTransaction;
	}

	public void setStockTransaction(StockTransaction stockTransaction) {
		this.stockTransaction = stockTransaction;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getBillDate() {
		return billDate;
	}

	public void setBillDate(LocalDateTime billDate) {
		this.billDate = billDate;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getBillType() {
		return billType;
	}

	public void setBillType(String billType) {
		this.billType = billType;
	}

	public String getBillFilePath() {
		return billFilePath;
	}

	public void setBillFilePath(String billFilePath) {
		this.billFilePath = billFilePath;
	}

	// Default constructor
    public StockTransactionBill() {}

    public StockTransactionBill(StockTransaction stockTransaction, String description, LocalDateTime billDate, BigDecimal totalAmount, String billType) {
        this.stockTransaction = stockTransaction;
        this.description = description;
        this.billDate = billDate;
        this.totalAmount = totalAmount;
        this.billType = billType;
    }

    public static StockTransactionBill createBill(StockTransaction stockTransaction) {
        BigDecimal totalAmount = stockTransaction.getTotalValue();
        String description = "OUT".equals(stockTransaction.getType())
                ? "Expense for stock purchase"
                : "Income from stock sale";
        String billType = "OUT".equals(stockTransaction.getType()) ? "PURCHASE" : "SALE";

        return new StockTransactionBill(
                stockTransaction,
                description,
                LocalDateTime.now(),
                totalAmount,
                billType
        );
    }

}

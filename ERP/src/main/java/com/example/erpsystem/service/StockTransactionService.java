package com.example.erpsystem.service;

import com.example.erpsystem.model.*;
import com.example.erpsystem.repository.*;
import java.io.IOException;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockTransactionService {
	
	@Value("${stock.bill.storage.path}")
    private String stockBillStoragePath; 
	
    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;
    private final TransactionService transactionService;
    private final StockTransactionBillRepository stockTransactionBillRepository;
    private final ProductService productService;
    public StockTransactionService(
            StockTransactionRepository stockTransactionRepository,
            ProductRepository productRepository,
            TransactionService transactionService,
            StockTransactionBillRepository stockTransactionBillRepository,
            ProductService productService
    ) {
        this.stockTransactionRepository = stockTransactionRepository;
        this.productRepository = productRepository;
        this.transactionService = transactionService;
        this.stockTransactionBillRepository = stockTransactionBillRepository;
        this.productService= productService;
    }
    
   
    public StockTransaction saveTransaction(StockTransaction transaction) {
        // Validate based on type
        if ("IN".equals(transaction.getType())) {
            // Purchase validation
            if (transaction.getInvoice() != null) {
                throw new IllegalArgumentException("Purchase transactions should not have an invoice");
            }
        } else if ("OUT".equals(transaction.getType())) {
            // Sales validation
            if (transaction.getInvoice() == null) {
                throw new IllegalArgumentException("Sales transactions must have an invoice");
            }
        }

        transaction.setTimestamp(LocalDateTime.now());
        return stockTransactionRepository.save(transaction);
    }
    
    public StockTransaction createPurchaseTransaction(Long productId, Integer quantity, BigDecimal pricePerUnit) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        StockTransaction transaction = new StockTransaction();
        transaction.setProduct(product);
        transaction.setQuantity(quantity);
        transaction.setPricePerUnit(pricePerUnit);
        transaction.setType("IN");
        transaction.setTimestamp(LocalDateTime.now());

        return saveTransaction(transaction);
    }

    @Transactional
    public StockTransaction logTransaction(
            Long productId,
            Integer quantity,
            String type,
            BigDecimal pricePerUnit,
            Invoice invoice) {
    	
    	
    	 productService.updateProductStock(productId, quantity, type);
        
        // Create and validate the stock transaction
        StockTransaction transaction = new StockTransaction();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product with ID " + productId + " not found"));
            
        transaction.setProduct(product);
        transaction.setQuantity(quantity);
        transaction.setType(type);
        transaction.setPricePerUnit(pricePerUnit);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInvoice(invoice);
        
        // Validate the transaction
        transaction.validateTransaction();
        
        // Save the stock transaction
        StockTransaction savedTransaction = stockTransactionRepository.save(transaction);
        
        // Create corresponding accounting transaction if not part of an invoice
        if (invoice == null) {
            String description = String.format(
                "%s of %d units of %s at %.2f per unit",
                type.equals("IN") ? "Purchase" : "Sale",
                quantity,
                product.getName(),
                pricePerUnit.doubleValue()
            );
            
            String transactionType = type.equals("IN") ? "EXPENSE" : "INCOME";
            
            transactionService.recordTransaction(
                description,
                savedTransaction.getTotalValue().doubleValue(),
                transactionType,
                null
            );
        }
        
        return savedTransaction;
    }
    
    

    // Helper method to get product
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    }

   
    
    public String generateBillForTransaction(Long transactionId) {
        // First check if the transaction exists
        StockTransaction transaction = stockTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

        // Check if a bill already exists
        StockTransactionBill existingBill = stockTransactionBillRepository.findByStockTransaction(transaction)
                .orElse(null);

        if (existingBill != null && existingBill.getBillFilePath() != null) {
            // If the bill exists and has a file path, verify if the file exists
            File billFile = new File(existingBill.getBillFilePath());
            if (billFile.exists()) {
                return existingBill.getBillFilePath();
            }
        }

        // If we get here, either there's no bill or the PDF file is missing
        // Generate a new bill
        StockTransactionBill newBill = createOrUpdateBill(transaction);
        return newBill.getBillFilePath();
    }


    
    public List<StockTransaction> getTransactionsByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return stockTransactionRepository.findAll().stream()
                .filter(transaction -> transaction.getProduct().equals(product))
                .toList();
    }

    public List<StockTransaction> getAllTransactions() {
        return stockTransactionRepository.findAll();
    }

    public String generateBillPdf(StockTransactionBill bill) {
        // Generate a temporary bill number if ID is not yet assigned
        String billIdentifier = bill.getId() != null ? 
            bill.getId().toString() : 
            "TEMP_" + System.currentTimeMillis();

        String billFilePath = stockBillStoragePath + File.separator + 
                             "Stock_Bill_" + bill.getStockTransaction().getId() + "_" + billIdentifier + ".pdf";

        try (PdfWriter writer = new PdfWriter(new FileOutputStream(billFilePath));
             PdfDocument pdfDoc = new PdfDocument(writer)) {
            
            PageSize receiptSize = new PageSize(226.77f, 340.16f);
            Document document = new Document(pdfDoc, receiptSize);
            document.setMargins(10, 10, 10, 10);

            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // University Header
            Paragraph universityHeader = new Paragraph("SIKSHA 'O' ANUSANDHAN")
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);

            // Subtitle with underline
            Paragraph subtitle = new Paragraph("STOCK BILL")
                .setFont(boldFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10)
                .setBorderBottom(new SolidBorder(1));

            // Create table for details
            Table detailsTable = new Table(2).setWidth(206.77f);
            detailsTable.setFont(regularFont).setFontSize(8);

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            
            // Add rows to the table
            addReceiptRow(detailsTable, "Transaction ID", bill.getStockTransaction().getId().toString());
            addReceiptRow(detailsTable, "Date", bill.getBillDate().format(formatter));
            addReceiptRow(detailsTable, "Transaction Type", bill.getBillType());
            
            // Product Details
            StockTransaction stockTx = bill.getStockTransaction();
            addReceiptRow(detailsTable, "Product", stockTx.getProduct().getName());
            addReceiptRow(detailsTable, "Quantity", stockTx.getQuantity().toString() + " units");
            addReceiptRow(detailsTable, "Price/Unit", "$" + stockTx.getPricePerUnit().toString());

            // If it's a sale, add invoice reference
            if (stockTx.getInvoice() != null) {
                addReceiptRow(detailsTable, "Invoice Ref", stockTx.getInvoice().getInvoiceNumber());
            }

            // Add total amount with bold formatting
            Cell amountLabelCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Total Amount").setFont(boldFont).setFontSize(9));
            Cell amountValueCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("$" + bill.getTotalAmount().toString()).setFont(boldFont).setFontSize(9));
            detailsTable.addCell(amountLabelCell);
            detailsTable.addCell(amountValueCell);

            // Generate QR Code content
            String qrContent = String.format("TxID:%d|Type:%s|Product:%s|Qty:%d|PPU:$%s|Total:$%s|Date:%s",
                bill.getStockTransaction().getId(),
                bill.getBillType(),
                stockTx.getProduct().getName(),
                stockTx.getQuantity(),
                stockTx.getPricePerUnit().toString(),
                bill.getTotalAmount().toString(),
                bill.getBillDate().format(formatter)
            );

            // Create and add QR Code
            BarcodeQRCode qrCode = new BarcodeQRCode(qrContent);
            Image qrImage = new Image(qrCode.createFormXObject(pdfDoc))
                .setWidth(50)
                .setHeight(50)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

            // Footer
            Paragraph footer = new Paragraph("*** FOR OFFICE USE ONLY ***")
                .setFont(regularFont)
                .setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);

            // Add all elements to the document
            document.add(universityHeader);
            document.add(subtitle);
            document.add(new Paragraph("\n").setFontSize(5));
            document.add(detailsTable);
            document.add(new Paragraph("\n").setFontSize(5));
            document.add(qrImage);
            document.add(footer);

            document.close();
            return billFilePath;

        } catch (IOException e) {
            throw new RuntimeException("Error generating stock bill PDF", e);
        }
    }

    // Update the createOrUpdateBill method to save the bill first
    private StockTransactionBill createOrUpdateBill(StockTransaction transaction) {
        String description;
        String billType;

        if (transaction.getType().equalsIgnoreCase("IN")) {
            description = "Purchase of " + transaction.getProduct().getName() +
                    " (Quantity: " + transaction.getQuantity() + ")";
            billType = "PURCHASE";
        } else {
            description = "Sale of " + transaction.getProduct().getName() +
                    " (Quantity: " + transaction.getQuantity() + ")";
            billType = "SALE";
        }

        BigDecimal totalAmount = transaction.getPricePerUnit().multiply(new BigDecimal(transaction.getQuantity()));

        // Check if bill already exists
        StockTransactionBill bill = stockTransactionBillRepository.findByStockTransaction(transaction)
                .orElse(new StockTransactionBill());

        // Update or set bill properties
        bill.setStockTransaction(transaction);
        bill.setDescription(description);
        bill.setTotalAmount(totalAmount);
        bill.setBillDate(LocalDateTime.now());
        bill.setBillType(billType);

        // Save the bill first to get the ID
        bill = stockTransactionBillRepository.save(bill);

        // Generate PDF and set the file path
        String billFilePath = generateBillPdf(bill);
        bill.setBillFilePath(billFilePath);

        // Save again with the file path
        return stockTransactionBillRepository.save(bill);
    }

    // Helper method to add rows to the receipt
    private void addReceiptRow(Table table, String label, String value) {
        Cell labelCell = new Cell().setBorder(Border.NO_BORDER)
            .add(new Paragraph(label).setFontSize(8));
        Cell valueCell = new Cell().setBorder(Border.NO_BORDER)
            .add(new Paragraph(value).setFontSize(8));
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

}
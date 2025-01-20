package com.example.erpsystem.service;

import com.example.erpsystem.model.Invoice;
import com.example.erpsystem.model.Product;
import com.example.erpsystem.model.StockTransaction;
import com.example.erpsystem.repository.InvoiceRepository;
import com.example.erpsystem.repository.StockTransactionRepository;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
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
import java.io.FileOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    
	@Value("${invoice.storage.path}")
	private String invoiceStoragePath;
	
	 private final ProductService productService;
    private final InvoiceRepository invoiceRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final TransactionService transactionService;
    public InvoiceService(InvoiceRepository invoiceRepository,
            StockTransactionRepository stockTransactionRepository,
            TransactionService transactionService,
            ProductService productService) throws IOException {
    		this.invoiceRepository = invoiceRepository;
    		this.stockTransactionRepository = stockTransactionRepository;
    		this.transactionService = transactionService;
    		 this.productService = productService;
}

    @Transactional
    public Invoice generateInvoiceAndRecordTransaction(
            String customerName,
            List<StockTransaction> stockTransactions,
            LocalDateTime dateOfSale) {
    	
    	for (StockTransaction stockTransaction : stockTransactions) {
            Product product = stockTransaction.getProduct();
            if (product.getStock() < stockTransaction.getQuantity()) {
                throw new IllegalStateException(
                    "Insufficient stock for product: " + product.getName() +
                    ". Available: " + product.getStock() +
                    ", Requested: " + stockTransaction.getQuantity()
                );
            }
        }
    	
    	//debugging, to be removed later
//    	System.out.println("Generating invoice for customer: " + customerName);
//        System.out.println("Number of transactions: " + stockTransactions.size());
//        for (StockTransaction tx : stockTransactions) {
//            System.out.println("Processing transaction - Product ID: " + 
//                tx.getProduct().getId() + ", Quantity: " + tx.getQuantity());
//        }
//        
        
        
        // Calculate total amount
        BigDecimal totalAmount = stockTransactions.stream()
                .map(tx -> tx.getPricePerUnit().multiply(BigDecimal.valueOf(tx.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create and save invoice
        Invoice invoice = new Invoice();
        invoice.setCustomerName(customerName);
        invoice.setTotalAmount(totalAmount);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        
        invoice = invoiceRepository.save(invoice);

        // Process stock transactions
        for (StockTransaction stockTransaction : stockTransactions) {
        	
        	 // Update product stock
        	productService.updateProductStock(
                    stockTransaction.getProduct().getId(),
                    stockTransaction.getQuantity(),
                    "OUT"
                );
        	
        	//save stock transaction
            stockTransaction.setType("OUT");
            stockTransaction.setInvoice(invoice);
            stockTransaction.setTimestamp(LocalDateTime.now());
            stockTransactionRepository.save(stockTransaction);
        }

        // Record financial transaction
        String description = "Income from sale, Invoice: " + invoice.getInvoiceNumber();
        transactionService.recordTransaction(
            description,
            totalAmount.doubleValue(),
            "INCOME",
            invoice
        );
        
        generateInvoicePdf(invoice);

        return invoice;
    }
    
    

//    private Invoice createInvoice(String customerName, List<StockTransaction> stockTransactions) {
//        BigDecimal totalAmount = calculateTotalAmount(stockTransactions);
//        Invoice invoice = new Invoice();
//        invoice.setCustomerName(customerName);
//        invoice.setTotalAmount(totalAmount);
//        invoice.setInvoiceNumber(generateInvoiceNumber());
//        return invoiceRepository.save(invoice);
//    }

//    private void recordFinancialTransaction(Invoice invoice) {
//        String description = "Income from sale, Invoice: " + invoice.getInvoiceNumber();
//        transactionService.recordTransaction(description, invoice.getTotalAmount().doubleValue(), "INCOME", invoice);
//    }
//    private BigDecimal calculateTotalAmount(List<StockTransaction> stockTransactions) {
//        if (stockTransactions == null || stockTransactions.isEmpty()) {
//            throw new IllegalArgumentException("Stock transactions cannot be null or empty");
//        }
//        
//        return stockTransactions.stream()
//            .map(tx -> tx.getPricePerUnit().multiply(BigDecimal.valueOf(tx.getQuantity())))
//            .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }

    private String generateInvoiceNumber() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "INV-" + uuid;
    }

	public String generateInvoicePdf(Invoice invoice) {
	    String filePath = invoiceStoragePath + "Invoice_" + invoice.getInvoiceNumber() + ".pdf";
	    //debugging purpose
	    System.out.println("Starting PDF generation for invoice: " + invoice.getInvoiceNumber());
	    
	    try (FileOutputStream fos = new FileOutputStream(filePath);
	            PdfWriter writer = new PdfWriter(fos);
	            PdfDocument pdfDoc = new PdfDocument(writer);
	            Document document = new Document(pdfDoc, PageSize.A4)) {
	        document.setMargins(40, 40, 40, 40);
	
	        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
	        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
	
	        // Header
	        Table header = new Table(2).useAllAvailableWidth();
	        
	        // Left side - University details
	        Cell universityCell = new Cell().setBorder(Border.NO_BORDER);
	        universityCell.add(new Paragraph("SIKSHA 'O' ANUSANDHAN").setFont(boldFont).setFontSize(20));
	        universityCell.add(new Paragraph("University Address Line 1").setFont(regularFont).setFontSize(10));
	        universityCell.add(new Paragraph("City, State, PIN").setFont(regularFont).setFontSize(10));
	        universityCell.add(new Paragraph("Phone: +91-XXXXXXXXXX").setFont(regularFont).setFontSize(10));
	        header.addCell(universityCell);
	
	        // Right side - Invoice details
	        Cell invoiceDetailCell = new Cell().setBorder(Border.NO_BORDER);
	        invoiceDetailCell.setTextAlignment(TextAlignment.RIGHT);
	        invoiceDetailCell.add(new Paragraph("INVOICE").setFont(boldFont).setFontSize(24).setMarginBottom(10));
	        invoiceDetailCell.add(new Paragraph("Invoice No: " + invoice.getInvoiceNumber()).setFont(boldFont).setFontSize(11));
	        invoiceDetailCell.add(new Paragraph("Date: " + invoice.getIssuedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).setFont(regularFont).setFontSize(11));
	        header.addCell(invoiceDetailCell);
	
	        // Bill To section
	        Table billTo = new Table(1).useAllAvailableWidth().setMarginTop(30);
	        Cell billToCell = new Cell().setBorder(Border.NO_BORDER);
	        billToCell.add(new Paragraph("Bill To:").setFont(boldFont).setFontSize(11));
	        billToCell.add(new Paragraph(invoice.getCustomerName()).setFont(regularFont).setFontSize(11));
	        billTo.addCell(billToCell);
	
	        // Items Table
	        Table items = new Table(new float[]{3, 1, 2, 2, 2}).useAllAvailableWidth().setMarginTop(20);
	        
	        // Table Header
	        items.addHeaderCell(createTableHeader("Item Description", boldFont));
	        items.addHeaderCell(createTableHeader("Qty", boldFont));
	        items.addHeaderCell(createTableHeader("Price/Unit", boldFont));
	        items.addHeaderCell(createTableHeader("Amount", boldFont));
	        items.addHeaderCell(createTableHeader("Tax", boldFont));
	
	        // Get stock transactions for this invoice
	        List<StockTransaction> transactions = stockTransactionRepository.findByInvoice(invoice);
	        
	        // Add items
	        BigDecimal subtotal = BigDecimal.ZERO;
	        for (StockTransaction tx : transactions) {
	            BigDecimal amount = tx.getPricePerUnit().multiply(BigDecimal.valueOf(tx.getQuantity()));
	            subtotal = subtotal.add(amount);
	            
	            items.addCell(createTableCell(tx.getProduct().getName(), regularFont));
	            items.addCell(createTableCell(tx.getQuantity().toString(), regularFont));
	            items.addCell(createTableCell("$" + tx.getPricePerUnit().toString(), regularFont));
	            items.addCell(createTableCell("$" + amount.toString(), regularFont));
	            items.addCell(createTableCell("N/A", regularFont)); // Add tax calculation if needed
	        }
	
	        // Totals section
	        Table totals = new Table(new float[]{4, 1}).useAllAvailableWidth().setMarginTop(10);
	        addTotalRow(totals, "Subtotal:", "$" + subtotal.toString(), boldFont, regularFont);
	        addTotalRow(totals, "Tax (0%):", "$0.00", boldFont, regularFont);
	        addTotalRow(totals, "Total:", "$" + invoice.getTotalAmount().toString(), boldFont, regularFont);
	
	        // Terms and Conditions
	        Paragraph terms = new Paragraph("\nTerms and Conditions:")
	            .setFont(boldFont)
	            .setFontSize(11)
	            .setMarginTop(40);
	
	        Paragraph termsContent = new Paragraph(
	            "1. Payment is due within 30 days\n" +
	            "2. Please include invoice number on your payment\n" +
	            "3. Make all checks payable to Siksha 'O' Anusandhan"
	        ).setFont(regularFont).setFontSize(9);
	
	        // Add QR Code
	        String qrContent = String.format("INV:%s|Date:%s|Amount:%s|Customer:%s",
	            invoice.getInvoiceNumber(),
	            invoice.getIssuedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
	            invoice.getTotalAmount().toString(),
	            invoice.getCustomerName()
	        );
	        
	        BarcodeQRCode qrCode = new BarcodeQRCode(qrContent);
	        Image qrImage = new Image(qrCode.createFormXObject(pdfDoc))
	            .setWidth(60)
	            .setHeight(60)
	            .setHorizontalAlignment(HorizontalAlignment.RIGHT);
	
	        // Add all elements to document
	        document.add(header);
	        document.add(billTo);
	        document.add(items);
	        document.add(totals);
	        document.add(terms);
	        document.add(termsContent);
	        document.add(qrImage);
	
	        return filePath;
	
	    } catch (IOException e) {
	        throw new RuntimeException("Error generating invoice PDF: " + e.getMessage(), e);
	    } 
	}
	
	
	
	// Helper methods
	private Cell createTableHeader(String text,PdfFont boldFont) {
	    return new Cell()
	        .setBackgroundColor(new DeviceRgb(242, 242, 242))
	        .setBorder(new SolidBorder(ColorConstants.BLACK, 1))
	        .setTextAlignment(TextAlignment.CENTER)
	        .setPadding(5)
	        .add(new Paragraph(text).setFont(boldFont).setFontSize(10));
	}
	
	private Cell createTableCell(String text, PdfFont regularFont) {
	    return new Cell()
	        .setBorder(new SolidBorder(ColorConstants.BLACK, 1))
	        .setTextAlignment(TextAlignment.CENTER)
	        .setPadding(5)
	        .add(new Paragraph(text).setFont(regularFont).setFontSize(10));
	}
	
	private void addTotalRow(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont) {
	    table.addCell(new Cell()
	        .setBorder(Border.NO_BORDER)
	        .setTextAlignment(TextAlignment.RIGHT)
	        .add(new Paragraph(label).setFont(boldFont).setFontSize(10)));
	    
	    table.addCell(new Cell()
	        .setBorder(Border.NO_BORDER)
	        .setTextAlignment(TextAlignment.RIGHT)
	        .add(new Paragraph(value).setFont(regularFont).setFontSize(10)));
	}
    
    
    
}

package com.example.erpsystem.service;

import com.example.erpsystem.model.Invoice;
import com.example.erpsystem.model.Transaction;
import com.example.erpsystem.repository.TransactionRepository;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
//import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.HorizontalAlignment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TransactionService {

    @Value("${bill.storage.path}")
    private String billStoragePath;
 
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction recordTransaction(String description, Double amount, String type, Invoice invoice) {
        if (!type.equalsIgnoreCase("INCOME") && !type.equalsIgnoreCase("EXPENSE")) {
            throw new IllegalArgumentException("Invalid transaction type. Use 'INCOME' or 'EXPENSE'.");
        }

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setType(type.toUpperCase());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInvoice(invoice);
        return transactionRepository.save(transaction);
        
        
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> getTransactionsByType(String type) {
        return transactionRepository.findByType(type.toUpperCase());
    }

    public Double calculateTotalIncome() {
        return transactionRepository.findByType("INCOME").stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Double calculateTotalExpense() {
        return transactionRepository.findByType("EXPENSE").stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Double calculateNetBalance() {
        return calculateTotalIncome() - calculateTotalExpense();
    }

    public Transaction updateTransaction(Long id, String description, Double amount, String type) {
        Optional<Transaction> transactionOpt = transactionRepository.findById(id);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found");
        }

        if (!type.equalsIgnoreCase("INCOME") && !type.equalsIgnoreCase("EXPENSE")) {
            throw new IllegalArgumentException("Invalid transaction type. Use 'INCOME' or 'EXPENSE'.");
        }

        Transaction transaction = transactionOpt.get();
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setType(type.toUpperCase());

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new RuntimeException("Transaction not found");
        }
        transactionRepository.deleteById(id);
    }

    public String generateBill(Long transactionId) throws IOException {
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            throw new RuntimeException("Transaction not found");
        }

        Transaction transaction = transactionOpt.get();
        String filePath = billStoragePath + "Bill_Transaction_" + transaction.getId() + ".pdf";

        try (PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
             PdfDocument pdfDoc = new PdfDocument(writer)) {
            
            PageSize receiptSize = new PageSize(226.77f, 340.16f); // Slightly increased height for additional details
            Document document = new Document(pdfDoc, receiptSize);
            document.setMargins(10, 10, 10, 10);

            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Header section
            Paragraph universityHeader = new Paragraph("SIKSHA 'O' ANUSANDHAN")
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);

            Paragraph subtitle = new Paragraph("Transaction Bill")
                .setFont(boldFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10)
                .setBorderBottom(new SolidBorder(1));

            // Parse description for details
            Map<String, String> parsedDetails = parseDescription(transaction.getDescription());
            
            // Create details table
            Table detailsTable = new Table(2).setWidth(206.77f);
            detailsTable.setFont(regularFont).setFontSize(8);

            // Add basic transaction details
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            addReceiptRow(detailsTable, "Transaction ID", transaction.getId().toString());
            addReceiptRow(detailsTable, "Date", transaction.getTimestamp().format(formatter));
            addReceiptRow(detailsTable, "Description", parsedDetails.get("description"));
            
            // Add quantity and price details if present
            if (parsedDetails.containsKey("units")) {
                addReceiptRow(detailsTable, "Quantity", parsedDetails.get("units") + " units");
                addReceiptRow(detailsTable, "Price per Unit", "$" + parsedDetails.get("pricePerUnit"));
            }
            
            // Add invoice number if present
            if (parsedDetails.containsKey("invoice")) {
                addReceiptRow(detailsTable, "Reference", parsedDetails.get("invoice"));
            }
            
            // Add transaction type and amount
            addReceiptRow(detailsTable, "Type", transaction.getType());
            
            // Amount row with bold formatting
            Cell amountLabelCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Amount").setFont(boldFont).setFontSize(9));
            Cell amountValueCell = new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph(String.format("$%.2f", transaction.getAmount())).setFont(boldFont).setFontSize(9));
            detailsTable.addCell(amountLabelCell);
            detailsTable.addCell(amountValueCell);

            // QR Code with enhanced information
            StringBuilder qrContent = new StringBuilder();
            qrContent.append(String.format("ID:%s|Amt:$%.2f|Type:%s|Time:%s",
                transaction.getId(), 
                transaction.getAmount(),
                transaction.getType(),
                transaction.getTimestamp().format(formatter)));
                
            if (parsedDetails.containsKey("units")) {
                qrContent.append(String.format("|Qty:%s|PPU:$%s", 
                    parsedDetails.get("units"),
                    parsedDetails.get("pricePerUnit")));
            }
            
            BarcodeQRCode qrCode = new BarcodeQRCode(qrContent.toString());
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

            // Add all elements to document
            document.add(universityHeader);
            document.add(subtitle);
            document.add(new Paragraph("\n").setFontSize(5));
            document.add(detailsTable);
            document.add(new Paragraph("\n").setFontSize(5));
            document.add(qrImage);
            document.add(footer);
            
            document.close();

        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return "Bill generated at: " + filePath;
    }

    // Helper method to parse quantity from description
    private Map<String, String> parseDescription(String description) {
        Map<String, String> result = new HashMap<>();
        result.put("description", description); // Default to full description
        
        // Pattern for quantity and price per unit
        Pattern quantityPattern = Pattern.compile("(.*?)\\s+of\\s+(\\d+)\\s+units?\\s+of\\s+(.*?)\\s+at\\s+(\\d+\\.?\\d*)\\s+per\\s+unit.*");
        Matcher quantityMatcher = quantityPattern.matcher(description);
        
        // Pattern for invoice numbers
        Pattern invoicePattern = Pattern.compile("(.*?),\\s*(Invoice:\\s*[\\w-]+)");
        Matcher invoiceMatcher = invoicePattern.matcher(description);
        
        if (quantityMatcher.matches()) {
            result.put("description", quantityMatcher.group(1) + " of " + quantityMatcher.group(3));
            result.put("quantity", quantityMatcher.group(2));
            result.put("pricePerUnit", quantityMatcher.group(4));
            result.put("units", quantityMatcher.group(2));
        } else if (invoiceMatcher.matches()) {
            result.put("description", invoiceMatcher.group(1));
            result.put("invoice", invoiceMatcher.group(2));
        }
        
        return result;
    }

    // Existing addReceiptRow method remains the same
    private void addReceiptRow(Table table, String label, String value) {
        Cell labelCell = new Cell().setBorder(Border.NO_BORDER)
            .add(new Paragraph(label).setFontSize(8));
        Cell valueCell = new Cell().setBorder(Border.NO_BORDER)
            .add(new Paragraph(value).setFontSize(8));
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    

}
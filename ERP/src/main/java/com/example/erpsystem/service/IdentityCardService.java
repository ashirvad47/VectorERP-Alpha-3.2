package com.example.erpsystem.service;

import com.example.erpsystem.model.User;
import com.example.erpsystem.repository.UserRepository;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class IdentityCardService {

    private final UserRepository userRepository;
    private static final DeviceRgb RED_COLOR = new DeviceRgb(255, 0, 0);
    private static final DeviceRgb BLACK_COLOR = new DeviceRgb(0, 0, 0);
    private static final DeviceRgb BLUE_COLOR = new DeviceRgb(0, 0, 255);
    private static final DeviceRgb CALM_GREEN = new DeviceRgb(220, 238, 220);
    private static final float CARD_WIDTH = 240f;
    private static final float CARD_HEIGHT = 336f;
    private static final float PHOTO_WIDTH = 80f;
    private static final float PHOTO_HEIGHT = 100f;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${identity.card.storage.path}")
    private String identityCardPath;

    public IdentityCardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateIdentityCard(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        
        // Create directory if it doesn't exist
        File directory = new File(identityCardPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create file path using the specified directory
        String fileName = "IdentityCard_User_" + user.getId() + ".pdf";
        String filePath = Paths.get(identityCardPath, fileName).toString();

        try (PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc, new PageSize(CARD_WIDTH, CARD_HEIGHT))) {

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Front side of the card
            Div frontSide = createCardSide(user, boldFont, normalFont, true, pdfDoc);
            document.add(frontSide);

            // Add new page for back side
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // Back side of the card
            Div backSide = createCardSide(user, boldFont, normalFont, false, pdfDoc);
            document.add(backSide);

        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }

        return "Identity card generated successfully at: " + filePath;
    }

    private Div createCardSide(User user, PdfFont boldFont, PdfFont normalFont, boolean isFrontSide, PdfDocument pdfDoc) throws IOException {
        Div cardSide = new Div()
                .setMargins(5, 5, 5, 5)
                .setBorder(new SolidBorder(BLACK_COLOR, 1))
                .setPadding(8)
                .setBackgroundColor(CALM_GREEN);

        if (isFrontSide) {
            // 1. University name
            Paragraph universityName = new Paragraph()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10)
                    .setMarginBottom(5);
            
            universityName
                    .add(new Text("S").setFont(boldFont).setFontColor(RED_COLOR))
                    .add(new Text("iksha '").setFont(boldFont).setFontColor(BLACK_COLOR))
                    .add(new Text("O").setFont(boldFont).setFontColor(RED_COLOR))
                    .add(new Text("' ").setFont(boldFont).setFontColor(BLACK_COLOR))
                    .add(new Text("A").setFont(boldFont).setFontColor(RED_COLOR))
                    .add(new Text("nusandhan").setFont(boldFont).setFontColor(BLACK_COLOR));

            cardSide.add(universityName);

            // 2. Identity Card title
            Div titleDiv = new Div()
                    .setBorder(new SolidBorder(BLUE_COLOR, 1))
                    .setPadding(3)
                    .setMarginBottom(15);
            
            titleDiv.add(new Paragraph("IDENTITY CARD")
                    .setFont(boldFont)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER));

            cardSide.add(titleDiv);

            // 3. Photo without border
            if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                try {
                    Path imagePath = Paths.get(uploadDir, user.getImageUrl().substring(user.getImageUrl().lastIndexOf("/") + 1));
                    ImageData imageData = ImageDataFactory.create(imagePath.toString());
                    Image photo = new Image(imageData)
                            .setWidth(PHOTO_WIDTH)
                            .setHeight(PHOTO_HEIGHT)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER);
                    
                    Div photoDiv = new Div()
                            .add(photo)
                            .setMarginBottom(10)
                            .setHorizontalAlignment(HorizontalAlignment.CENTER);
                    
                    cardSide.add(photoDiv);
                } catch (Exception e) {
                    System.err.println("Error adding photo to ID card: " + e.getMessage());
                }
            }

            // 4. Name below photo
            Paragraph nameParagraph = new Paragraph()
                    .add(new Text(user.getUsername().toUpperCase()).setFont(boldFont))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);

            cardSide.add(nameParagraph);

        } else {
            // Back side content
            BarcodeQRCode qrCode = new BarcodeQRCode(
                    String.format("ID: %s\nName: %s\nEmail: %s",
                            user.getId(), user.getUsername(), user.getEmail()));
            
            Image qrImage = new Image(qrCode.createFormXObject(pdfDoc))
                    .setWidth(100)
                    .setHeight(100)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            Div qrContainer = new Div()
                    .add(qrImage)
                    .setPadding(5)
                    .setMarginBottom(30);

            Div signatureDiv = new Div();
            signatureDiv.add(new Paragraph("_________________")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));
            signatureDiv.add(new Paragraph("Signature")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(8)
                    .setFont(normalFont));

            cardSide.add(qrContainer);
            cardSide.add(signatureDiv);
        }

        return cardSide;
    }
}
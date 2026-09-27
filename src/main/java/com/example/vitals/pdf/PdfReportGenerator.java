package com.example.vitals.pdf;

import com.example.vitals.entity.SessionStatus;
import com.example.vitals.entity.VitalsSession;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Component
public class PdfReportGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Color BRAND_COLOR = new Color(20, 60, 120);

    public byte[] generateReport(VitalsSession session) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document);
            addParticipantDetails(document, session);
            addVitalsTable(document, session);
            addAccuracyBadge(document, session);
            addDisclaimer(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }

        return out.toByteArray();
    }

    private void addHeader(Document document) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND_COLOR);
        Font subtitleFont = new Font(Font.HELVETICA, 11, Font.ITALIC, Color.GRAY);

        Paragraph title = new Paragraph("VitalsFromVideo", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Contactless Vital Sign Screening Report", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(10);
        document.add(subtitle);

        LineSeparator separator = new LineSeparator();
        separator.setLineColor(BRAND_COLOR);
        Chunk lineChunk = new Chunk(separator);
        Paragraph linePara = new Paragraph(lineChunk);
        linePara.setSpacingAfter(15);
        document.add(linePara);
    }

    private void addParticipantDetails(Document document, VitalsSession session) throws DocumentException {
        Font labelFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font valueFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);
        table.setWidths(new float[]{1f, 2f});

        addBorderlessRow(table, "Participant Name:", session.getParticipantName(), labelFont, valueFont);
        addBorderlessRow(table, "Session ID:", "#" + session.getId(), labelFont, valueFont);
        addBorderlessRow(table, "Scan Timestamp:", session.getTimestamp().format(DATE_FORMAT), labelFont, valueFont);
        addBorderlessRow(table, "Report Status:", session.getStatus().name(), labelFont, valueFont);

        document.add(table);
    }

    private void addBorderlessRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value == null ? "N/A" : value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6);
        table.addCell(valueCell);
    }

    private void addVitalsTable(Document document, VitalsSession session) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, BRAND_COLOR);
        Paragraph sectionTitle = new Paragraph("Vitals Summary", sectionFont);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);
        table.setWidths(new float[]{2f, 1f});

        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
        addHeaderCell(table, "Metric", headerFont);
        addHeaderCell(table, "Value", headerFont);

        Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

        addDataRow(table, "Estimated Heart Rate (rPPG)",
                formatOrNA(session.getEstimatedHeartRate(), "BPM"), bodyFont);
        addDataRow(table, "Estimated Respiration Rate",
                formatOrNA(session.getEstimatedRespirationRate(), "breaths/min"), bodyFont);
        addDataRow(table, "Ground Truth Heart Rate (Oximeter)",
                formatOrNA(session.getGroundTruthHeartRate(), "BPM"), bodyFont);
        addDataRow(table, "Absolute Error",
                formatOrNA(session.getAbsoluteError(), "BPM"), bodyFont);
        addDataRow(table, "Accuracy",
                session.getPercentageAccuracy() != null
                        ? session.getPercentageAccuracy() + " %"
                        : "Pending Verification",
                bodyFont);

        document.add(table);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BRAND_COLOR);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addDataRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setPadding(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setPadding(6);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(valueCell);
    }

    private String formatOrNA(Double value, String unit) {
        if (value == null) {
            return "N/A";
        }
        return value + " " + unit;
    }

    private void addAccuracyBadge(Document document, VitalsSession session) throws DocumentException {
        if (session.getStatus() != SessionStatus.VERIFIED || session.getPercentageAccuracy() == null) {
            Font pendingFont = new Font(Font.HELVETICA, 12, Font.ITALIC, Color.GRAY);
            Paragraph pending = new Paragraph(
                    "Benchmark verification pending - ground truth reading not yet submitted.", pendingFont);
            pending.setSpacingAfter(20);
            document.add(pending);
            return;
        }

        double accuracy = session.getPercentageAccuracy();
        Color badgeColor;
        String badgeLabel;

        if (accuracy >= 90) {
            badgeColor = new Color(34, 139, 34);
            badgeLabel = "EXCELLENT ACCURACY";
        } else if (accuracy >= 75) {
            badgeColor = new Color(200, 150, 20);
            badgeLabel = "ACCEPTABLE ACCURACY";
        } else {
            badgeColor = new Color(178, 34, 34);
            badgeLabel = "LOW ACCURACY";
        }

        PdfPTable badgeTable = new PdfPTable(1);
        badgeTable.setWidthPercentage(65);
        badgeTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeTable.setSpacingAfter(20);

        Font badgeFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.WHITE);
        PdfPCell badgeCell = new PdfPCell(new Phrase(badgeLabel + "  (" + accuracy + "%)", badgeFont));
        badgeCell.setBackgroundColor(badgeColor);
        badgeCell.setPadding(14);
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeTable.addCell(badgeCell);

        document.add(badgeTable);
    }

    private void addDisclaimer(Document document) throws DocumentException {
        Font disclaimerFont = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);
        Paragraph disclaimer = new Paragraph(
                "Disclaimer: This report is generated by a college technical expo prototype (VitalsFromVideo) "
                        + "for demonstration purposes only. Vital sign estimates are derived from remote "
                        + "photoplethysmography (rPPG) and are NOT medical-grade measurements. This report must "
                        + "not be used for diagnosis, treatment, or any clinical decision-making. Consult a "
                        + "certified medical professional for actual health assessments.",
                disclaimerFont
        );
        disclaimer.setSpacingBefore(30);
        document.add(disclaimer);
    }
}
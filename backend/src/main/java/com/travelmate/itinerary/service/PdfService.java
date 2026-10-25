package com.travelmate.itinerary.service;

import com.travelmate.itinerary.dto.BudgetSummaryDto;
import com.travelmate.itinerary.dto.DayDto;
import com.travelmate.itinerary.dto.ItemDto;
import com.travelmate.itinerary.dto.ItineraryDto;
import com.travelmate.itinerary.entity.ItemType;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Export PDF de l'itineraire (bibliotheque OpenPDF), aux couleurs de la charte TravelMate
@Service
public class PdfService {

    private static final Color TEAL = new Color(0x0E, 0x7C, 0x86);    // --tm-teal
    private static final Color NAVY = new Color(0x12, 0x26, 0x3A);    // --tm-navy
    private static final Color OCEAN_50 = new Color(0xEF, 0xF9, 0xF9);
    private static final Color ERROR = new Color(0xE5, 0x48, 0x4D);   // --tm-error
    private static final Color SUCCESS = new Color(0x22, 0xA0, 0x6B); // --tm-success
    private static final Locale FR = Locale.FRENCH;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", FR);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", FR);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final Font TITLE = new Font(Font.HELVETICA, 22, Font.BOLD, NAVY);
    private static final Font SUBTITLE = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
    private static final Font DAY = new Font(Font.HELVETICA, 14, Font.BOLD, TEAL);
    private static final Font HEADER = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font CELL = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font NOTE = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
    private static final Font BOLD = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
    private static final Font ALERT = new Font(Font.HELVETICA, 11, Font.BOLD, ERROR);
    private static final Font OK = new Font(Font.HELVETICA, 11, Font.BOLD, SUCCESS);

    private final ItineraryService itineraryService;
    private final BudgetService budgetService;

    public PdfService(ItineraryService itineraryService, BudgetService budgetService) {
        this.itineraryService = itineraryService;
        this.budgetService = budgetService;
    }

    public byte[] export(String email, Long itineraryId) {
        ItineraryDto it = itineraryService.getById(email, itineraryId);
        BudgetSummaryDto budget = budgetService.summary(email, itineraryId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        doc.add(new Paragraph("TravelMate - " + it.destination(), TITLE));
        doc.add(new Paragraph("Du " + it.startDate().format(SHORT_DATE) + " au " + it.endDate().format(SHORT_DATE)
                + "  |  " + it.days().size() + " jours" + (it.generatedByAi() ? "  |  généré par l'IA" : ""), SUBTITLE));
        doc.add(Chunk.NEWLINE);

        for (DayDto day : it.days()) {
            Paragraph dayTitle = new Paragraph("Jour " + day.dayNumber() + " - " + day.title(), DAY);
            dayTitle.setSpacingBefore(8);
            doc.add(dayTitle);
            doc.add(new Paragraph(capitalize(day.date().format(DATE)), SUBTITLE));
            doc.add(dayTable(day));
        }

        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Résumé du budget", DAY));
        doc.add(new Paragraph(euros("Budget : ", budget.budget()), BOLD));
        doc.add(new Paragraph(euros("Coût estimé : ", budget.estimatedTotal()), BOLD));
        if (budget.spentTotal() > 0) {
            doc.add(new Paragraph(euros("Déjà dépensé : ", budget.spentTotal()), BOLD));
        }
        doc.add(budget.overBudget()
                ? new Paragraph(euros("Dépassement du budget : ", -budget.remainingEstimated()), ALERT)
                : new Paragraph(euros("Dans le budget, il reste ", budget.remainingEstimated()), OK));

        doc.close();
        return out.toByteArray();
    }

    private PdfPTable dayTable(DayDto day) {
        PdfPTable table = new PdfPTable(new float[]{1.3f, 1.6f, 5f, 1.6f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        for (String h : new String[]{"Heure", "Type", "Programme", "Coût"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, HEADER));
            cell.setBackgroundColor(TEAL);
            cell.setPadding(5);
            table.addCell(cell);
        }
        boolean shaded = false;
        for (ItemDto item : day.items()) {
            Color bg = shaded ? OCEAN_50 : Color.WHITE;
            shaded = !shaded;
            table.addCell(cell(time(item.startTime()) + (item.endTime() != null ? " - " + time(item.endTime()) : ""), bg));
            table.addCell(cell(typeLabel(item.type()), bg));

            Paragraph what = new Paragraph(item.name(), CELL);
            if (item.notes() != null && !item.notes().isBlank()) {
                what.add(new Chunk("\n" + item.notes(), NOTE));
            }
            PdfPCell whatCell = new PdfPCell(what);
            whatCell.setBackgroundColor(bg);
            whatCell.setPadding(5);
            table.addCell(whatCell);

            PdfPCell cost = cell(String.format(FR, "%.2f EUR", item.estimatedCost()), bg);
            cost.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cost);
        }
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total de la journée", BOLD));
        totalLabel.setColspan(3);
        totalLabel.setPadding(5);
        table.addCell(totalLabel);
        PdfPCell total = new PdfPCell(new Phrase(String.format(FR, "%.2f EUR", day.dayCost()), BOLD));
        total.setHorizontalAlignment(Element.ALIGN_RIGHT);
        total.setPadding(5);
        table.addCell(total);
        return table;
    }

    private PdfPCell cell(String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        return cell;
    }

    private static String typeLabel(ItemType type) {
        return switch (type) {
            case ACTIVITE -> "Activité";
            case RESTAURANT -> "Restaurant";
            case HEBERGEMENT -> "Hébergement";
            case TRANSPORT -> "Transport";
            case AUTRE -> "Autre";
        };
    }

    private static String euros(String label, double amount) {
        return label + String.format(FR, "%.2f EUR", amount);
    }

    private static String time(LocalTime t) {
        return t == null ? "" : t.format(TIME);
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

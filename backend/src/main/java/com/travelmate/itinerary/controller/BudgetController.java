package com.travelmate.itinerary.controller;

import com.travelmate.itinerary.dto.BudgetSummaryDto;
import com.travelmate.itinerary.dto.ExpenseDto;
import com.travelmate.itinerary.dto.ExpenseRequest;
import com.travelmate.itinerary.service.BudgetService;
import com.travelmate.itinerary.service.PdfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

// Suivi du budget (depenses reelles) et export PDF d'un itineraire de l'utilisateur connecte
@RestController
@RequestMapping("/api/itinerary")
public class BudgetController {

    private final BudgetService budgetService;
    private final PdfService pdfService;

    public BudgetController(BudgetService budgetService, PdfService pdfService) {
        this.budgetService = budgetService;
        this.pdfService = pdfService;
    }

    // GET /api/itinerary/5/budget : prevu et depense, par categorie
    @GetMapping("/{id}/budget")
    public BudgetSummaryDto summary(Principal principal, @PathVariable Long id) {
        return budgetService.summary(principal.getName(), id);
    }

    // GET /api/itinerary/5/expenses
    @GetMapping("/{id}/expenses")
    public List<ExpenseDto> expenses(Principal principal, @PathVariable Long id) {
        return budgetService.getExpenses(principal.getName(), id);
    }

    // POST /api/itinerary/5/expenses
    @PostMapping("/{id}/expenses")
    public ExpenseDto addExpense(Principal principal, @PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return budgetService.addExpense(principal.getName(), id, request);
    }

    // DELETE /api/itinerary/expenses/8
    @DeleteMapping("/expenses/{expenseId}")
    public void deleteExpense(Principal principal, @PathVariable Long expenseId) {
        budgetService.deleteExpense(principal.getName(), expenseId);
    }

    // GET /api/itinerary/5/pdf
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=itineraire-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfService.export(principal.getName(), id));
    }
}

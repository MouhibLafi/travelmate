package com.travelmate.itinerary.service;

import com.travelmate.exception.ApiException;
import com.travelmate.itinerary.dto.BudgetSummaryDto;
import com.travelmate.itinerary.dto.BudgetSummaryDto.CategoryBudget;
import com.travelmate.itinerary.dto.ExpenseDto;
import com.travelmate.itinerary.dto.ExpenseRequest;
import com.travelmate.itinerary.entity.Expense;
import com.travelmate.itinerary.entity.ItemType;
import com.travelmate.itinerary.entity.Itinerary;
import com.travelmate.itinerary.entity.ItineraryItem;
import com.travelmate.itinerary.repository.ExpenseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// Suivi du budget : cout prevu (elements de l'itineraire) compare aux depenses reelles, par categorie
@Service
public class BudgetService {

    private final ItineraryService itineraryService;
    private final ExpenseRepository expenseRepository;

    public BudgetService(ItineraryService itineraryService, ExpenseRepository expenseRepository) {
        this.itineraryService = itineraryService;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public BudgetSummaryDto summary(String email, Long itineraryId) {
        Itinerary it = itineraryService.findMyItinerary(email, itineraryId);
        List<ItineraryItem> items = it.allItems();
        List<Expense> expenses = it.getExpenses();

        // Une ligne par categorie qui a un cout prevu ou une depense
        List<CategoryBudget> categories = Arrays.stream(ItemType.values())
                .map(type -> new CategoryBudget(type,
                        items.stream().filter(i -> i.getType() == type).mapToDouble(ItineraryItem::getEstimatedCost).sum(),
                        expenses.stream().filter(e -> e.getCategory() == type).mapToDouble(Expense::getAmount).sum()))
                .filter(c -> c.estimated() > 0 || c.spent() > 0)
                .toList();

        double estimated = it.getTotalEstimatedCost();
        double spent = expenses.stream().mapToDouble(Expense::getAmount).sum();
        return new BudgetSummaryDto(it.getBudget(), estimated, spent,
                it.getBudget() - estimated, it.getBudget() - spent, estimated > it.getBudget(), categories);
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getExpenses(String email, Long itineraryId) {
        itineraryService.findMyItinerary(email, itineraryId);   // verifie que l'itineraire est a l'utilisateur
        return expenseRepository.findByItineraryIdOrderByDateAsc(itineraryId).stream().map(ExpenseDto::from).toList();
    }

    @Transactional
    public ExpenseDto addExpense(String email, Long itineraryId, ExpenseRequest request) {
        Expense e = new Expense();
        e.setItinerary(itineraryService.findMyItinerary(email, itineraryId));
        e.setCategory(request.category());
        e.setAmount(request.amount());
        e.setDate(request.date() != null ? request.date() : LocalDate.now());
        e.setDescription(request.description());
        return ExpenseDto.from(expenseRepository.save(e));
    }

    @Transactional
    public void deleteExpense(String email, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dépense introuvable"));
        Itinerary it = itineraryService.findMyItinerary(email, expense.getItinerary().getId());
        it.getExpenses().remove(expense);
    }
}

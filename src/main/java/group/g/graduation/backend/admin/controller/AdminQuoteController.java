package group.g.graduation.backend.admin.controller;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.service.AdminQuoteService;
import group.g.graduation.backend.common.enums.QuoteCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Quote Controller - واجهة API لإدارة الاقتباسات اليومية
 */
@RestController
@RequestMapping("/api/admin/quotes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Daily Quotes", description = "Daily quotes management APIs for administrators - إدارة الاقتباسات اليومية")
public class AdminQuoteController {
    
    private final AdminQuoteService quoteService;
    
    // ===== CRUD Operations =====
    
    @PostMapping
    @Operation(summary = "Create a new quote", description = "إنشاء اقتباس يومي جديد")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Quote created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    public ResponseEntity<QuoteResponse> createQuote(
            @Valid @RequestBody QuoteCreateRequest request) {
        QuoteResponse response = quoteService.createQuote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update a quote", description = "تعديل اقتباس موجود")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quote updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Quote not found")
    })
    public ResponseEntity<QuoteResponse> updateQuote(
            @Parameter(description = "Quote ID") @PathVariable Long id,
            @Valid @RequestBody QuoteUpdateRequest request) {
        QuoteResponse response = quoteService.updateQuote(id, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a quote", description = "حذف اقتباس")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Quote deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Quote not found")
    })
    public ResponseEntity<Void> deleteQuote(
            @Parameter(description = "Quote ID") @PathVariable Long id) {
        quoteService.deleteQuote(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get quote by ID", description = "جلب اقتباس بالمعرف")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Quote found"),
        @ApiResponse(responseCode = "404", description = "Quote not found")
    })
    public ResponseEntity<QuoteResponse> getQuoteById(
            @Parameter(description = "Quote ID") @PathVariable Long id) {
        QuoteResponse response = quoteService.getQuoteById(id);
        return ResponseEntity.ok(response);
    }
    
    // ===== List & Filter Operations =====
    
    @GetMapping
    @Operation(summary = "Get all quotes", description = "جلب كل الاقتباسات مع الترقيم")
    @ApiResponse(responseCode = "200", description = "Quotes retrieved successfully")
    public ResponseEntity<QuoteListResponse> getAllQuotes(
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") 
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") 
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        QuoteListResponse response = quoteService.getAllQuotes(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "Get quotes by category", description = "جلب الاقتباسات حسب التصنيف")
    @ApiResponse(responseCode = "200", description = "Quotes retrieved by category")
    public ResponseEntity<QuoteListResponse> getQuotesByCategory(
            @Parameter(description = "Quote category (PROVERB, MOTIVATION, WISDOM, TIP)") 
            @PathVariable QuoteCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        QuoteListResponse response = quoteService.getQuotesByCategory(category, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active quotes only", description = "جلب الاقتباسات النشطة فقط")
    @ApiResponse(responseCode = "200", description = "Active quotes retrieved")
    public ResponseEntity<QuoteListResponse> getActiveQuotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        QuoteListResponse response = quoteService.getActiveQuotes(pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search quotes", description = "البحث في الاقتباسات")
    @ApiResponse(responseCode = "200", description = "Search results")
    public ResponseEntity<QuoteListResponse> searchQuotes(
            @Parameter(description = "Search keyword") 
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        QuoteListResponse response = quoteService.searchQuotes(q, pageable);
        return ResponseEntity.ok(response);
    }
    
    // ===== Status Operations =====
    
    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle quote status", description = "تفعيل/تعطيل اقتباس")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status toggled successfully"),
        @ApiResponse(responseCode = "404", description = "Quote not found")
    })
    public ResponseEntity<QuoteResponse> toggleQuoteStatus(
            @Parameter(description = "Quote ID") @PathVariable Long id) {
        QuoteResponse response = quoteService.toggleQuoteStatus(id);
        return ResponseEntity.ok(response);
    }
}

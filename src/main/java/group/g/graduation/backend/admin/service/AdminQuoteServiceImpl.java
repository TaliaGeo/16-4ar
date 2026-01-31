package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.mapper.QuoteMapper;
import group.g.graduation.backend.common.enums.QuoteCategory;
import group.g.graduation.backend.common.exception.ResourceNotFoundException;
import group.g.graduation.backend.common.model.DailyQuote;
import group.g.graduation.backend.common.repository.DailyQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Quote Service Implementation - تنفيذ خدمة إدارة الاقتباسات
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminQuoteServiceImpl implements AdminQuoteService {
    
    private final DailyQuoteRepository quoteRepository;
    private final QuoteMapper quoteMapper;
    
    // ===== CRUD Operations =====
    
    @Override
    public QuoteResponse createQuote(QuoteCreateRequest request) {
        log.info("Creating new quote: {}", request.getQuoteTextAr().substring(0, Math.min(50, request.getQuoteTextAr().length())));
        
        DailyQuote quote = quoteMapper.toEntity(request);
        DailyQuote savedQuote = quoteRepository.save(quote);
        
        log.info("Quote created successfully with ID: {}", savedQuote.getId());
        return quoteMapper.toResponse(savedQuote);
    }
    
    @Override
    public QuoteResponse updateQuote(Long id, QuoteUpdateRequest request) {
        log.info("Updating quote with ID: {}", id);
        
        DailyQuote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote", "id", id));
        
        quoteMapper.updateEntity(quote, request);
        DailyQuote updatedQuote = quoteRepository.save(quote);
        
        log.info("Quote updated successfully: {}", id);
        return quoteMapper.toResponse(updatedQuote);
    }
    
    @Override
    public void deleteQuote(Long id) {
        log.info("Deleting quote with ID: {}", id);
        
        if (!quoteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Quote", "id", id);
        }
        
        quoteRepository.deleteById(id);
        log.info("Quote deleted successfully: {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public QuoteResponse getQuoteById(Long id) {
        log.debug("Fetching quote with ID: {}", id);
        
        DailyQuote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote", "id", id));
        
        return quoteMapper.toResponse(quote);
    }
    
    // ===== List & Filter Operations =====
    
    @Override
    @Transactional(readOnly = true)
    public QuoteListResponse getAllQuotes(Pageable pageable) {
        log.debug("Fetching all quotes, page: {}", pageable.getPageNumber());
        
        Page<DailyQuote> quotePage = quoteRepository.findAll(pageable);
        return buildQuoteListResponse(quotePage);
    }
    
    @Override
    @Transactional(readOnly = true)
    public QuoteListResponse getQuotesByCategory(QuoteCategory category, Pageable pageable) {
        log.debug("Fetching quotes by category: {}", category);
        
        // Get all quotes and filter by category (we'll add a proper query later)
        List<DailyQuote> allQuotes = quoteRepository.findAll();
        List<DailyQuote> filteredQuotes = allQuotes.stream()
                .filter(q -> q.getCategory() == category)
                .collect(Collectors.toList());
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredQuotes.size());
        
        List<QuoteResponse> quotes = filteredQuotes.subList(start, end).stream()
                .map(quoteMapper::toResponse)
                .collect(Collectors.toList());
        
        return QuoteListResponse.builder()
                .quotes(quotes)
                .currentPage(pageable.getPageNumber())
                .totalPages((int) Math.ceil((double) filteredQuotes.size() / pageable.getPageSize()))
                .totalElements(filteredQuotes.size())
                .pageSize(pageable.getPageSize())
                .first(pageable.getPageNumber() == 0)
                .last(end >= filteredQuotes.size())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public QuoteListResponse getActiveQuotes(Pageable pageable) {
        log.debug("Fetching active quotes");
        
        List<DailyQuote> activeQuotes = quoteRepository.findByIsActiveTrue();
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), activeQuotes.size());
        
        List<QuoteResponse> quotes = activeQuotes.subList(start, end).stream()
                .map(quoteMapper::toResponse)
                .collect(Collectors.toList());
        
        return QuoteListResponse.builder()
                .quotes(quotes)
                .currentPage(pageable.getPageNumber())
                .totalPages((int) Math.ceil((double) activeQuotes.size() / pageable.getPageSize()))
                .totalElements(activeQuotes.size())
                .pageSize(pageable.getPageSize())
                .first(pageable.getPageNumber() == 0)
                .last(end >= activeQuotes.size())
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public QuoteListResponse searchQuotes(String keyword, Pageable pageable) {
        log.debug("Searching quotes with keyword: {}", keyword);
        
        String searchLower = keyword.toLowerCase();
        List<DailyQuote> allQuotes = quoteRepository.findAll();
        
        List<DailyQuote> filteredQuotes = allQuotes.stream()
                .filter(q -> 
                    (q.getQuoteTextAr() != null && q.getQuoteTextAr().contains(keyword)) ||
                    (q.getQuoteTextEn() != null && q.getQuoteTextEn().toLowerCase().contains(searchLower)) ||
                    (q.getAuthor() != null && q.getAuthor().toLowerCase().contains(searchLower))
                )
                .collect(Collectors.toList());
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredQuotes.size());
        
        List<QuoteResponse> quotes = filteredQuotes.subList(start, end).stream()
                .map(quoteMapper::toResponse)
                .collect(Collectors.toList());
        
        return QuoteListResponse.builder()
                .quotes(quotes)
                .currentPage(pageable.getPageNumber())
                .totalPages((int) Math.ceil((double) filteredQuotes.size() / pageable.getPageSize()))
                .totalElements(filteredQuotes.size())
                .pageSize(pageable.getPageSize())
                .first(pageable.getPageNumber() == 0)
                .last(end >= filteredQuotes.size())
                .build();
    }
    
    // ===== Status Operations =====
    
    @Override
    public QuoteResponse toggleQuoteStatus(Long id) {
        log.info("Toggling status for quote: {}", id);
        
        DailyQuote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote", "id", id));
        
        quote.setIsActive(!quote.getIsActive());
        DailyQuote updatedQuote = quoteRepository.save(quote);
        
        log.info("Quote {} status toggled to: {}", id, updatedQuote.getIsActive());
        return quoteMapper.toResponse(updatedQuote);
    }
    
    // ===== Helper Methods =====
    
    private QuoteListResponse buildQuoteListResponse(Page<DailyQuote> quotePage) {
        List<QuoteResponse> quotes = quotePage.getContent().stream()
                .map(quoteMapper::toResponse)
                .collect(Collectors.toList());
        
        return QuoteListResponse.builder()
                .quotes(quotes)
                .currentPage(quotePage.getNumber())
                .totalPages(quotePage.getTotalPages())
                .totalElements(quotePage.getTotalElements())
                .pageSize(quotePage.getSize())
                .first(quotePage.isFirst())
                .last(quotePage.isLast())
                .build();
    }
}

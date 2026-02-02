package group.g.graduation.backend.admin.service;

import group.g.graduation.backend.admin.dto.*;
import group.g.graduation.backend.admin.mapper.MonthPlantMapper;
import group.g.graduation.backend.common.model.Month;
import group.g.graduation.backend.common.model.MonthPlant;
import group.g.graduation.backend.common.model.Plant;
import group.g.graduation.backend.common.repository.MonthPlantRepository;
import group.g.graduation.backend.common.repository.MonthRepository;
import group.g.graduation.backend.common.repository.PlantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Service for MonthPlant Management
 * خدمة إدارة علاقة الأشهر بالنباتات للأدمن
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminMonthPlantService {
    
    private final MonthPlantRepository monthPlantRepository;
    private final MonthRepository monthRepository;
    private final PlantRepository plantRepository;
    private final MonthPlantMapper mapper;
    
    // ===================== CRUD Operations =====================
    
    /**
     * Add a plant to a month
     */
    public MonthPlantResponse addPlantToMonth(Long monthId, MonthPlantRequest request) {
        log.info("Adding plant {} to month {}", request.getPlantId(), monthId);
        
        Month month = monthRepository.findById(monthId)
                .orElseThrow(() -> new EntityNotFoundException("Month not found with ID: " + monthId));
        
        Plant plant = plantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new EntityNotFoundException("Plant not found with ID: " + request.getPlantId()));
        
        // Check if already exists
        if (monthPlantRepository.existsByMonthIdAndPlantId(monthId, request.getPlantId())) {
            throw new IllegalArgumentException("Plant is already added to this month");
        }
        
        MonthPlant monthPlant = MonthPlant.builder()
                .month(month)
                .plant(plant)
                .plantingNoteAr(request.getPlantingNoteAr())
                .plantingNoteEn(request.getPlantingNoteEn())
                .build();
        
        MonthPlant saved = monthPlantRepository.save(monthPlant);
        log.info("Added plant {} to month {} with ID: {}", plant.getNameAr(), month.getNameAr(), saved.getId());
        
        return mapper.toResponse(saved);
    }
    
    /**
     * Add multiple plants to a month
     */
    public List<MonthPlantResponse> addPlantsToMonth(Long monthId, BulkPlantsToMonthRequest request) {
        log.info("Adding {} plants to month {}", request.getPlants().size(), monthId);
        
        Month month = monthRepository.findById(monthId)
                .orElseThrow(() -> new EntityNotFoundException("Month not found with ID: " + monthId));
        
        List<MonthPlantResponse> responses = new ArrayList<>();
        
        for (BulkPlantsToMonthRequest.PlantEntry entry : request.getPlants()) {
            try {
                if (monthPlantRepository.existsByMonthIdAndPlantId(monthId, entry.getPlantId())) {
                    continue; // Skip if already exists
                }
                
                Plant plant = plantRepository.findById(entry.getPlantId()).orElse(null);
                if (plant == null) continue;
                
                MonthPlant monthPlant = MonthPlant.builder()
                        .month(month)
                        .plant(plant)
                        .plantingNoteAr(entry.getPlantingNoteAr())
                        .plantingNoteEn(entry.getPlantingNoteEn())
                        .build();
                
                MonthPlant saved = monthPlantRepository.save(monthPlant);
                responses.add(mapper.toResponse(saved));
            } catch (Exception e) {
                log.warn("Failed to add plant {} to month {}: {}", entry.getPlantId(), monthId, e.getMessage());
            }
        }
        
        log.info("Added {} plants to month {}", responses.size(), monthId);
        return responses;
    }
    
    /**
     * Add a plant to multiple months
     */
    public List<MonthPlantResponse> addPlantToMonths(Long plantId, BulkMonthsToPlantRequest request) {
        log.info("Adding plant {} to {} months", plantId, request.getMonths().size());
        
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new EntityNotFoundException("Plant not found with ID: " + plantId));
        
        List<MonthPlantResponse> responses = new ArrayList<>();
        
        for (BulkMonthsToPlantRequest.MonthEntry entry : request.getMonths()) {
            try {
                if (monthPlantRepository.existsByMonthIdAndPlantId(entry.getMonthId(), plantId)) {
                    continue; // Skip if already exists
                }
                
                Month month = monthRepository.findById(entry.getMonthId()).orElse(null);
                if (month == null) continue;
                
                MonthPlant monthPlant = MonthPlant.builder()
                        .month(month)
                        .plant(plant)
                        .plantingNoteAr(entry.getPlantingNoteAr())
                        .plantingNoteEn(entry.getPlantingNoteEn())
                        .build();
                
                MonthPlant saved = monthPlantRepository.save(monthPlant);
                responses.add(mapper.toResponse(saved));
            } catch (Exception e) {
                log.warn("Failed to add plant {} to month {}: {}", plantId, entry.getMonthId(), e.getMessage());
            }
        }
        
        log.info("Added plant {} to {} months", plantId, responses.size());
        return responses;
    }
    
    /**
     * Get all month-plant relationships
     */
    @Transactional(readOnly = true)
    public List<MonthPlantResponse> getAllMonthPlants() {
        return mapper.toResponseList(monthPlantRepository.findAll());
    }
    
    /**
     * Get month-plant by ID
     */
    @Transactional(readOnly = true)
    public MonthPlantResponse getMonthPlantById(Long id) {
        try {
            log.info("Fetching month-plant with ID: {}", id);
            
            // Check if month-plant exists first
            if (!monthPlantRepository.existsById(id)) {
                log.warn("Month-plant with ID {} does not exist", id);
                throw new EntityNotFoundException("MonthPlant not found with ID: " + id);
            }
            
            // Use JOIN FETCH query to avoid LazyInitializationException
            MonthPlant monthPlant = monthPlantRepository.findByIdWithDetails(id)
                    .orElse(null);
            
            if (monthPlant != null) {
                log.info("Found month-plant with JOIN FETCH - ID: {}, Month: {}, Plant: {}", 
                    monthPlant.getId(),
                    monthPlant.getMonth() != null ? monthPlant.getMonth().getNameEn() : "NULL",
                    monthPlant.getPlant() != null ? monthPlant.getPlant().getNameEn() : "NULL");
                
                MonthPlantResponse response = mapper.toResponse(monthPlant);
                log.info("Successfully mapped month-plant to response");
                return response;
            }
            
            // Fallback to normal find if JOIN FETCH fails
            log.warn("JOIN FETCH failed for month-plant ID: {}, trying regular findById", id);
            monthPlant = monthPlantRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("MonthPlant not found with ID: " + id));
            
            log.info("Found month-plant with regular query - ID: {}", monthPlant.getId());
            MonthPlantResponse response = mapper.toResponse(monthPlant);
            log.info("Successfully mapped month-plant to response using fallback method");
            return response;
            
        } catch (EntityNotFoundException e) {
            log.error("MonthPlant not found with ID: {} - {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching month-plant with ID: {} - Error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to get month-plant with ID " + id + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Get plants for a specific month
     */
    @Transactional(readOnly = true)
    public List<MonthPlantResponse> getPlantsByMonth(Long monthId) {
        if (!monthRepository.existsById(monthId)) {
            throw new EntityNotFoundException("Month not found with ID: " + monthId);
        }
        return mapper.toResponseList(monthPlantRepository.findByMonthIdWithPlantDetails(monthId));
    }
    
    /**
     * Get months for a specific plant
     */
    @Transactional(readOnly = true)
    public List<MonthPlantResponse> getMonthsByPlant(Long plantId) {
        if (!plantRepository.existsById(plantId)) {
            throw new EntityNotFoundException("Plant not found with ID: " + plantId);
        }
        return mapper.toResponseList(monthPlantRepository.findByPlantId(plantId));
    }
    
    /**
     * Get plants for current month
     */
    @Transactional(readOnly = true)
    public List<MonthPlantResponse> getPlantsForCurrentMonth() {
        int currentMonthNumber = LocalDate.now().getMonthValue();
        
        Optional<Month> currentMonth = monthRepository.findByMonthNumber(currentMonthNumber);
        if (currentMonth.isEmpty()) {
            return Collections.emptyList();
        }
        
        return mapper.toResponseList(monthPlantRepository.findByMonthIdWithPlantDetails(currentMonth.get().getId()));
    }
    
    /**
     * Update month-plant relationship
     */
    public MonthPlantResponse updateMonthPlant(Long id, MonthPlantUpdateRequest request) {
        log.info("Updating month-plant with ID: {}", id);
        
        try {
            MonthPlant monthPlant = monthPlantRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("MonthPlant not found with ID: " + id));
            
            if (request.getPlantingNoteAr() != null) {
                monthPlant.setPlantingNoteAr(request.getPlantingNoteAr());
            }
            if (request.getPlantingNoteEn() != null) {
                monthPlant.setPlantingNoteEn(request.getPlantingNoteEn());
            }
            
            MonthPlant saved = monthPlantRepository.save(monthPlant);
            log.info("Updated month-plant with ID: {}", saved.getId());
            
            return mapper.toResponse(saved);
        } catch (EntityNotFoundException e) {
            log.error("MonthPlant not found with ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Error updating month-plant with ID: {}", id, e);
            throw new RuntimeException("Failed to update month-plant: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete month-plant relationship
     */
    public void deleteMonthPlant(Long id) {
        try {
            log.info("Deleting month-plant with ID: {}", id);
            if (!monthPlantRepository.existsById(id)) {
                throw new EntityNotFoundException("MonthPlant not found with ID: " + id);
            }
            monthPlantRepository.deleteById(id);
            log.info("Deleted month-plant with ID: {}", id);
        } catch (EntityNotFoundException e) {
            log.error("MonthPlant not found for deletion with ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting month-plant with ID: {}", id, e);
            throw new RuntimeException("Failed to delete month-plant: " + e.getMessage(), e);
        }
    }
    
    /**
     * Remove plant from month
     */
    public void removePlantFromMonth(Long monthId, Long plantId) {
        try {
            log.info("Removing plant {} from month {}", plantId, monthId);
            MonthPlant monthPlant = monthPlantRepository.findByMonthIdAndPlantId(monthId, plantId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "MonthPlant not found for monthId=" + monthId + " and plantId=" + plantId));
            
            monthPlantRepository.delete(monthPlant);
            log.info("Removed plant {} from month {}", plantId, monthId);
        } catch (EntityNotFoundException e) {
            log.error("MonthPlant not found for monthId={} and plantId={}", monthId, plantId);
            throw e;
        } catch (Exception e) {
            log.error("Error removing plant {} from month {}", plantId, monthId, e);
            throw new RuntimeException("Failed to remove plant from month: " + e.getMessage(), e);
        }
    }
    
    /**
     * Remove all plants from a month
     */
    public void removeAllPlantsFromMonth(Long monthId) {
        List<MonthPlant> monthPlants = monthPlantRepository.findByMonthId(monthId);
        monthPlantRepository.deleteAll(monthPlants);
        log.info("Removed {} plants from month {}", monthPlants.size(), monthId);
    }
    
    /**
     * Remove plant from all months
     */
    public void removePlantFromAllMonths(Long plantId) {
        List<MonthPlant> monthPlants = monthPlantRepository.findByPlantId(plantId);
        monthPlantRepository.deleteAll(monthPlants);
        log.info("Removed plant {} from {} months", plantId, monthPlants.size());
    }
    
    // ===================== Statistics =====================
    
    /**
     * Get statistics for month-plant relationships
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthPlantStats() {
        List<MonthPlant> all = monthPlantRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRelationships", all.size());
        
        // Count plants per month
        Map<Long, Long> plantsPerMonth = all.stream()
                .collect(Collectors.groupingBy(
                        mp -> mp.getMonth().getId(),
                        Collectors.counting()));
        stats.put("plantsPerMonth", plantsPerMonth);
        
        // Count months per plant
        Map<Long, Long> monthsPerPlant = all.stream()
                .collect(Collectors.groupingBy(
                        mp -> mp.getPlant().getId(),
                        Collectors.counting()));
        stats.put("monthsPerPlant", monthsPerPlant);
        
        // Current month info
        int currentMonthNumber = LocalDate.now().getMonthValue();
        Optional<Month> currentMonth = monthRepository.findByMonthNumber(currentMonthNumber);
        if (currentMonth.isPresent()) {
            long currentMonthPlants = all.stream()
                    .filter(mp -> mp.getMonth().getId().equals(currentMonth.get().getId()))
                    .count();
            stats.put("currentMonth", currentMonth.get().getNameAr());
            stats.put("currentMonthPlantCount", currentMonthPlants);
        }
        
        return stats;
    }
    
    /**
     * Get planting calendar (all months with their plants)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPlantingCalendar() {
        List<Month> allMonths = monthRepository.findAllByOrderByMonthNumberAsc();
        List<Map<String, Object>> calendar = new ArrayList<>();
        
        for (Month month : allMonths) {
            List<MonthPlant> monthPlants = monthPlantRepository.findByMonthIdWithPlantDetails(month.getId());
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("monthId", month.getId());
            monthData.put("monthNumber", month.getMonthNumber());
            monthData.put("monthNameAr", month.getNameAr());
            monthData.put("monthNameEn", month.getNameEn());
            monthData.put("plantCount", monthPlants.size());
            monthData.put("plants", mapper.toResponseList(monthPlants));
            
            calendar.add(monthData);
        }
        
        return calendar;
    }
}

# دليل عمل النظام - كيف ستعمل كل صفحة بالتفصيل

## 🏠 **الصفحة الرئيسية (Home Page)**

### 1. **معلومات الطقس اليومية** 🌤️

#### كيف تعمل:
```java
@GetMapping("/api/user/home/weather")
public WeatherResponse getTodayWeather() {
    // 1. جلب طقس اليوم من قاعدة البيانات
    DailyWeather todayWeather = dailyWeatherService.getTodayWeather();
    
    // 2. إذا لم يوجد، جلب من API خارجي (OpenWeatherMap)
    if (todayWeather == null) {
        todayWeather = weatherApiService.fetchTodayWeather("Gaza, Palestine");
        dailyWeatherService.saveTodayWeather(todayWeather);
    }
    
    return WeatherResponse.builder()
        .temperature(todayWeather.getTemperatureCelsius())
        .condition(todayWeather.getWeatherCondition())
        .description(todayWeather.getWeatherDescriptionAr())
        .iconUrl(todayWeather.getWeatherIconUrl())
        .humidity(todayWeather.getHumidityPercentage())
        .build();
}
```

#### البيانات المطلوبة:
- **درجة الحرارة**: 22°C
- **حالة الطقس**: مشمس، ممطر، غائم
- **صورة معبرة**: أيقونة الطقس
- **وصف**: "يوم مشمس ومعتدل مناسب للعناية بالنباتات"

### 2. **الاقتباس أو التحفيز اليومي** ✨

```java
@GetMapping("/api/user/home/daily-quote")
public DailyQuoteResponse getTodayQuote(@AuthenticationPrincipal UserPrincipal user) {
    // 1. التحقق من وجود اقتباس لهذا المستخدم اليوم
    UserDailyQuote todayQuote = userDailyQuoteService.getTodayQuote(user.getId());
    
    if (todayQuote == null) {
        // 2. اختيار اقتباس عشوائي من المجموعة النشطة
        DailyQuote randomQuote = dailyQuoteService.getRandomActiveQuote();
        
        // 3. ربطه بالمستخدم لهذا اليوم
        todayQuote = userDailyQuoteService.assignQuoteToUser(user.getId(), randomQuote);
    }
    
    return DailyQuoteResponse.builder()
        .id(todayQuote.getQuote().getId())
        .quoteAr(todayQuote.getQuote().getQuoteAr())
        .authorAr(todayQuote.getQuote().getAuthorAr())
        .category(todayQuote.getQuote().getCategory())
        .isLiked(todayQuote.getIsLiked())
        .build();
}
```

#### أمثلة على الاقتباسات:
- **تحفيزية**: "كل نبتة تزرعها اليوم هي أكسجين للغد"
- **نصائح**: "أفضل وقت لسقي النباتات هو الصباح الباكر"
- **حكم**: "الصبر على النبتة كالصبر على الحلم"

### 3. **التقويم الزراعي** 📅

```java
@GetMapping("/api/user/home/planting-calendar")
public PlantingCalendarResponse getPlantingCalendar() {
    // 1. جلب الشهر الحالي
    int currentMonth = LocalDate.now().getMonthValue();
    
    // 2. جلب معلومات الشهر مع النباتات المناسبة
    Month monthInfo = monthService.getMonthWithPlants(currentMonth);
    
    // 3. جلب النباتات الموصى بزراعتها هذا الشهر
    List<MonthPlantResponse> recommendedPlants = monthPlantService.getPlantsForMonth(currentMonth);
    
    return PlantingCalendarResponse.builder()
        .currentMonth(monthInfo.getNameAr())
        .season(monthInfo.getSeason().getDisplayNameAr())
        .weatherDescription(monthInfo.getWeatherDescriptionAr())
        .plantingNote(monthInfo.getPlantingNoteAr())
        .recommendedPlants(recommendedPlants)
        .build();
}
```

---

## 🌱 **صفحة محاصيلي (My Crops)**

### القسم الأول: **النباتات المخطط لزراعتها**

```java
@GetMapping("/api/user/plants/planned")
public List<UserPlantResponse> getPlannedPlants(@AuthenticationPrincipal UserPrincipal user) {
    List<UserPlant> plannedPlants = userPlantService.getPlantsByStatus(user.getId(), PlantStatus.PLANNED);
    
    return plannedPlants.stream()
        .map(plant -> UserPlantResponse.builder()
            .id(plant.getId())
            .plantName(plant.getPlant().getNameAr())
            .plantImage(plant.getPlant().getMainImageUrl())
            .plannedDate(plant.getPlannedDate())
            .daysUntilPlanting(ChronoUnit.DAYS.between(LocalDate.now(), plant.getPlannedDate()))
            .build())
        .collect(Collectors.toList());
}

// عند الضغط على "زرعت"
@PostMapping("/api/user/plants/{id}/plant")
public UserPlantResponse markAsPlanted(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
    UserPlant userPlant = userPlantService.markAsPlanted(id, user.getId());
    
    // 1. تحديث حالة النبتة
    userPlant.setStatus(PlantStatus.PLANTED);
    userPlant.setPlantedDate(LocalDate.now());
    
    // 2. إنشاء المهام التلقائية بناءً على PlantTasks
    taskSchedulingService.createTasksForPlant(userPlant);
    
    // 3. إرسال إشعار تهنئة
    notificationService.sendPlantingCongratulation(user.getId(), userPlant);
    
    return mapToResponse(userPlant);
}
```

### القسم الثاني: **المزروع الآن**

```java
@GetMapping("/api/user/plants/planted")
public List<CurrentPlantResponse> getCurrentPlants(@AuthenticationPrincipal UserPrincipal user) {
    List<UserPlant> plantedPlants = userPlantService.getPlantsByStatus(user.getId(), PlantStatus.PLANTED);
    
    return plantedPlants.stream()
        .map(plant -> {
            // حساب إحصائيات كل نبتة
            int daysPlanted = (int) ChronoUnit.DAYS.between(plant.getPlantedDate(), LocalDate.now());
            LocalDate lastWatering = wateringHistoryService.getLastWateringDate(plant.getId());
            LocalDate nextWatering = calculateNextWateringDate(plant, lastWatering);
            
            return CurrentPlantResponse.builder()
                .id(plant.getId())
                .plantName(plant.getPlant().getNameAr())
                .nickname(plant.getNickname())
                .plantedDate(plant.getPlantedDate())
                .daysPlanted(daysPlanted)
                .lastWateringDate(lastWatering)
                .nextWateringDate(nextWatering)
                .pendingTasksCount(userPlantTaskService.getPendingTasksCount(plant.getId()))
                .overdueTasksCount(userPlantTaskService.getOverdueTasksCount(plant.getId()))
                .build();
        })
        .collect(Collectors.toList());
}
```

---

## 📊 **صفحة تتبع النبتة (Plant Tracking)**

### قسم النظرة العامة:

```java
@GetMapping("/api/user/plants/{plantId}/overview")
public PlantOverviewResponse getPlantOverview(@PathVariable Long plantId, @AuthenticationPrincipal UserPrincipal user) {
    UserPlant userPlant = userPlantService.getUserPlant(plantId, user.getId());
    
    // حساب الإحصائيات
    int daysPlanted = (int) ChronoUnit.DAYS.between(userPlant.getPlantedDate(), LocalDate.now());
    LocalDate lastWatering = wateringHistoryService.getLastWateringDate(plantId);
    LocalDate nextWatering = calculateNextWateringDate(userPlant, lastWatering);
    int daysUntilHarvest = calculateDaysUntilHarvest(userPlant);
    
    return PlantOverviewResponse.builder()
        .plantId(userPlant.getId())
        .plantNameAr(userPlant.getPlant().getNameAr())
        .scientificName(userPlant.getPlant().getNameScientific())
        .nickname(userPlant.getNickname())
        .plantedDate(userPlant.getPlantedDate())
        .daysPlanted(daysPlanted)
        .lastWateringDate(lastWatering)
        .nextWateringDate(nextWatering)
        .daysUntilHarvest(daysUntilHarvest)
        .expectedHarvestDate(userPlant.getPlantedDate().plusDays(userPlant.getPlant().getDaysToHarvest()))
        .growthStage(calculateGrowthStage(daysPlanted, userPlant.getPlant().getDaysToHarvest()))
        .overallHealth(calculatePlantHealth(userPlant))
        .build();
}

// حساب مرحلة النمو
private String calculateGrowthStage(int daysPlanted, int totalDaysToHarvest) {
    double progress = (double) daysPlanted / totalDaysToHarvest;
    
    if (progress < 0.25) return "إنبات وبداية النمو";
    else if (progress < 0.5) return "نمو الأوراق";
    else if (progress < 0.75) return "تكوين الثمار/الأزهار";
    else if (progress < 1.0) return "نضج ما قبل الحصاد";
    else return "جاهز للحصاد";
}
```

### قسم المهام:

```java
@GetMapping("/api/user/plants/{plantId}/tasks")
public PlantTasksResponse getPlantTasks(@PathVariable Long plantId, @AuthenticationPrincipal UserPrincipal user) {
    List<UserPlantTask> allTasks = userPlantTaskService.getTasksByPlant(plantId, user.getId());
    
    // تصنيف المهام حسب الحالة
    List<UserPlantTask> todayTasks = allTasks.stream()
        .filter(task -> task.getDueDate().equals(LocalDate.now()) && task.getStatus() == TaskStatus.PENDING)
        .collect(Collectors.toList());
        
    List<UserPlantTask> overdueTasks = allTasks.stream()
        .filter(task -> task.getDueDate().isBefore(LocalDate.now()) && task.getStatus() == TaskStatus.PENDING)
        .collect(Collectors.toList());
        
    List<UserPlantTask> upcomingTasks = allTasks.stream()
        .filter(task -> task.getDueDate().isAfter(LocalDate.now()) && task.getStatus() == TaskStatus.PENDING)
        .collect(Collectors.toList());
    
    return PlantTasksResponse.builder()
        .todayTasks(mapToTaskResponses(todayTasks))
        .overdueTasks(mapToTaskResponses(overdueTasks))
        .upcomingTasks(mapToTaskResponses(upcomingTasks))
        .completedTasksCount(getCompletedTasksCount(allTasks))
        .build();
}

// إتمام مهمة
@PostMapping("/api/user/tasks/{taskId}/complete")
public TaskResponse completeTask(@PathVariable Long taskId, @AuthenticationPrincipal UserPrincipal user) {
    UserPlantTask task = userPlantTaskService.completeTask(taskId, user.getId());
    
    // 1. تحديث حالة المهمة
    task.setStatus(TaskStatus.COMPLETED);
    task.setCompletedAt(LocalDateTime.now());
    
    // 2. إنشاء المهمة التالية إذا كانت متكررة
    if (task.getTaskType().getIsRecurring()) {
        taskSchedulingService.scheduleNextTask(task);
    }
    
    // 3. تحديث إحصائيات المستخدم
    userStatisticsService.incrementTasksCompleted(user.getId());
    
    // 4. منح نقاط للمستخدم
    pointsService.awardPoints(user.getId(), task.getTaskType().getPoints());
    
    return mapToTaskResponse(task);
}
```

### قسم المعلومات:

```java
@GetMapping("/api/user/plants/{plantId}/info")
public PlantInfoResponse getPlantInfo(@PathVariable Long plantId) {
    UserPlant userPlant = userPlantService.getUserPlant(plantId);
    Plant plant = userPlant.getPlant();
    
    return PlantInfoResponse.builder()
        .careInfo(PlantCareInfo.builder()
            .lightInfoAr(plant.getLightInfoAr())
            .wateringInfoAr(plant.getWateringInfoAr())
            .soilInfoAr(plant.getSoilInfoAr())
            .careInfoAr(plant.getCareInfoAr())
            .build())
        .harvestInfo(PlantHarvestInfo.builder()
            .harvestInfoAr(plant.getHarvestInfoAr())
            .daysToHarvest(plant.getDaysToHarvest())
            .expectedHarvestDate(userPlant.getPlantedDate().plusDays(plant.getDaysToHarvest()))
            .harvestSigns(getHarvestSigns(plant))
            .build())
        .usageInfo(PlantUsageInfo.builder()
            .usesInfoAr(plant.getUsesInfoAr())
            .nutritionalBenefits(getNutritionalBenefits(plant))
            .cookingTips(getCookingTips(plant))
            .build())
        .build();
}
```

---

## ❓ **صفحة إضافة محصول (Add Crop)**

### الأسئلة الإجبارية:

```java
@GetMapping("/api/user/questions/planting")
public PlantingQuestionsResponse getPlantingQuestions() {
    // جلب الأسئلة الإجبارية والاختيارية
    List<PlantingQuestion> requiredQuestions = plantingQuestionService.getRequiredQuestions();
    List<PlantingQuestion> optionalQuestions = plantingQuestionService.getOptionalQuestions();
    
    return PlantingQuestionsResponse.builder()
        .requiredQuestions(mapToQuestionDTOs(requiredQuestions))
        .optionalQuestions(mapToQuestionDTOs(optionalQuestions))
        .totalQuestions(requiredQuestions.size() + optionalQuestions.size())
        .build();
}
```

### معالجة الإجابات وإرجاع الاقتراحات:

```java
@PostMapping("/api/user/questions/submit")
public PlantRecommendationsResponse submitAnswers(
    @RequestBody PlantingAnswersRequest answers,
    @AuthenticationPrincipal UserPrincipal user) {
    
    // 1. التحقق من صحة الإجابات
    validateAnswers(answers);
    
    // 2. حساب النقاط لكل نبات
    List<PlantCompatibility> compatibilities = plantRecommendationService.calculateCompatibility(answers);
    
    // 3. ترتيب النباتات حسب التوافق
    List<PlantRecommendation> recommendations = compatibilities.stream()
        .filter(compatibility -> compatibility.getCompatibilityPercentage() >= 60) // 60% حد أدنى
        .sorted((a, b) -> Double.compare(b.getCompatibilityPercentage(), a.getCompatibilityPercentage()))
        .limit(10) // أفضل 10 اقتراحات
        .map(this::buildRecommendation)
        .collect(Collectors.toList());
    
    return PlantRecommendationsResponse.builder()
        .recommendations(recommendations)
        .totalFound(recommendations.size())
        .searchCriteria(answers.getAnswerSummary())
        .build();
}

private PlantRecommendation buildRecommendation(PlantCompatibility compatibility) {
    Plant plant = compatibility.getPlant();
    
    // حساب أسباب الاقتراح
    List<String> reasons = reasonGenerator.generateReasons(compatibility);
    
    // حساب التوصيات للنجاح
    List<String> recommendations = recommendationGenerator.generateRecommendations(compatibility);
    
    return PlantRecommendation.builder()
        .plantId(plant.getId())
        .plantNameAr(plant.getNameAr())
        .plantNameEn(plant.getNameEn())
        .shortDescriptionAr(plant.getShortDescriptionAr())
        .imageUrl(plant.getMainImageUrl())
        .compatibilityPercentage(compatibility.getCompatibilityPercentage())
        .matchedCriteria(compatibility.getMatchedCriteria())
        .totalScore(compatibility.getTotalScore())
        .reasons(reasons)
        .recommendations(recommendations)
        .difficultyLevel(plant.getDifficultyLevel())
        .daysToHarvest(plant.getDaysToHarvest())
        .wateringFrequency(plant.getWateringIntervalDays())
        .build();
}
```

### اختيار النبات وإضافته للمخططات:

```java
@PostMapping("/api/user/plants/plan")
public UserPlantResponse planPlant(
    @RequestBody PlanPlantRequest request,
    @AuthenticationPrincipal UserPrincipal user) {
    
    // 1. التحقق من عدم وجود النبات مسبقاً
    if (userPlantService.hasPlantAlready(user.getId(), request.getPlantId())) {
        throw new DuplicatePlantException("النبات موجود بالفعل في خططك");
    }
    
    // 2. إنشاء خطة الزراعة
    UserPlant plannedPlant = UserPlant.builder()
        .user(userService.getUser(user.getId()))
        .plant(plantService.getPlant(request.getPlantId()))
        .status(PlantStatus.PLANNED)
        .nickname(request.getNickname())
        .plannedDate(request.getPlannedDate() != null ? request.getPlannedDate() : LocalDate.now())
        .notes(request.getNotes())
        .build();
    
    UserPlant saved = userPlantService.save(plannedPlant);
    
    // 3. تسجيل النشاط
    userActivityService.logActivity(
        user.getId(), 
        ActivityType.PLANT_PLANNED,
        "تم التخطيط لزراعة " + saved.getPlant().getNameAr()
    );
    
    return mapToUserPlantResponse(saved);
}
```

### خوارزمية حساب التوافق:

```java
@Service
public class PlantRecommendationService {
    
    public List<PlantCompatibility> calculateCompatibility(PlantingAnswersRequest answers) {
        List<Plant> allPlants = plantService.getAllActivePlants();
        
        return allPlants.stream()
            .map(plant -> calculatePlantScore(plant, answers))
            .filter(compatibility -> compatibility.getCompatibilityPercentage() > 0)
            .collect(Collectors.toList());
    }
    
    private PlantCompatibility calculatePlantScore(Plant plant, PlantingAnswersRequest answers) {
        List<PlantSuitability> suitabilities = plantSuitabilityService.getSuitabilitiesByPlant(plant.getId());
        
        int totalScore = 0;
        int matchedCriteria = 0;
        int totalPossibleScore = 0;
        
        // لكل إجابة، ابحث عن النقاط المقابلة
        for (Map.Entry<String, String> answer : answers.getAnswers().entrySet()) {
            String questionKey = answer.getKey();
            String optionKey = answer.getValue();
            
            // البحث عن PlantSuitability المطابقة
            Optional<PlantSuitability> suitability = suitabilities.stream()
                .filter(s -> s.getOption().getOptionKey().equals(optionKey))
                .findFirst();
                
            if (suitability.isPresent()) {
                totalScore += suitability.get().getScore();
                matchedCriteria++;
            }
            
            // حساب أقصى نقاط ممكنة لهذا السؤال
            totalPossibleScore += getMaxScoreForQuestion(questionKey);
        }
        
        // حساب النسبة المئوية
        double compatibilityPercentage = totalPossibleScore > 0 ? 
            (double) totalScore / totalPossibleScore * 100 : 0;
            
        return PlantCompatibility.builder()
            .plant(plant)
            .totalScore(totalScore)
            .matchedCriteria(matchedCriteria)
            .compatibilityPercentage(compatibilityPercentage)
            .build();
    }
}
```

---

## 🎯 **الخلاصة والتكامل**

### تدفق البيانات الكامل:

1. **المستخدم** يجيب على الأسئلة
2. **النظام** يحسب التوافق مع كل نبات
3. **خوارزمية الذكاء** ترتب النتائج
4. **المستخدم** يختار نباتات للتخطيط
5. **النظام** ينشئ جدولة المهام التلقائية
6. **الإشعارات** تذكر بالمهام المستحقة
7. **التتبع** يرصد التقدم والإحصائيات

### APIs الرئيسية المطلوبة:

```java
// الصفحة الرئيسية
GET /api/user/home/weather
GET /api/user/home/daily-quote
GET /api/user/home/planting-calendar
GET /api/user/home/statistics

// محاصيلي
GET /api/user/plants/planned
GET /api/user/plants/planted
POST /api/user/plants/{id}/plant
DELETE /api/user/plants/{id}

// تتبع النبتة
GET /api/user/plants/{id}/overview
GET /api/user/plants/{id}/tasks
GET /api/user/plants/{id}/info
POST /api/user/tasks/{id}/complete
POST /api/user/tasks/{id}/snooze

// إضافة محصول
GET /api/user/questions/planting
POST /api/user/questions/submit
POST /api/user/plants/plan
```

**النظام جاهز تماماً - كل ما نحتاجه هو تنفيذ controller methods و service logic! 🚀**
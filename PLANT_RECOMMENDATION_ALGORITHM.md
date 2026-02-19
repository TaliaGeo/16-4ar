# 🤖 خوارزمية اقتراح النباتات - كيف تعمل بالتفصيل

## 🧠 **فهم الخوارزمية**

### المبدأ الأساسي:
كل نبات له **نقاط توافق** مع كل خيار من خيارات الأسئلة. عندما المستخدم يجيب، النظام يجمع النقاط ويحسب نسبة التوافق.

---

## 📊 **1. هيكل البيانات**

### الجداول المشاركة:
```sql
-- أسئلة الزراعة (26 سؤال)
planting_questions: 
├── id, question_key, question_ar, is_required
├── "planting_location", "ما هو مكان الزراعة؟"
├── "space_size", "ما حجم المساحة المتاحة؟"  
├── "experience_level", "ما مستوى خبرتك؟"
└── ...

-- خيارات كل سؤال
question_options:
├── id, question_id, option_key, option_ar
├── "home_garden", "حديقة منزلية"
├── "balcony", "بلكونة" 
├── "rooftop", "سطح المنزل"
└── ...

-- نقاط التوافق (القلب!)
plant_suitability:
├── plant_id, option_id, score (0-100)
├── نبات البقدونس + "مبتدئ" = 85 نقطة
├── نبات البقدونس + "بلكونة" = 90 نقطة  
├── نبات الورد + "مبتدئ" = 30 نقطة
└── نبات الورد + "خبير" = 95 نقطة
```

---

## ⚙️ **2. خطوات الخوارزمية**

### الخطوة 1: **تحليل إجابات المستخدم**

```java
// المستخدم يرسل إجاباته
POST /api/user/questions/submit
{
    "answers": {
        "planting_location": "balcony",        // بلكونة
        "space_size": "small",                 // مساحة صغيرة  
        "experience_level": "beginner",        // مبتدئ
        "time_available": "medium",            // وقت متوسط
        "climate": "mediterranean"             // مناخ متوسطي
    }
}

// النظام يحول الإجابات لـ option_ids
resolveOptionIds(answers) {
    List<Long> optionIds = [];
    
    for (String optionKey : answers.values()) {
        QuestionOption option = questionOptionRepository.findByOptionKey(optionKey);
        optionIds.add(option.getId());
    }
    
    return optionIds; // [15, 23, 7, 41, 18]
}
```

### الخطوة 2: **حساب نقاط كل نبات**

```java
// استعلام جلب نقاط كل نبات حسب الخيارات المختارة  
@Query("SELECT ps.plant.id, SUM(ps.score) as totalScore " +
       "FROM PlantSuitability ps " + 
       "WHERE ps.option.id IN :optionIds " +
       "GROUP BY ps.plant.id " +
       "ORDER BY totalScore DESC")
List<Object[]> findPlantScoresByOptions(@Param("optionIds") List<Long> optionIds);

// مثال النتائج:
// Plant ID | Total Score
// ---------|------------  
//    5     |    425      (البقدونس - مجموع عالي)
//    18    |    380      (النعناع - جيد جداً)
//    12    |    320      (الريحان - جيد) 
//    25    |    180      (الورد - ضعيف للمبتدئين)
```

### الخطوة 3: **حساب نسبة التوافق**

```java
// لكل نبات، احسب النسبة المئوية
for (Object[] row : plantScores) {
    Long plantId = (Long) row[0];
    Long totalScore = (Long) row[1];
    
    // أقصى نقاط ممكنة = عدد الأسئلة × 100 نقطة
    int maxPossibleScore = selectedOptionIds.size() * 100; // 5 × 100 = 500
    
    // نسبة التوافق
    double matchPercentage = (totalScore.doubleValue() / maxPossibleScore) * 100;
    
    // مثال:
    // البقدونس: (425 / 500) × 100 = 85% توافق ممتاز! ✅
    // النعناع: (380 / 500) × 100 = 76% توافق جيد جداً
    // الريحان: (320 / 500) × 100 = 64% توافق جيد  
    // الورد: (180 / 500) × 100 = 36% توافق ضعيف ❌
}
```

### الخطوة 4: **فلترة وترتيب النتائج**

```java
// تطبيق الحد الأدنى للتوافق (عادة 60%)
if (matchPercentage < request.getMinMatchPercentage()) {
    continue; // تجاهل النباتات ضعيفة التوافق
}

// ترتيب حسب النسبة (الأعلى أولاً)
recommendations.sort((a, b) -> 
    Double.compare(b.getMatchPercentage(), a.getMatchPercentage()));

// أخذ أفضل 10 اقتراحات  
return recommendations.stream().limit(10).collect(Collectors.toList());
```

---

## 🔍 **3. تفاصيل التوافق**

### حساب تفاصيل كل سؤال:

```java
// للنبات الواحد، إظهار نقاط كل سؤال على حدة
private List<QuestionMatchDetail> getQuestionMatchDetails(Long plantId, List<Long> selectedOptionIds) {
    
    // جلب نقاط النبات لكل خيار محدد
    List<PlantSuitability> suitabilities = suitabilityRepository.findByPlantIdWithOptionDetails(plantId);
    
    // تجميع حسب السؤال
    Map<Long, List<PlantSuitability>> byQuestion = suitabilities.stream()
            .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
            .collect(Collectors.groupingBy(s -> s.getOption().getQuestion().getId()));
    
    List<QuestionMatchDetail> details = new ArrayList<>();
    
    for (Map.Entry<Long, List<PlantSuitability>> entry : byQuestion.entrySet()) {
        List<PlantSuitability> questionSuits = entry.getValue();
        PlantingQuestion question = questionSuits.get(0).getOption().getQuestion();
        
        // متوسط النقاط لهذا السؤال
        double avgScore = questionSuits.stream()
                .mapToInt(PlantSuitability::getScore)
                .average().orElse(0);
        
        QuestionMatchDetail detail = QuestionMatchDetail.builder()
                .questionTextAr(question.getQuestionTextAr())
                .selectedOptionText(getSelectedOptionsText(questionSuits))
                .score((int) avgScore)
                .scoreLevel(getScoreLevel((int) avgScore)) // "ممتاز", "جيد", "مقبول", "ضعيف"
                .build();
                
        details.add(detail);
    }
    
    return details;
}
```

---

## 🎯 **4. مثال عملي كامل**

### سيناريو: **مستخدم مبتدئ يريد زراعة في البلكونة**

#### إجابات المستخدم:
```json
{
    "planting_location": "balcony",       // بلكونة
    "space_size": "small",               // مساحة صغيرة
    "experience_level": "beginner",      // مبتدئ  
    "time_available": "low",             // وقت قليل
    "budget": "low"                      // ميزانية محدودة
}
```

#### النظام يحول للخيارات:
```java
selectedOptionIds = [12, 25, 7, 33, 19] // IDs الخيارات المحددة
```

#### حساب النقاط لكل نبات:

| النبات | مكان البلكونة | مساحة صغيرة | مبتدئ | وقت قليل | ميزانية محدودة | **المجموع** | **النسبة** |
|--------|-------------|-------------|------|---------|---------------|-----------|----------|
| **البقدونس** | 90 | 85 | 95 | 90 | 85 | **445/500** | **89%** ✅ |
| **النعناع** | 88 | 80 | 90 | 85 | 80 | **423/500** | **85%** ✅ |
| **الجرجير** | 75 | 90 | 85 | 95 | 90 | **435/500** | **87%** ✅ |
| **الريحان** | 70 | 75 | 80 | 70 | 75 | **370/500** | **74%** ✅ |
| **الورد** | 60 | 40 | 25 | 30 | 45 | **200/500** | **40%** ❌ |

#### النتيجة النهائية:
```json
{
    "recommendations": [
        {
            "plantId": 5,
            "nameAr": "بقدونس",
            "matchPercentage": 89.0,
            "matchLevel": "ممتاز",
            "questionMatches": [
                {
                    "questionTextAr": "ما هو مكان الزراعة؟",
                    "selectedOptionText": "بلكونة", 
                    "score": 90,
                    "scoreLevel": "ممتاز"
                },
                {
                    "questionTextAr": "ما مستوى خبرتك في الزراعة؟",
                    "selectedOptionText": "مبتدئ",
                    "score": 95,
                    "scoreLevel": "ممتاز" 
                }
                // ... باقي الأسئلة
            ],
            "adjustmentTips": [
                "البقدونس خيار مثالي للمبتدئين",
                "ينمو جيداً في البلكونة", 
                "يحتاج عناية بسيطة"
            ]
        }
        // ... باقي الاقتراحات
    ]
}
```

---

## 💡 **5. ذكاء الخوارزمية**

### الميزات المتقدمة:

#### أ) **نصائح التحسين**
```java
private List<String> getAdjustmentTips(Long plantId, List<Long> selectedOptionIds) {
    List<String> tips = new ArrayList<>();
    
    // البحث عن نقاط ضعيفة (أقل من 50)
    List<PlantSuitability> lowScores = suitabilityRepository.findByPlantIdWithOptionDetails(plantId)
            .stream()
            .filter(s -> selectedOptionIds.contains(s.getOption().getId()))
            .filter(s -> s.getScore() < 50)
            .collect(Collectors.toList());
    
    for (PlantSuitability lowScore : lowScores) {
        tips.add("يمكن تحسين " + lowScore.getOption().getQuestion().getQuestionTextAr() + 
                " عبر " + lowScore.getAdjustmentTip());
    }
    
    return tips;
}
```

#### ب) **مستويات التوافق**
```java
private String getMatchLevelAr(double percentage) {
    if (percentage >= 85) return "ممتاز - مناسب جداً لك! 🌟";
    if (percentage >= 75) return "جيد جداً - اختيار موفق 👍";  
    if (percentage >= 65) return "جيد - يمكن أن ينجح 🙂";
    if (percentage >= 50) return "مقبول - قد يحتاج عناية إضافية ⚠️";  
    return "غير مناسب - لا ننصح به ❌";
}
```

#### ج) **فلترة ذكية**
```java
// إزالة النباتات غير المناسبة تماماً
if (matchPercentage < 60) continue;

// إعطاء أولوية للنباتات سهلة العناية للمبتدئين
if (isBeginnerUser && plant.getDifficultyLevel() == EASY) {
    matchPercentage += 5; // بونوس للمبتدئين
}

// أولوية للنباتات سريعة النمو
if (plant.getDaysToHarvest() < 30) {
    matchPercentage += 3; // بونوس للنتائج السريعة  
}
```

---

## 🎉 **النتيجة النهائية**

### خوارزمية ذكية تعطي:
✅ **اقتراحات دقيقة** حسب ظروف المستخدم  
✅ **تفسير مفصل** لسبب كل اقتراح  
✅ **نصائح عملية** لتحسين النجاح  
✅ **ترتيب منطقي** من الأفضل للأقل مناسبة  

**النظام يفهم احتياجات المستخدم ويقترح النباتات المثالية له! 🌱🧠**
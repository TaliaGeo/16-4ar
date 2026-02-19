# تصميم قاعدة البيانات الشاملة - مشروع غرسيه

## 📋 **الوضع الحالي - ما موجود بالفعل ✅**

### 1. نظام الأدمن والصلاحيات
- ✅ Users (المستخدمين)
- ✅ Roles (الأدوار: ADMIN, USER)  
- ✅ Permissions (الصلاحيات)
- ✅ OAuth2 Integration (Google & Facebook)

### 2. النباتات والتقويم الزراعي
- ✅ Plants (النباتات الأساسية)
- ✅ Months (الأشهر الـ12 + معلومات الطقس الفلسطيني)
- ✅ MonthPlants (علاقة الأشهر بالنباتات)
- ✅ PlantImages (صور النباتات)

### 3. أسئلة إضافة المحصول
- ✅ PlantingQuestions (الأسئلة الخمسة الإجبارية + الاختيارية)
- ✅ QuestionOptions (خيارات كل سؤال)
- ✅ PlantSuitability (نقاط توافق النباتات مع الخيارات)

### 4. نظام محاصيل المستخدم
- ✅ UserPlants (محاصيل المستخدم)
- ✅ PlantTasks (المهام الافتراضية)
- ✅ UserPlantTasks (مهام المستخدم الفعلية)
- ✅ TaskTypes (أنواع المهام: ري، تسميد، حصاد)
- ✅ WateringHistory (سجل الري)
- ✅ Notifications (الإشعارات)

---

## 📊 **ما نحتاجه إضافياً للنظام الكامل**

### 1. الصفحة الرئيسية (Home Page)

#### A. معلومات الطقس اليومية 🌤️
```sql
-- جدول الطقس اليومي (يمكن ملؤه من API خارجي أو يدوياً)
CREATE TABLE daily_weather (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    weather_date DATE NOT NULL UNIQUE,
    temperature_celsius INTEGER NOT NULL,
    weather_condition VARCHAR(50) NOT NULL, -- sunny, cloudy, rainy, etc.
    weather_description_ar TEXT,
    weather_description_en TEXT,
    weather_icon_url VARCHAR(255),
    humidity_percentage INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### B. الاقتباسات والتحفيز اليومي ✨
```sql
-- جدول الاقتباسات والنصائح اليومية
CREATE TABLE daily_quotes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_ar TEXT NOT NULL,
    quote_en TEXT,
    author_ar VARCHAR(100),
    author_en VARCHAR(100),
    category VARCHAR(50) DEFAULT 'motivation', -- motivation, tip, wisdom
    is_active BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- جدول لتتبع اقتباس اليوم لكل مستخدم
CREATE TABLE user_daily_quotes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quote_id BIGINT NOT NULL,
    shown_date DATE NOT NULL,
    is_liked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (quote_id) REFERENCES daily_quotes(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_date (user_id, shown_date)
);
```

### 2. نظام الإحصائيات والتتبع 📈

#### A. إحصائيات المستخدم
```sql
-- جدول إحصائيات المستخدم الشخصية
CREATE TABLE user_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_plants_planted INTEGER DEFAULT 0,
    total_plants_harvested INTEGER DEFAULT 0,
    current_active_plants INTEGER DEFAULT 0,
    total_tasks_completed INTEGER DEFAULT 0,
    current_streak_days INTEGER DEFAULT 0, -- الأيام المتتالية للعناية
    longest_streak_days INTEGER DEFAULT 0,
    last_activity_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### B. سجل نشاطات المستخدم
```sql
-- جدول لتسجيل نشاطات المستخدم
CREATE TABLE user_activity_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL, -- plant_added, task_completed, plant_harvested, etc.
    activity_description_ar TEXT,
    activity_description_en TEXT,
    related_plant_id BIGINT,
    related_task_id BIGINT,
    points_earned INTEGER DEFAULT 0,
    activity_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (related_plant_id) REFERENCES plants(id) ON DELETE SET NULL,
    FOREIGN KEY (related_task_id) REFERENCES user_plant_tasks(id) ON DELETE SET NULL
);
```

### 3. نظام التذكيرات المتقدم ⏰

#### A. إعدادات التذكيرات لكل مستخدم
```sql
-- جدول إعدادات التذكيرات الشخصية
CREATE TABLE user_notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    watering_reminders_enabled BOOLEAN DEFAULT TRUE,
    fertilizing_reminders_enabled BOOLEAN DEFAULT TRUE,
    harvest_reminders_enabled BOOLEAN DEFAULT TRUE,
    daily_tips_enabled BOOLEAN DEFAULT TRUE,
    reminder_time TIME DEFAULT '08:00:00', -- وقت التذكير المفضل
    timezone VARCHAR(50) DEFAULT 'Asia/Gaza',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 4. نظام التقييم والمراجعات 🌟

#### A. تقييمات النباتات
```sql
-- جدول تقييمات النباتات من المستخدمين
CREATE TABLE plant_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text_ar TEXT,
    review_text_en TEXT,
    difficulty_rating INTEGER CHECK (difficulty_rating >= 1 AND difficulty_rating <= 5),
    would_recommend BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_plant_review (user_id, plant_id)
);
```

### 5. نظام البحث والفلترة المتقدم 🔍

#### A. تحسين جدول النباتات للبحث
```sql
-- إضافة فهارس للبحث السريع
ALTER TABLE plants ADD FULLTEXT(name_ar, name_en, short_description_ar, short_description_en);

-- جدول الكلمات المفتاحية للنباتات
CREATE TABLE plant_keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    keyword_ar VARCHAR(100) NOT NULL,
    keyword_en VARCHAR(100),
    keyword_type VARCHAR(50) DEFAULT 'general', -- general, use, benefit, season
    FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE CASCADE,
    INDEX idx_keyword_ar (keyword_ar),
    INDEX idx_keyword_en (keyword_en)
);
```

### 6. نظام المساعد الذكي (للمستقبل) 🤖

#### A. سجل المحادثات
```sql
-- جدول محادثات المساعد الذكي
CREATE TABLE ai_chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    total_messages INTEGER DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- جدول رسائل المحادثة
CREATE TABLE ai_chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    message_type ENUM('user', 'assistant') NOT NULL,
    message_content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES ai_chat_sessions(id) ON DELETE CASCADE
);
```

---

## 🔄 **خوارزمية اقتراح النباتات**

### كيف تعمل عملية الاقتراح:

#### 1. **المستخدم يجيب على الأسئلة**
```json
{
  "location": "balcony",
  "sunlight": "partial_sun_3_6_hours", 
  "container": "medium_pot",
  "watering_frequency": "every_2_3_days",
  "soil_type": "ready_potting_soil",
  "drainage": "good",
  "has_drainage_holes": "yes",
  "preferences": ["beginner_friendly", "for_cooking"]
}
```

#### 2. **النظام يحسب النقاط لكل نبات**
```sql
-- استعلام حساب النقاط
SELECT 
    p.id,
    p.name_ar,
    p.name_en,
    p.short_description_ar,
    SUM(ps.score) as total_score,
    COUNT(ps.id) as matched_criteria,
    ROUND((SUM(ps.score) / COUNT(ps.id)), 2) as compatibility_percentage
FROM plants p
JOIN plant_suitability ps ON p.id = ps.plant_id
JOIN question_options qo ON ps.option_id = qo.id
WHERE qo.option_key IN ('balcony', 'partial_sun_3_6_hours', 'medium_pot', 'every_2_3_days', 'ready_potting_soil', 'good', 'yes', 'beginner_friendly', 'for_cooking')
GROUP BY p.id
HAVING matched_criteria >= 6  -- يجب أن يطابق 6 معايير على الأقل
ORDER BY compatibility_percentage DESC, total_score DESC
LIMIT 10;
```

#### 3. **إرجاع النتائج مع التوصيات**
```json
{
  "recommendations": [
    {
      "plant_id": 1,
      "plant_name_ar": "البقدونس",
      "plant_name_en": "Parsley",
      "compatibility_percentage": 95,
      "total_score": 480,
      "matched_criteria": 8,
      "reasons": [
        "مناسب للشرفة",
        "يحتاج ضوء جزئي", 
        "سهل للمبتدئين",
        "ممتاز للطبخ"
      ],
      "recommendations": [
        "تأكد من وجود تصريف جيد",
        "اروي كل 2-3 أيام"
      ]
    }
  ]
}
```

---

## 📝 **جداول إضافية مطلوبة**

### 1. **لتحسين تجربة المستخدم**
```sql
-- جدول النصائح اليومية حسب الموسم
CREATE TABLE seasonal_tips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    month_number INTEGER NOT NULL CHECK (month_number >= 1 AND month_number <= 12),
    tip_ar TEXT NOT NULL,
    tip_en TEXT,
    tip_type VARCHAR(50) DEFAULT 'general', -- care, planting, harvest, weather
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- جدول مشاكل النباتات الشائعة وحلولها
CREATE TABLE plant_problems (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT,
    problem_name_ar VARCHAR(200) NOT NULL,
    problem_name_en VARCHAR(200),
    problem_description_ar TEXT,
    problem_description_en TEXT,
    solution_ar TEXT NOT NULL,
    solution_en TEXT,
    prevention_tips_ar TEXT,
    prevention_tips_en TEXT,
    severity_level ENUM('low', 'medium', 'high') DEFAULT 'medium',
    FOREIGN KEY (plant_id) REFERENCES plants(id) ON DELETE CASCADE
);
```

### 2. **للتقويم الزراعي المتقدم**
```sql
-- جدول الفصول والمناخ الفلسطيني التفصيلي
CREATE TABLE palestine_seasons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    season_name_ar VARCHAR(50) NOT NULL,
    season_name_en VARCHAR(50) NOT NULL,
    start_month INTEGER NOT NULL,
    end_month INTEGER NOT NULL,
    avg_temperature_min INTEGER,
    avg_temperature_max INTEGER,
    rainfall_description_ar TEXT,
    rainfall_description_en TEXT,
    farming_activities_ar TEXT,
    farming_activities_en TEXT
);
```

---

## 🎯 **الخلاصة النهائية**

### ✅ **موجود ويعمل (95% مكتمل)**
1. **نظام الأدمن** - كامل مع الصلاحيات
2. **النباتات الأساسية** - معلومات شاملة 
3. **التقويم الزراعي** - الأشهر + النباتات الموسمية
4. **أسئلة المحصول** - الخوارزمية جاهزة
5. **محاصيل المستخدم** - التتبع الكامل
6. **نظام المهام** - ري، تسميد، حصاد
7. **الإشعارات** - تذكيرات ذكية

### ⚠️ **يحتاج إضافة بسيطة**
1. **الطقس اليومي** - جدول + API integration
2. **الاقتباسات اليومية** - جدول بسيط
3. **الإحصائيات** - تتبع نشاط المستخدم
4. **نظام التقييم** - اختياري للتحسين

### 🏗️ **الهيكل مجهز لـ**
- تطبيق الموبايل (كل ال APIs جاهزة)
- المساعد الذكي (الأساس موجود)
- نظام AR (يمكن إضافة معلومات النباتات)

**النظام 95% جاهز لجميع الميزات المطلوبة! 🎉**
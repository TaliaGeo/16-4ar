# ====================================================================
# سيناريو اختبار كامل - إضافة محصول (Plant Recommendation)
# ====================================================================
# هذا السكريبت يغطي المسار الكامل:
# 1. ادمن يسجل دخول
# 2. ادمن يبني الأسئلة الافتراضية (initialize)
# 3. ادمن يضيف 3 نباتات (نعنع، زعتر، بابونج)
# 4. ادمن يحدد نقاط الملاءمة (Suitability) لكل نبتة مع كل خيار
# 5. يوزر يسجل دخول
# 6. يوزر يجلب الأسئلة
# 7. يوزر يجاوب عالأسئلة
# 8. يوزر يشوف الاقتراحات
# 9. يوزر يضغط على اقتراح يشوف تفاصيله
# 10. يوزر يختار نبتة → تنضاف لمحاصيله
# ====================================================================

$BASE = "http://localhost:8081/api"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Start: Full Plant Recommendation Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ====================================================================
# الخطوة 1: تسجيل دخول الأدمن
# ====================================================================
Write-Host "`n--- Step 1: Admin Login ---" -ForegroundColor Yellow

$adminLogin = @{
    email    = "admin@admin.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $adminRes = Invoke-RestMethod -Uri "$BASE/auth/login" -Method POST -Body $adminLogin -ContentType "application/json"
    $ADMIN_TOKEN = $adminRes.accessToken
    Write-Host "Admin logged in successfully!" -ForegroundColor Green
    Write-Host "Token: $($ADMIN_TOKEN.Substring(0, 30))..." -ForegroundColor DarkGray
} catch {
    Write-Host "Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure admin account exists. Try registering first." -ForegroundColor Red
    
    # محاولة تسجيل أدمن جديد
    try {
        $registerAdmin = @{
            fullName = "Admin"
            email    = "admin@admin.com"
            password = "admin123"
        } | ConvertTo-Json
        $regRes = Invoke-RestMethod -Uri "$BASE/auth/register" -Method POST -Body $registerAdmin -ContentType "application/json"
        $ADMIN_TOKEN = $regRes.accessToken
        Write-Host "Admin registered & logged in!" -ForegroundColor Green
    } catch {
        Write-Host "Could not register admin either. Check server status." -ForegroundColor Red
        exit
    }
}

$adminHeaders = @{
    "Authorization" = "Bearer $ADMIN_TOKEN"
    "Content-Type"  = "application/json"
}

# ====================================================================
# الخطوة 2: إنشاء الأسئلة الافتراضية
# ====================================================================
Write-Host "`n--- Step 2: Initialize Default Questions ---" -ForegroundColor Yellow

try {
    $questionsInit = Invoke-RestMethod -Uri "$BASE/admin/planting-questions/initialize" -Method POST -Headers $adminHeaders
    Write-Host "Questions initialized!" -ForegroundColor Green
} catch {
    Write-Host "Questions may already exist (OK): $($_.Exception.Message)" -ForegroundColor DarkYellow
}

# جلب الأسئلة مع الخيارات
$questions = Invoke-RestMethod -Uri "$BASE/admin/planting-questions/active/with-options" -Method GET -Headers $adminHeaders
Write-Host "Active questions count: $($questions.Count)" -ForegroundColor Green

# حفظ IDs الخيارات المهمة
$optionIds = @{}

foreach ($q in $questions) {
    Write-Host "  Q: $($q.questionKey) - $($q.questionTextAr) (ID: $($q.id))" -ForegroundColor DarkCyan
    foreach ($opt in $q.options) {
        $optionIds["$($q.questionKey)_$($opt.optionKey)"] = $opt.id
        Write-Host "    Option: $($opt.optionKey) = $($opt.optionTextAr) (ID: $($opt.id))" -ForegroundColor DarkGray
    }
}

# ====================================================================
# الخطوة 3: إضافة 3 نباتات
# ====================================================================
Write-Host "`n--- Step 3: Add 3 Plants ---" -ForegroundColor Yellow

# --- نبتة 1: النعنع ---
$mint = @{
    nameAr             = "النعنع"
    nameEn             = "Mint"
    nameScientific     = "Mentha spp"
    shortDescriptionAr = "نبتة عطرية سهلة الزراعة، ممتازة للمبتدئين"
    shortDescriptionEn = "Easy aromatic herb, perfect for beginners"
    lightInfoAr        = "شمس جزئية إلى ضوء غير مباشر"
    lightInfoEn        = "Partial sun to indirect light"
    soilInfoAr         = "تربة غنية رطبة جيدة التصريف"
    soilInfoEn         = "Rich, moist, well-drained soil"
    wateringInfoAr     = "ري منتظم - التربة تبقى رطبة"
    wateringInfoEn     = "Regular watering - keep soil moist"
    wateringIntervalDays = 2
    careInfoAr         = "قص الأوراق بانتظام لتشجيع النمو"
    careInfoEn         = "Regularly trim leaves to encourage growth"
    harvestInfoAr      = "يمكن حصاد الأوراق بعد 3-4 أسابيع"
    harvestInfoEn      = "Leaves can be harvested after 3-4 weeks"
    usesInfoAr         = "شاي، طبخ، مشروبات"
    usesInfoEn         = "Tea, cooking, beverages"
    daysToHarvest      = 25
    germinationDays    = 10
    minTemp            = 15
    maxTemp            = 30
    difficultyLevel    = "EASY"
    category           = "HERBS"
} | ConvertTo-Json

try {
    $mintRes = Invoke-RestMethod -Uri "$BASE/admin/plants" -Method POST -Headers $adminHeaders -Body $mint
    $MINT_ID = $mintRes.id
    Write-Host "Mint created! ID: $MINT_ID" -ForegroundColor Green
} catch {
    Write-Host "Mint creation issue: $($_.Exception.Message)" -ForegroundColor DarkYellow
    # محاولة جلب الموجود
    $allPlants = Invoke-RestMethod -Uri "$BASE/admin/plants?page=0&size=100" -Method GET -Headers $adminHeaders
    $existing = $allPlants.content | Where-Object { $_.nameAr -eq "النعنع" } | Select-Object -First 1
    if ($existing) { $MINT_ID = $existing.id; Write-Host "Using existing mint ID: $MINT_ID" -ForegroundColor DarkYellow }
}

# --- نبتة 2: الزعتر ---
$thyme = @{
    nameAr             = "الزعتر البلدي"
    nameEn             = "Thyme"
    nameScientific     = "Thymus vulgaris"
    shortDescriptionAr = "نبتة عطرية تقليدية، تتحمل الجفاف"
    shortDescriptionEn = "Traditional aromatic herb, drought tolerant"
    lightInfoAr        = "شمس مباشرة كاملة"
    lightInfoEn        = "Full direct sun"
    soilInfoAr         = "تربة خفيفة جيدة التصريف"
    soilInfoEn         = "Light, well-drained soil"
    wateringInfoAr     = "ري قليل - تحمل الجفاف"
    wateringInfoEn     = "Low watering - drought tolerant"
    wateringIntervalDays = 5
    careInfoAr         = "تقليم بعد الإزهار، لا تفرط بالري"
    careInfoEn         = "Prune after flowering, don't overwater"
    harvestInfoAr      = "حصاد الأوراق عند الحاجة بعد شهرين"
    harvestInfoEn      = "Harvest leaves as needed after 2 months"
    usesInfoAr         = "طبخ، توابل، شاي"
    usesInfoEn         = "Cooking, seasoning, tea"
    daysToHarvest      = 60
    germinationDays    = 14
    minTemp            = 10
    maxTemp            = 35
    difficultyLevel    = "EASY"
    category           = "HERBS"
} | ConvertTo-Json

try {
    $thymeRes = Invoke-RestMethod -Uri "$BASE/admin/plants" -Method POST -Headers $adminHeaders -Body $thyme
    $THYME_ID = $thymeRes.id
    Write-Host "Thyme created! ID: $THYME_ID" -ForegroundColor Green
} catch {
    Write-Host "Thyme creation issue: $($_.Exception.Message)" -ForegroundColor DarkYellow
    $allPlants = Invoke-RestMethod -Uri "$BASE/admin/plants?page=0&size=100" -Method GET -Headers $adminHeaders
    $existing = $allPlants.content | Where-Object { $_.nameAr -eq "الزعتر البلدي" } | Select-Object -First 1
    if ($existing) { $THYME_ID = $existing.id; Write-Host "Using existing thyme ID: $THYME_ID" -ForegroundColor DarkYellow }
}

# --- نبتة 3: البابونج ---
$chamomile = @{
    nameAr             = "البابونج"
    nameEn             = "Chamomile"
    nameScientific     = "Matricaria chamomilla"
    shortDescriptionAr = "نبتة طبية مهدئة، للشاي والاسترخاء"
    shortDescriptionEn = "Calming medicinal herb, for tea and relaxation"
    lightInfoAr        = "شمس مباشرة إلى جزئية"
    lightInfoEn        = "Full to partial sun"
    soilInfoAr         = "تربة خفيفة جيدة التصريف"
    soilInfoEn         = "Light, well-drained soil"
    wateringInfoAr     = "ري متوسط - لا تفرط"
    wateringInfoEn     = "Moderate watering - don't overwater"
    wateringIntervalDays = 3
    careInfoAr         = "قطف الأزهار بانتظام لتشجيع الإزهار"
    careInfoEn         = "Pick flowers regularly to encourage blooming"
    harvestInfoAr      = "قطف الأزهار عند التفتح الكامل"
    harvestInfoEn      = "Pick flowers when fully open"
    usesInfoAr         = "شاي، علاج، تهدئة"
    usesInfoEn         = "Tea, remedy, calming"
    daysToHarvest      = 70
    germinationDays    = 14
    minTemp            = 8
    maxTemp            = 30
    difficultyLevel    = "MEDIUM"
    category           = "MEDICINAL_HERBS"
} | ConvertTo-Json

try {
    $chamomileRes = Invoke-RestMethod -Uri "$BASE/admin/plants" -Method POST -Headers $adminHeaders -Body $chamomile
    $CHAMOMILE_ID = $chamomileRes.id
    Write-Host "Chamomile created! ID: $CHAMOMILE_ID" -ForegroundColor Green
} catch {
    Write-Host "Chamomile creation issue: $($_.Exception.Message)" -ForegroundColor DarkYellow
    $allPlants = Invoke-RestMethod -Uri "$BASE/admin/plants?page=0&size=100" -Method GET -Headers $adminHeaders
    $existing = $allPlants.content | Where-Object { $_.nameAr -eq "البابونج" } | Select-Object -First 1
    if ($existing) { $CHAMOMILE_ID = $existing.id; Write-Host "Using existing chamomile ID: $CHAMOMILE_ID" -ForegroundColor DarkYellow }
}

Write-Host "`nPlant IDs -> Mint: $MINT_ID, Thyme: $THYME_ID, Chamomile: $CHAMOMILE_ID" -ForegroundColor Cyan

# ====================================================================
# الخطوة 4: تحديد نقاط الملاءمة (Suitability Scores)
# ====================================================================
Write-Host "`n--- Step 4: Set Suitability Scores ---" -ForegroundColor Yellow

# شرح النقاط:
# 100 = ممتاز (الظرف مثالي لهالنبتة)
# 80  = جيد جداً
# 60  = جيد (بيزبط بس مش الأفضل)
# 40  = يحتاج تعديل (بيزبط مع نصيحة)
# 20  = ضعيف
# 0   = غير مناسب

# ============================================================
# الجدول الكامل لنقاط الملاءمة:
# ============================================================
#
# | السؤال       | الخيار          | النعنع | الزعتر | البابونج |
# |-------------|----------------|--------|--------|---------|
# | site        | indoor         |  90    |  40    |  50     |
# | site        | balcony        |  95    |  80    |  85     |
# | site        | rooftop        |  70    |  95    |  80     |
# | site        | garden         |  85    | 100    |  90     |
# | site        | unknown        |  60    |  50    |  55     |
# | light       | full_sun       |  60    | 100    |  85     |
# | light       | partial_sun    | 100    |  70    |  90     |
# | light       | bright_indirect|  85    |  40    |  60     |
# | light       | low_light      |  50    |  15    |  30     |
# | light       | unknown        |  60    |  40    |  50     |
# | container   | small_pot      |  80    |  70    |  50     |
# | container   | medium_pot     | 100    |  90    |  85     |
# | container   | large_pot      |  90    | 100    |  95     |
# | container   | bed_or_ground  |  85    | 100    | 100     |
# | container   | unknown        |  60    |  50    |  50     |
# | water       | daily          |  90    |  30    |  50     |
# | water       | every_2_3      | 100    |  70    |  90     |
# | water       | weekly         |  40    | 100    |  60     |
# | water       | irregular      |  20    |  80    |  30     |
# | water       | unknown        |  50    |  50    |  50     |
# | soil        | potting_mix    | 100    |  80    |  85     |
# | soil        | garden_soil    |  70    |  90    |  80     |
# | soil        | sandy          |  40    |  95    |  70     |
# | soil        | clay           |  30    |  20    |  25     |
# | soil        | compost_mix    |  90    |  70    |  80     |
# | soil        | unknown        |  50    |  50    |  50     |
# ============================================================

# بناء مصفوفة الملاءمة
$suitabilityData = @(
    # ===== site (أين ستزرع) =====
    @{ qKey="site"; oKey="indoor";          mint=90;  thyme=40;  chamomile=50;  mintTipAr="";                                                      thymeTipAr="يفضل خارج المنزل (شرفة/سطح) لنتيجة أفضل";     chamomileTipAr="يفضل مكان بضوء أكثر" },
    @{ qKey="site"; oKey="balcony";         mint=95;  thyme=80;  chamomile=85;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="site"; oKey="rooftop";         mint=70;  thyme=95;  chamomile=80;  mintTipAr="وفرلو ظل جزئي";                                         thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="site"; oKey="garden";          mint=85;  thyme=100; chamomile=90;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="site"; oKey="unknown";         mint=60;  thyme=50;  chamomile=55;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    
    # ===== light (الضوء) =====
    @{ qKey="light"; oKey="full_sun";       mint=60;  thyme=100; chamomile=85;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="light"; oKey="partial_sun";    mint=100; thyme=70;  chamomile=90;  mintTipAr="";                                                      thymeTipAr="حاول تزيد التعرض للشمس";                        chamomileTipAr="" },
    @{ qKey="light"; oKey="bright_indirect"; mint=85;  thyme=40;  chamomile=60; mintTipAr="";                                                      thymeTipAr="الضوء قريب من المطلوب، انقل النبات لمكان أشمس"; chamomileTipAr="حاول توفير ضوء مباشر ساعتين يومياً" },
    @{ qKey="light"; oKey="low_light";      mint=50;  thyme=15;  chamomile=30;  mintTipAr="افحص رطوبة التربة قبل الري وحافظ على تصريف جيد";         thymeTipAr="الزعتر يحتاج شمس مباشرة، هالمكان غير مناسب";   chamomileTipAr="البابونج يحتاج ضوء أكثر" },
    @{ qKey="light"; oKey="unknown";        mint=60;  thyme=40;  chamomile=50;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    
    # ===== container (الوعاء) =====
    @{ qKey="container"; oKey="small_pot";   mint=80;  thyme=70;  chamomile=50;  mintTipAr="";                                                     thymeTipAr="";                                              chamomileTipAr="حجم الوعاء مناسب لكن الأكبر أفضل" },
    @{ qKey="container"; oKey="medium_pot";  mint=100; thyme=90;  chamomile=85;  mintTipAr="";                                                     thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="container"; oKey="large_pot";   mint=90;  thyme=100; chamomile=95;  mintTipAr="";                                                     thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="container"; oKey="bed_or_ground"; mint=85; thyme=100; chamomile=100; mintTipAr="";                                                   thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="container"; oKey="unknown";     mint=60;  thyme=50;  chamomile=50;  mintTipAr="";                                                     thymeTipAr="";                                              chamomileTipAr="" },
    
    # ===== water (الري) =====
    @{ qKey="water"; oKey="daily";          mint=90;  thyme=30;  chamomile=50;  mintTipAr="";                                                      thymeTipAr="الزعتر ما بحب ماء كثير، خفف الري";             chamomileTipAr="قلل الري شوي" },
    @{ qKey="water"; oKey="every_2_3";      mint=100; thyme=70;  chamomile=90;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="water"; oKey="weekly";         mint=40;  thyme=100; chamomile=60;  mintTipAr="النعنع بحب رطوبة أكثر، حاول تزيد الري";                  thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="water"; oKey="irregular";      mint=20;  thyme=80;  chamomile=30;  mintTipAr="النعنع لازم ري منتظم، ممكن يذبل";                        thymeTipAr="";                                              chamomileTipAr="البابونج يحتاج ري منتظم أكثر" },
    @{ qKey="water"; oKey="unknown";        mint=50;  thyme=50;  chamomile=50;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    
    # ===== soil (التربة) =====
    @{ qKey="soil"; oKey="potting_mix";     mint=100; thyme=80;  chamomile=85;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="soil"; oKey="garden_soil";     mint=70;  thyme=90;  chamomile=80;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="soil"; oKey="sandy";           mint=40;  thyme=95;  chamomile=70;  mintTipAr="أضف كمبوست للتربة الرملية";                              thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="soil"; oKey="clay";            mint=30;  thyme=20;  chamomile=25;  mintTipAr="تربة طينية ثقيلة، أضف رمل وكمبوست";                     thymeTipAr="تربة طينية غير مناسبة، استبدلها";              chamomileTipAr="أضف رمل لتحسين التصريف" },
    @{ qKey="soil"; oKey="compost_mix";     mint=90;  thyme=70;  chamomile=80;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" },
    @{ qKey="soil"; oKey="unknown";         mint=50;  thyme=50;  chamomile=50;  mintTipAr="";                                                      thymeTipAr="";                                              chamomileTipAr="" }
)

$totalCreated = 0
$totalFailed = 0

foreach ($row in $suitabilityData) {
    $qKey = $row.qKey
    $oKey = $row.oKey
    $optId = $optionIds["${qKey}_${oKey}"]
    
    if (-not $optId) {
        Write-Host "  WARNING: Option not found: ${qKey}_${oKey}" -ForegroundColor Red
        continue
    }
    
    # إضافة لكل نبتة
    $plants = @(
        @{ id=$MINT_ID;      score=$row.mint;      tipAr=$row.mintTipAr;      name="Mint" },
        @{ id=$THYME_ID;     score=$row.thyme;     tipAr=$row.thymeTipAr;     name="Thyme" },
        @{ id=$CHAMOMILE_ID; score=$row.chamomile; tipAr=$row.chamomileTipAr; name="Chamomile" }
    )
    
    foreach ($p in $plants) {
        $suitReq = @{
            plantId        = $p.id
            optionId       = $optId
            score          = $p.score
            adjustmentTipAr = $p.tipAr
            adjustmentTipEn = ""
        } | ConvertTo-Json
        
        try {
            $null = Invoke-RestMethod -Uri "$BASE/admin/plant-suitability" -Method POST -Headers $adminHeaders -Body $suitReq
            $totalCreated++
        } catch {
            # قد تكون موجودة مسبقاً
            $totalFailed++
        }
    }
}

Write-Host "Suitability scores: $totalCreated created, $totalFailed skipped (may already exist)" -ForegroundColor Green

# ====================================================================
# الآن نتحول لليوزر العادي
# ====================================================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Now: USER FLOW" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ====================================================================
# الخطوة 5: تسجيل دخول/تسجيل يوزر
# ====================================================================
Write-Host "`n--- Step 5: User Login ---" -ForegroundColor Yellow

# تسجيل يوزر جديد
$userRegister = @{
    fullName = "Ahmad Test"
    email    = "ahmad@test.com"
    password = "test1234"
} | ConvertTo-Json

try {
    $null = Invoke-RestMethod -Uri "$BASE/auth/register" -Method POST -Body $userRegister -ContentType "application/json"
    Write-Host "User registered!" -ForegroundColor Green
} catch {
    Write-Host "User may already exist (OK)" -ForegroundColor DarkYellow
}

$userLogin = @{
    email    = "ahmad@test.com"
    password = "test1234"
} | ConvertTo-Json

try {
    $userRes = Invoke-RestMethod -Uri "$BASE/auth/login" -Method POST -Body $userLogin -ContentType "application/json"
    $USER_TOKEN = $userRes.accessToken
    Write-Host "User logged in!" -ForegroundColor Green
} catch {
    Write-Host "User login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

$userHeaders = @{
    "Authorization" = "Bearer $USER_TOKEN"
    "Content-Type"  = "application/json"
}

# ====================================================================
# الخطوة 6: جلب الأسئلة (كما يراها اليوزر)
# ====================================================================
Write-Host "`n--- Step 6: GET Questions (User View) ---" -ForegroundColor Yellow

$userQuestions = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/questions" -Method GET -Headers $userHeaders

Write-Host "Required questions: $($userQuestions.totalRequired)" -ForegroundColor Green
Write-Host "Optional questions: $($userQuestions.totalOptional)" -ForegroundColor Green

Write-Host "`nRequired:" -ForegroundColor White
foreach ($q in $userQuestions.requiredQuestions) {
    Write-Host "  $($q.displayOrder)) $($q.questionTextAr) [key=$($q.questionKey), allowMultiple=$($q.allowMultiple)]" -ForegroundColor Cyan
    foreach ($opt in $q.options) {
        Write-Host "     - $($opt.optionTextAr) (ID: $($opt.optionId))" -ForegroundColor DarkGray
    }
}

Write-Host "`nOptional:" -ForegroundColor White
foreach ($q in $userQuestions.optionalQuestions) {
    Write-Host "  $($q.displayOrder)) $($q.questionTextAr) [key=$($q.questionKey)]" -ForegroundColor DarkCyan
    foreach ($opt in $q.options) {
        Write-Host "     - $($opt.optionTextAr) (ID: $($opt.optionId))" -ForegroundColor DarkGray
    }
}

# ====================================================================
# الخطوة 7: اليوزر يجاوب على الأسئلة
# ====================================================================
Write-Host "`n--- Step 7: Submit Answers ---" -ForegroundColor Yellow

# سيناريو اليوزر:
# - بيزرع داخل المنزل (indoor)
# - شمس جزئية (partial_sun)
# - أصيص متوسط (medium_pot)
# - ري كل 2-3 أيام (every_2_3)
# - تربة أصص جاهزة (potting_mix)

# نجد IDs الخيارات من الأسئلة اللي جلبناها
$answersList = @()

foreach ($q in $userQuestions.requiredQuestions) {
    switch ($q.questionKey) {
        "site"      { $chosenKey = "indoor" }
        "light"     { $chosenKey = "partial_sun" }
        "container" { $chosenKey = "medium_pot" }
        "water"     { $chosenKey = "every_2_3" }
        "soil"      { $chosenKey = "potting_mix" }
    }
    
    $chosenOption = $q.options | Where-Object { $_.optionKey -eq $chosenKey }
    if ($chosenOption) {
        $answersList += @{
            questionId        = $q.questionId
            selectedOptionIds = @($chosenOption.optionId)
        }
        Write-Host "  $($q.questionTextAr) -> $($chosenOption.optionTextAr)" -ForegroundColor DarkCyan
    }
}

Write-Host "`nSubmitting $($answersList.Count) answers..." -ForegroundColor White

$submitRequest = @{
    answers = $answersList
} | ConvertTo-Json -Depth 5

try {
    $recommendations = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/submit-answers" -Method POST -Headers $userHeaders -Body $submitRequest
    
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "  RECOMMENDATIONS!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Session: $($recommendations.sessionId)" -ForegroundColor DarkGray
    Write-Host "Total: $($recommendations.totalRecommendations)" -ForegroundColor White
    Write-Host "$($recommendations.summaryAr)" -ForegroundColor White
    
    Write-Host "`nConditions:" -ForegroundColor DarkCyan
    foreach ($c in $recommendations.userConditionsSummary) {
        Write-Host "  - $c" -ForegroundColor DarkGray
    }
    
    Write-Host "`nPlants:" -ForegroundColor White
    foreach ($rec in $recommendations.recommendations) {
        $bar = "=" * [math]::Floor($rec.matchPercentage / 5)
        $color = if ($rec.matchPercentage -ge 80) { "Green" } elseif ($rec.matchPercentage -ge 60) { "Yellow" } else { "Red" }
        
        Write-Host "`n  $($rec.nameAr) ($($rec.nameScientific))" -ForegroundColor White
        Write-Host "  [$bar] $($rec.matchPercentage)% - $($rec.matchLevelAr)" -ForegroundColor $color
        Write-Host "  Needs adjustment: $($rec.needsAdjustment)" -ForegroundColor DarkGray
        if ($rec.quickTagsAr) {
            Write-Host "  Tags: $($rec.quickTagsAr -join ' | ')" -ForegroundColor DarkGray
        }
        if ($rec.conditionsSummaryAr) {
            Write-Host "  $($rec.conditionsSummaryAr)" -ForegroundColor DarkGray
        }
    }
    
    $SESSION_ID = $recommendations.sessionId
    
} catch {
    Write-Host "Submit failed: $($_.Exception.Message)" -ForegroundColor Red
    $errorStream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($errorStream)
    $errorBody = $reader.ReadToEnd()
    Write-Host "Error body: $errorBody" -ForegroundColor Red
    exit
}

# ====================================================================
# الخطوة 8: تفاصيل أفضل اقتراح (الأول بالقائمة)
# ====================================================================
Write-Host "`n--- Step 8: Get Best Recommendation Details ---" -ForegroundColor Yellow

$bestPlantId = $recommendations.recommendations[0].plantId
$bestPlantName = $recommendations.recommendations[0].nameAr

Write-Host "Getting details for: $bestPlantName (ID: $bestPlantId)" -ForegroundColor White

try {
    $detail = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/recommendations/$SESSION_ID/plant/$bestPlantId" -Method GET -Headers $userHeaders
    
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  $($detail.nameAr) - $($detail.nameScientific)" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Match: $($detail.matchPercentage)% ($($detail.matchLevelAr))" -ForegroundColor Green
    Write-Host "Category: $($detail.category)" -ForegroundColor DarkGray
    Write-Host "Difficulty: $($detail.difficultyLevel)" -ForegroundColor DarkGray
    
    Write-Host "`nWhy we recommended:" -ForegroundColor White
    foreach ($r in $detail.recommendationReasonsAr) {
        Write-Host "  - $r" -ForegroundColor DarkCyan
    }
    
    Write-Host "`nQuestion Details:" -ForegroundColor White
    foreach ($qd in $detail.questionDetails) {
        $icon = if ($qd.score -ge 80) { "✓" } elseif ($qd.score -ge 60) { "~" } else { "✗" }
        Write-Host "  $icon $($qd.questionTextAr): $($qd.selectedOptionTextAr) -> $($qd.score)/100 ($($qd.scoreLevelAr))" -ForegroundColor DarkGray
        if ($qd.adjustmentTipAr) {
            Write-Host "    Tip: $($qd.adjustmentTipAr)" -ForegroundColor DarkYellow
        }
    }
    
    if ($detail.adjustmentTipsAr -and $detail.adjustmentTipsAr.Count -gt 0) {
        Write-Host "`nAdjustment Tips:" -ForegroundColor Yellow
        foreach ($tip in $detail.adjustmentTipsAr) {
            Write-Host "  - $tip" -ForegroundColor DarkYellow
        }
    }
    
    Write-Host "`nCare Info:" -ForegroundColor White
    Write-Host "  Light: $($detail.careInfo.lightInfoAr)" -ForegroundColor DarkGray
    Write-Host "  Soil: $($detail.careInfo.soilInfoAr)" -ForegroundColor DarkGray
    Write-Host "  Water: $($detail.careInfo.wateringInfoAr) (every $($detail.careInfo.wateringIntervalDays) days)" -ForegroundColor DarkGray
    Write-Host "  Care: $($detail.careInfo.careInfoAr)" -ForegroundColor DarkGray
    
} catch {
    Write-Host "Detail failed: $($_.Exception.Message)" -ForegroundColor Red
}

# ====================================================================
# الخطوة 9: اليوزر يختار النبتة
# ====================================================================
Write-Host "`n--- Step 9: Select Plant ---" -ForegroundColor Yellow

$selectReq = @{
    sessionId = $SESSION_ID
    plantId   = $bestPlantId
    nickname  = "نعناعتي الحلوة"
    notes     = "أول نبتة!"
} | ConvertTo-Json

try {
    $selectRes = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/select" -Method POST -Headers $userHeaders -Body $selectReq
    
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "  PLANT SELECTED!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "$($selectRes.messageAr)" -ForegroundColor Green
    Write-Host "User Plant ID: $($selectRes.userPlantId)" -ForegroundColor White
    Write-Host "Plant: $($selectRes.plantNameAr) ($($selectRes.plantNameScientific))" -ForegroundColor White
    Write-Host "Nickname: $($selectRes.nickname)" -ForegroundColor DarkCyan
    Write-Host "Status: $($selectRes.status)" -ForegroundColor DarkCyan
    Write-Host "Planned Date: $($selectRes.plannedDate)" -ForegroundColor DarkCyan
    
} catch {
    Write-Host "Select failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  TEST COMPLETE!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host @"

Expected Results for this scenario (Indoor, Partial Sun, Medium Pot, Every 2-3 days, Potting Mix):

  1. Mint (mint):
     - site:indoor=90, light:partial_sun=100, container:medium_pot=100, water:every_2_3=100, soil:potting_mix=100
     - Total: 490/500 = 98%   -> "suitable" (excellent match!)

  2. Chamomile:
     - site:indoor=50, light:partial_sun=90, container:medium_pot=85, water:every_2_3=90, soil:potting_mix=85
     - Total: 400/500 = 80%   -> "suitable"

  3. Thyme:
     - site:indoor=40, light:partial_sun=70, container:medium_pot=90, water:every_2_3=70, soil:potting_mix=80
     - Total: 350/500 = 70%   -> "suitable with adjustment" (needs more sun, less water)

"@ -ForegroundColor DarkGray

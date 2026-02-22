# =============================================================================
# Plant Recommendation Test - Correct Version
# نسخة صحيحة 100% لاختبار نظام التوصية
# =============================================================================

$baseUrl = "http://localhost:8081"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "  Plant Recommendation - CORRECT TEST" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""

# =============================================================================
# STEP 1: Admin Login & Setup 
# =============================================================================
Write-Host "--- Step 1: Admin Login ---" -ForegroundColor Yellow

$adminLoginData = @{
    email = "admin@localhost"
    password = "admin123"
} | ConvertTo-Json

try {
    $adminResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $adminLoginData -ContentType "application/json"
    $adminToken = $adminResponse.accessToken
    Write-Host "✅ Admin logged in successfully!" -ForegroundColor Green
} catch {
    Write-Host "❌ Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# =============================================================================
# STEP 2: User Registration & Login
# =============================================================================
Write-Host "--- Step 2: User Setup ---" -ForegroundColor Yellow

$userRegData = @{
    firstName = "Test"
    lastName = "User" 
    email = "testuser@example.com"
    password = "password123"
    phoneNumber = "1234567890"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -Body $userRegData -ContentType "application/json" | Out-Null
    Write-Host "✅ User registered!" -ForegroundColor Green
} catch {
    Write-Host "⚠️  User might already exist, continuing..." -ForegroundColor Yellow
}

$userLoginData = @{
    email = "testuser@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $userResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $userLoginData -ContentType "application/json"
    $userToken = $userResponse.accessToken
    Write-Host "✅ User logged in!" -ForegroundColor Green
} catch {
    Write-Host "❌ User login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# =============================================================================
# STEP 3: GET Questions (to see actual IDs)
# =============================================================================
Write-Host "--- Step 3: GET Questions ---" -ForegroundColor Yellow

$headers = @{"Authorization" = "Bearer $userToken"}

try {
    $questionsResponse = Invoke-RestMethod -Uri "$baseUrl/api/user/plant-recommendation/questions" -Headers $headers
    Write-Host "✅ Questions received!" -ForegroundColor Green
    Write-Host "Required: $($questionsResponse.totalRequired), Optional: $($questionsResponse.totalOptional)" -ForegroundColor Cyan
    
    # عرض الأسئلة الإجبارية مع الخيارات
    Write-Host "`n=== REQUIRED QUESTIONS ===" -ForegroundColor Magenta
    foreach ($q in $questionsResponse.requiredQuestions) {
        Write-Host "📋 Question ID: $($q.questionId)" -ForegroundColor Green
        Write-Host "   AR: $($q.questionTextAr)" -ForegroundColor Cyan
        Write-Host "   EN: $($q.questionTextEn)" -ForegroundColor Cyan
        Write-Host "   Options:" -ForegroundColor Yellow
        foreach ($opt in $q.options) {
            Write-Host "     [$($opt.optionId)] $($opt.optionTextAr)" -ForegroundColor White
        }
        Write-Host ""
    }
} catch {
    Write-Host "❌ Failed to get questions: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# =============================================================================
# STEP 4: Submit Answers (with REAL IDs)
# =============================================================================
Write-Host "--- Step 4: Submit Answers ---" -ForegroundColor Yellow

# بناء الإجابات بناءً على الأسئلة الموجودة فعلياً
$answers = @()

foreach ($question in $questionsResponse.requiredQuestions) {
    if ($question.options.Count -gt 0) {
        # اختيار أول خيار متاح لكل سؤال إجباري
        $selectedOption = $question.options[0].optionId
        $answers += @{
            questionId = $question.questionId
            selectedOptionIds = @($selectedOption)
        }
        Write-Host "📝 Question $($question.questionId): Selected option $selectedOption" -ForegroundColor White
    }
}

$submitData = @{
    answers = $answers
} | ConvertTo-Json -Depth 3

Write-Host "`n🔥 Submitting answers..." -ForegroundColor Yellow
Write-Host "Request Body:" -ForegroundColor Gray
Write-Host $submitData -ForegroundColor DarkGray

try {
    $recommendationsResponse = Invoke-RestMethod -Uri "$baseUrl/api/user/plant-recommendation/submit-answers" -Method POST -Body $submitData -Headers $headers -ContentType "application/json"
    
    Write-Host "`n========================================" -ForegroundColor Blue
    Write-Host "  SUCCESS! RECOMMENDATIONS RECEIVED!" -ForegroundColor Blue
    Write-Host "========================================" -ForegroundColor Blue
    
    $sessionId = $recommendationsResponse.sessionId
    Write-Host "Session ID: $sessionId" -ForegroundColor Green
    Write-Host "Total recommendations: $($recommendationsResponse.recommendations.Count)" -ForegroundColor Green
    
    foreach ($rec in $recommendationsResponse.recommendations) {
        Write-Host "`nTop recommendation:" -ForegroundColor Yellow
        Write-Host "  Plant: $($rec.plantNameEn) ($($rec.scientificName))" -ForegroundColor Cyan
        Write-Host "  Match: $($rec.matchPercentage)% ($($rec.recommendationLevel))" -ForegroundColor Cyan
        Write-Host "  Needs adjustment: $($rec.needsAdjustment)" -ForegroundColor Cyan
    }
    
} catch {
    Write-Host "`n❌ Failed to submit answers!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    
    # عرض تفاصيل الخطأ
    if ($_.Exception.Response) {
        try {
            $errorDetails = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorDetails)
            $errorText = $reader.ReadToEnd()
            Write-Host "Server Response: $errorText" -ForegroundColor Red
        } catch {
            Write-Host "Could not read server response details" -ForegroundColor Red
        }
    }
    exit
}

# =============================================================================
# STEP 5: Get Recommendation Details
# =============================================================================
if ($recommendationsResponse.recommendations.Count -gt 0) {
    Write-Host "`n--- Step 5: Get Recommendation Details ---" -ForegroundColor Yellow
    
    $firstRec = $recommendationsResponse.recommendations[0]
    $plantId = $firstRec.plantId
    
    try {
        $detailResponse = Invoke-RestMethod -Uri "$baseUrl/api/user/plant-recommendation/recommendations/$sessionId/plant/$plantId" -Headers $headers
        Write-Host "✅ Details received for: $($detailResponse.plantNameEn)" -ForegroundColor Green
        Write-Host "Match: $($detailResponse.matchPercentage)% ($($detailResponse.matchLevelAr))" -ForegroundColor Cyan
        
        # =============================================================================
        # STEP 6: Select Plant
        # =============================================================================
        Write-Host "`n--- Step 6: Select Plant ---" -ForegroundColor Yellow
        
        $selectData = @{
            sessionId = $sessionId
            plantId = $plantId
            plannedPlantingDate = "2026-02-22"
        } | ConvertTo-Json
        
        $selectResponse = Invoke-RestMethod -Uri "$baseUrl/api/user/plant-recommendation/select" -Method POST -Body $selectData -Headers $headers -ContentType "application/json"
        
        Write-Host "`n========================================" -ForegroundColor Blue
        Write-Host "  PLANT SELECTED SUCCESS!" -ForegroundColor Blue  
        Write-Host "========================================" -ForegroundColor Blue
        Write-Host "$($selectResponse.plantNameEn) has been added to your crops! 🌱" -ForegroundColor Green
        Write-Host "User Plant ID: $($selectResponse.userPlantId)" -ForegroundColor Green
        Write-Host "Status: $($selectResponse.status)" -ForegroundColor Green
        Write-Host "Planned Date: $($selectResponse.plannedPlantingDate)" -ForegroundColor Green
        
    } catch {
        Write-Host "❌ Failed in details/select: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n========================================" -ForegroundColor Blue
Write-Host "  TEST COMPLETE!" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
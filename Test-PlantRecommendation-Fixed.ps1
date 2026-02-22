# ===============================================
# FIXED Plant Recommendation Test 
# ===============================================

$BASE = "http://localhost:8081"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "  FIXED PLANT RECOMMENDATION TEST" -ForegroundColor Blue  
Write-Host "========================================" -ForegroundColor Blue

# Step 1: User Login
Write-Host "`n--- Step 1: User Login ---" -ForegroundColor Yellow

$userLoginData = @{
    email = "testuser@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $userResponse = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -Body $userLoginData -ContentType "application/json"
    $userToken = $userResponse.accessToken
    Write-Host "✅ User logged in successfully!" -ForegroundColor Green
} catch {
    Write-Host "❌ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

$headers = @{"Authorization" = "Bearer $userToken"}

# Step 2: Get Questions
Write-Host "`n--- Step 2: Get Questions ---" -ForegroundColor Yellow
try {
    $questionsResponse = Invoke-RestMethod -Uri "$BASE/api/user/plant-recommendation/questions" -Headers $headers
    Write-Host "✅ Questions received!" -ForegroundColor Green
    Write-Host "Required: $($questionsResponse.totalRequired)" -ForegroundColor Cyan
    
    # عرض الأسئلة
    Write-Host "`n=== QUESTIONS TO ANSWER ===" -ForegroundColor Magenta
    foreach ($q in $questionsResponse.requiredQuestions) {
        Write-Host "Q$($q.questionId): $($q.questionTextAr)" -ForegroundColor Green
        foreach ($opt in $q.options) {
            Write-Host "   [$($opt.optionId)] $($opt.optionTextAr)" -ForegroundColor White
        }
    }
} catch {
    Write-Host "❌ Failed to get questions: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Step 3: Submit CORRECT Answers (ONE answer per question)
Write-Host "`n--- Step 3: Submit Correct Answers ---" -ForegroundColor Yellow

# بناء إجابة صحيحة - خيار واحد فقط لكل سؤال
$correctAnswers = @()

# لكل سؤال إجباري، اختار أول خيار متاح
foreach ($question in $questionsResponse.requiredQuestions) {
    if ($question.options.Count -gt 0) {
        $selectedOption = $question.options[0].optionId  # أول خيار
        $correctAnswers += @{
            questionId = $question.questionId
            selectedOptionIds = @($selectedOption)  # خيار واحد فقط
        }
        Write-Host "✅ Q$($question.questionId) → Option $selectedOption" -ForegroundColor Green
    }
}

$submitData = @{
    answers = $correctAnswers  
} | ConvertTo-Json -Depth 3

Write-Host "`n🚀 Submitting FIXED answers..." -ForegroundColor Yellow
try {
    $recommendationsResponse = Invoke-RestMethod -Uri "$BASE/api/user/plant-recommendation/submit-answers" -Method POST -Body $submitData -Headers $headers -ContentType "application/json"
    
    Write-Host "`n========================================" -ForegroundColor Blue
    Write-Host "  RESULT:" -ForegroundColor Blue
    Write-Host "========================================" -ForegroundColor Blue
    
    Write-Host "Session ID: $($recommendationsResponse.sessionId)" -ForegroundColor Green
    Write-Host "Total Recommendations: $($recommendationsResponse.totalRecommendations)" -ForegroundColor Green
    
    if ($recommendationsResponse.totalRecommendations -eq 0) {
        Write-Host "`n❌ STILL NO RECOMMENDATIONS!" -ForegroundColor Red
        Write-Host "Reason: Missing Plant Suitability data in database" -ForegroundColor Yellow
        Write-Host "Solution: Admin must add plants and suitability scores first" -ForegroundColor Yellow
    } else {
        Write-Host "`n✅ SUCCESS! Got recommendations:" -ForegroundColor Green
        foreach ($rec in $recommendationsResponse.recommendations) {
            Write-Host "  - $($rec.plantNameEn): $($rec.matchPercentage)%" -ForegroundColor Cyan
        }
    }
    
} catch {
    Write-Host "`n❌ Still failed: $($_.Exception.Message)" -ForegroundColor Red
    
    # تفاصيل الخطأ 
    if ($_.Exception.Response) {
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorDetails = $reader.ReadToEnd()
            Write-Host "Server Error: $errorDetails" -ForegroundColor DarkRed
        } catch {}
    }
}

Write-Host "`n========================================" -ForegroundColor Blue
Write-Host "  TEST COMPLETE" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
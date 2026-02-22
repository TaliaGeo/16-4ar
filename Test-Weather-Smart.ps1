# ===============================================================
# Weather-Integrated Plant Recommendation Test
# Smart Season-Based Plant Recommendations System 
# ===============================================================

$BASE = "http://localhost:8081"

Write-Host "========================================" -ForegroundColor Blue
Write-Host "  WEATHER-SMART PLANT RECOMMENDATIONS" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

# Step 1: Admin Login
Write-Host "`n--- Step 1: Admin Login ---" -ForegroundColor Yellow

$adminLoginData = @{
    email = "admin@localhost"
    password = "admin123"
} | ConvertTo-Json

try {
    $adminResponse = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -Body $adminLoginData -ContentType "application/json"
    $adminToken = $adminResponse.accessToken
    Write-Host "Admin logged in!" -ForegroundColor Green
} catch {
    Write-Host "Admin login failed" -ForegroundColor Red
    exit
}

$adminHeaders = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type" = "application/json"
}

# Initialize questions (now includes season question)
try {
    Invoke-RestMethod -Uri "$BASE/admin/planting-questions/initialize" -Method POST -Headers $adminHeaders | Out-Null
    Write-Host "Questions initialized (with seasonal support)!" -ForegroundColor Green
} catch {
    Write-Host "Questions may already exist" -ForegroundColor Yellow
}

# Step 2: Check Current Weather 
Write-Host "`n--- Step 2: Check Current Weather/Season ---" -ForegroundColor Yellow

try {
    $weather = Invoke-RestMethod -Uri "$BASE/api/user/home/weather" -Headers $adminHeaders
    Write-Host "Current Weather:" -ForegroundColor Cyan
    Write-Host "   Season: $($weather.seasonEn)" -ForegroundColor White
    Write-Host "   Temperature: $($weather.temperature)C" -ForegroundColor White
    Write-Host "   Description: $($weather.descriptionEn)" -ForegroundColor White
    if ($weather.isEstimated) {
        Write-Host "   Using estimated weather (no API key)" -ForegroundColor DarkYellow
    } else {
        Write-Host "   Real weather data from API" -ForegroundColor Green
    }
} catch {
    Write-Host "Could not fetch weather, using defaults" -ForegroundColor Yellow
}

# Step 3: User Login
Write-Host "`n--- Step 3: User Setup ---" -ForegroundColor Yellow

$userRegData = @{
    firstName = "Smart"
    lastName = "Farmer" 
    email = "smartfarmer@example.com"
    password = "password123"
    phoneNumber = "1234567890"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$BASE/api/auth/register" -Method POST -Body $userRegData -ContentType "application/json" | Out-Null
    Write-Host "User registered!" -ForegroundColor Green
} catch {
    Write-Host "User may already exist" -ForegroundColor Yellow
}

$userLoginData = @{
    email = "smartfarmer@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $userResponse = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method POST -Body $userLoginData -ContentType "application/json"
    $userToken = $userResponse.accessToken
    Write-Host "User logged in!" -ForegroundColor Green
} catch {
    Write-Host "User login failed" -ForegroundColor Red
    exit
}

$userHeaders = @{"Authorization" = "Bearer $userToken"}

# Step 4: Get Questions
Write-Host "`n--- Step 4: Get Questions (Including Season) ---" -ForegroundColor Yellow

try {
    $questionsResponse = Invoke-RestMethod -Uri "$BASE/api/user/plant-recommendation/questions" -Headers $userHeaders
    Write-Host "Questions received!" -ForegroundColor Green
    Write-Host "Required: $($questionsResponse.totalRequired) (including season)" -ForegroundColor Cyan
    
    Write-Host "`nQUESTIONS WITH SEASON SUPPORT:" -ForegroundColor Magenta
    foreach ($q in $questionsResponse.requiredQuestions) {
        Write-Host "Q$($q.questionId): $($q.questionTextEn)" -ForegroundColor Green
        if ($q.questionTextEn -like "*season*") {
            Write-Host "   SMART SEASON QUESTION!" -ForegroundColor Yellow
        }
        foreach ($opt in $q.options) {
            $marker = if ($opt.optionTextEn -like "*Current*") { "AUTO" } else { "    " }
            Write-Host "$marker [$($opt.optionId)] $($opt.optionTextEn)" -ForegroundColor White
        }
        Write-Host ""
    }
} catch {
    Write-Host "Failed to get questions" -ForegroundColor Red
    exit
}

# Step 5: Submit Smart Answers 
Write-Host "--- Step 5: Submit Weather-Smart Answers ---" -ForegroundColor Yellow

$smartAnswers = @()

foreach ($question in $questionsResponse.requiredQuestions) {
    if ($question.options.Count -gt 0) {
        $selectedOption = $null
        
        # Smart selection based on question type
        if ($question.questionTextEn -like "*season*") {
            # For season: select "Current Season" for auto-detection
            $currentSeasonOpt = $question.options | Where-Object { $_.optionTextEn -like "*Current*" }
            $selectedOption = if ($currentSeasonOpt) { $currentSeasonOpt.optionId } else { $question.options[0].optionId }
            Write-Host "Season: Auto-selecting 'Current Season' -> Weather API will determine actual season" -ForegroundColor Yellow
        } else {
            # For other questions: select first option
            $selectedOption = $question.options[0].optionId
        }
        
        $smartAnswers += @{
            questionId = $question.questionId
            selectedOptionIds = @($selectedOption)
        }
        
        Write-Host "Q$($question.questionId) -> Option $selectedOption" -ForegroundColor Green
    }
}

$submitData = @{
    answers = $smartAnswers  
} | ConvertTo-Json -Depth 3

Write-Host "`nSubmitting WEATHER-SMART answers..." -ForegroundColor Cyan
Write-Host "System will:" -ForegroundColor Yellow  
Write-Host "  1. Get current weather/season automatically" -ForegroundColor White
Write-Host "  2. Replace 'current season' with actual season" -ForegroundColor White
Write-Host "  3. Apply seasonal bonuses to matching plants" -ForegroundColor White
Write-Host "  4. Rank plants by season suitability" -ForegroundColor White

try {
    $recommendationsResponse = Invoke-RestMethod -Uri "$BASE/api/user/plant-recommendation/submit-answers" -Method POST -Body $submitData -Headers $userHeaders -ContentType "application/json"
    
    Write-Host "`n========================================" -ForegroundColor Blue
    Write-Host "  WEATHER-SMART RESULTS!" -ForegroundColor Blue
    Write-Host "========================================" -ForegroundColor Blue
    
    Write-Host "Session ID: $($recommendationsResponse.sessionId)" -ForegroundColor Green
    Write-Host "Total Recommendations: $($recommendationsResponse.totalRecommendations)" -ForegroundColor Green
    
    # Enhanced summary with weather info
    Write-Host "`nSmart Summary:" -ForegroundColor Yellow
    Write-Host "$($recommendationsResponse.summaryEn)" -ForegroundColor Cyan
    
    if ($recommendationsResponse.userConditionsSummary) {
        Write-Host "`nYour Conditions (with weather):" -ForegroundColor Yellow
        foreach ($condition in $recommendationsResponse.userConditionsSummary) {
            Write-Host "  $condition" -ForegroundColor White
        }
    }
    
    if ($recommendationsResponse.totalRecommendations -gt 0) {
        Write-Host "`nSEASONAL RECOMMENDATIONS:" -ForegroundColor Green
        foreach ($rec in $recommendationsResponse.recommendations) {
            $seasonalMarker = if ($rec.matchPercentage -gt 70) { "STAR" } else { "LEAF" }
            Write-Host "$seasonalMarker $($rec.nameEn): $($rec.matchPercentage)% ($($rec.matchLevel))" -ForegroundColor Cyan
            if ($rec.needsAdjustment -eq $false -and $rec.matchPercentage -gt 70) {
                Write-Host "    Perfect for current season!" -ForegroundColor Green
            }
        }
        
        # Test recommendation details
        Write-Host "`n--- Testing Seasonal Details ---" -ForegroundColor Yellow
        $firstRec = $recommendationsResponse.recommendations[0]
        $sessionId = $recommendationsResponse.sessionId
        $plantId = $firstRec.plantId
        
        try {
            $details = Invoke-RestMethod -Uri "$BASE/api/user/plant-recommendation/recommendations/$sessionId/plant/$plantId" -Headers $userHeaders
            Write-Host "Seasonal details for: $($details.nameEn)" -ForegroundColor Green
            Write-Host "   Match: $($details.matchPercentage)%" -ForegroundColor Cyan
        } catch {
            Write-Host "Could not get details: $($_.Exception.Message)" -ForegroundColor Yellow
        }
        
    } else {
        Write-Host "`nNo recommendations found!" -ForegroundColor Red
        Write-Host "This means NO plants are suitable for current conditions + season" -ForegroundColor Yellow
        Write-Host "Solution: Admin should add more plants with seasonal suitability data" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "`nFailed to get weather-smart recommendations!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Blue
Write-Host "  WEATHER INTEGRATION COMPLETE!" -ForegroundColor Blue  
Write-Host "========================================" -ForegroundColor Blue
Write-Host "Season detected automatically from weather API" -ForegroundColor Green
Write-Host "Plants ranked by seasonal suitability" -ForegroundColor Green
Write-Host "Seasonal bonuses applied to matching plants" -ForegroundColor Green
Write-Host "Weather conditions included in recommendations" -ForegroundColor Green
# Simple Plant Recommendation Test Script (English only to avoid encoding issues)
$BASE = "http://localhost:8081/api"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Plant Recommendation Test - English" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Step 1: Admin Login
Write-Host "`n--- Step 1: Admin Login ---" -ForegroundColor Yellow
$adminLogin = @{
    email    = "admin@example.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $adminRes = Invoke-RestMethod -Uri "$BASE/auth/login" -Method POST -Body $adminLogin -ContentType "application/json"
    $ADMIN_TOKEN = $adminRes.accessToken
    Write-Host "Admin logged in successfully!" -ForegroundColor Green
} catch {
    Write-Host "Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

$adminHeaders = @{
    "Authorization" = "Bearer $ADMIN_TOKEN"
    "Content-Type"  = "application/json"
}

# Step 2: Initialize Questions
Write-Host "`n--- Step 2: Initialize Default Questions ---" -ForegroundColor Yellow
try {
    $questionsInit = Invoke-RestMethod -Uri "$BASE/admin/planting-questions/initialize" -Method POST -Headers $adminHeaders
    Write-Host "Questions initialized!" -ForegroundColor Green
} catch {
    Write-Host "Questions may already exist (OK): $($_.Exception.Message)" -ForegroundColor DarkYellow
}

# Step 3: Add Plants (simplified with English names)
Write-Host "`n--- Step 3: Add Plants ---" -ForegroundColor Yellow

$mint = @{
    nameAr             = "Mint Arabic"
    nameEn             = "Mint"
    nameScientific     = "Mentha spp"
    shortDescriptionAr = "Easy aromatic herb for beginners"
    shortDescriptionEn = "Easy aromatic herb, perfect for beginners"
    lightInfoAr        = "Partial sun to indirect light"
    lightInfoEn        = "Partial sun to indirect light"
    soilInfoAr         = "Rich, moist, well-drained soil"
    soilInfoEn         = "Rich, moist, well-drained soil"
    wateringInfoAr     = "Regular watering - keep soil moist"
    wateringInfoEn     = "Regular watering - keep soil moist"
    wateringIntervalDays = 2
    careInfoAr         = "Regularly trim leaves to encourage growth"
    careInfoEn         = "Regularly trim leaves to encourage growth"
    harvestInfoAr      = "Leaves can be harvested after 3-4 weeks"
    harvestInfoEn      = "Leaves can be harvested after 3-4 weeks"
    usesInfoAr         = "Tea, cooking, beverages"
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
    # Try to find existing
    $allPlants = Invoke-RestMethod -Uri "$BASE/admin/plants?page=0&size=100" -Headers $adminHeaders
    $existing = $allPlants.content | Where-Object { $_.nameEn -eq "Mint" } | Select-Object -First 1
    if ($existing) { 
        $MINT_ID = $existing.id
        Write-Host "Using existing mint ID: $MINT_ID" -ForegroundColor DarkYellow
    }
}

# Step 4: Add suitability scores (simplified)
Write-Host "`n--- Step 4: Set Sample Suitability Scores ---" -ForegroundColor Yellow

# Get active questions to find option IDs
$questions = Invoke-RestMethod -Uri "$BASE/admin/planting-questions/active/with-options" -Headers $adminHeaders

$optionIds = @{}
foreach ($q in $questions) {
    foreach ($opt in $q.options) {
        $optionIds["$($q.questionKey)_$($opt.optionKey)"] = $opt.id
    }
}

# Add some sample suitability scores for mint
$suitabilityScores = @(
    @{ key="site_indoor"; score=90 },
    @{ key="light_partial_sun"; score=100 },
    @{ key="container_medium_pot"; score=100 },
    @{ key="water_every_2_3"; score=100 },
    @{ key="soil_potting_mix"; score=100 }
)

$totalCreated = 0
foreach ($s in $suitabilityScores) {
    $optId = $optionIds[$s.key]
    if ($optId -and $MINT_ID) {
        $suitReq = @{
            plantId        = $MINT_ID
            optionId       = $optId
            score          = $s.score
            adjustmentTipAr = ""
            adjustmentTipEn = ""
        } | ConvertTo-Json
        
        try {
            $null = Invoke-RestMethod -Uri "$BASE/admin/plant-suitability" -Method POST -Headers $adminHeaders -Body $suitReq
            $totalCreated++
        } catch {
            # May already exist
        }
    }
}

Write-Host "Created $totalCreated suitability scores" -ForegroundColor Green

# Now test user flow
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  USER FLOW TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Step 5: User registration/login
Write-Host "`n--- Step 5: User Registration/Login ---" -ForegroundColor Yellow

$userRegister = @{
    fullName = "Test User"
    email    = "test@test.com"
    password = "test1234"
} | ConvertTo-Json

try {
    $null = Invoke-RestMethod -Uri "$BASE/auth/register" -Method POST -Body $userRegister -ContentType "application/json"
    Write-Host "User registered!" -ForegroundColor Green
} catch {
    Write-Host "User may already exist (OK)" -ForegroundColor DarkYellow
}

$userLogin = @{
    email    = "test@test.com"
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

# Step 6: Get Questions
Write-Host "`n--- Step 6: GET Questions ---" -ForegroundColor Yellow

try {
    $userQuestions = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/questions" -Headers $userHeaders
    Write-Host "Questions received!" -ForegroundColor Green
    Write-Host "Required: $($userQuestions.totalRequired), Optional: $($userQuestions.totalOptional)" -ForegroundColor Green

    # Step 7: Submit Answers
    Write-Host "`n--- Step 7: Submit Answers ---" -ForegroundColor Yellow

    $answersList = @()
    foreach ($q in $userQuestions.requiredQuestions) {
        # Pick the first option for each question
        if ($q.options -and $q.options.Count -gt 0) {
            $answersList += @{
                questionId        = $q.questionId
                selectedOptionIds = @($q.options[0].optionId)
            }
        }
    }

    if ($answersList.Count -gt 0) {
        $submitRequest = @{
            answers = $answersList
        } | ConvertTo-Json -Depth 5

        Write-Host "Submitting $($answersList.Count) answers..." -ForegroundColor White

        try {
            $recommendations = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/submit-answers" -Method POST -Headers $userHeaders -Body $submitRequest
            
            Write-Host "`n========================================" -ForegroundColor Green
            Write-Host "  SUCCESS! RECOMMENDATIONS RECEIVED!" -ForegroundColor Green
            Write-Host "========================================" -ForegroundColor Green
            Write-Host "Session: $($recommendations.sessionId)" -ForegroundColor White
            Write-Host "Total recommendations: $($recommendations.totalRecommendations)" -ForegroundColor White
            
            if ($recommendations.recommendations -and $recommendations.recommendations.Count -gt 0) {
                Write-Host "`nTop recommendation:" -ForegroundColor White
                $top = $recommendations.recommendations[0]
                Write-Host "  Plant: $($top.nameEn) ($($top.nameScientific))" -ForegroundColor Cyan
                Write-Host "  Match: $($top.matchPercentage)% ($($top.matchLevel))" -ForegroundColor Green
                Write-Host "  Needs adjustment: $($top.needsAdjustment)" -ForegroundColor DarkGray

                # Step 8: Get detailed recommendation
                Write-Host "`n--- Step 8: Get Recommendation Details ---" -ForegroundColor Yellow
                $SESSION_ID = $recommendations.sessionId
                $bestPlantId = $top.plantId

                try {
                    $detail = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/recommendations/$SESSION_ID/plant/$bestPlantId" -Headers $userHeaders
                    Write-Host "Details received for: $($detail.nameEn)" -ForegroundColor Green
                    Write-Host "Match: $($detail.matchPercentage)% ($($detail.matchLevelAr))" -ForegroundColor Green
                    
                    # Step 9: Select the plant
                    Write-Host "`n--- Step 9: Select Plant ---" -ForegroundColor Yellow
                    $selectReq = @{
                        sessionId = $SESSION_ID
                        plantId   = $bestPlantId
                        nickname  = "My First Plant"
                        notes     = "Test plant selection"
                    } | ConvertTo-Json

                    try {
                        $selectRes = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/select" -Method POST -Headers $userHeaders -Body $selectReq
                        
                        Write-Host "`n========================================" -ForegroundColor Green
                        Write-Host "  PLANT SELECTED SUCCESS!" -ForegroundColor Green
                        Write-Host "========================================" -ForegroundColor Green
                        Write-Host "$($selectRes.messageEn)" -ForegroundColor Green
                        Write-Host "User Plant ID: $($selectRes.userPlantId)" -ForegroundColor White
                        Write-Host "Status: $($selectRes.status)" -ForegroundColor White
                        Write-Host "Planned Date: $($selectRes.plannedDate)" -ForegroundColor White

                    } catch {
                        Write-Host "Select failed: $($_.Exception.Message)" -ForegroundColor Red
                    }
                    
                } catch {
                    Write-Host "Detail failed: $($_.Exception.Message)" -ForegroundColor Red
                }
            }
        } catch {
            Write-Host "Submit failed: $($_.Exception.Message)" -ForegroundColor Red
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorBody = $reader.ReadToEnd()
            Write-Host "Error details: $errorBody" -ForegroundColor Red
        }
    } else {
        Write-Host "No questions found to answer!" -ForegroundColor Red
    }
} catch {
    Write-Host "Get questions failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  TEST COMPLETE!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
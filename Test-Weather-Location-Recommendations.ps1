# ============================================================
#  Weather + Location Smart Recommendations - Full E2E Test
#  Tests the GPS/Season/Climate-Zone integration
# ============================================================

$BASE = "http://localhost:8081/api"

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  WEATHER + LOCATION SMART RECOMMENDATIONS TEST" -ForegroundColor Cyan
Write-Host "  Testing: GPS, Season, Climate Zone, Temperature scoring" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

# ─────────────────────────────────────────────
# Helper function: pretty print JSON
# ─────────────────────────────────────────────
function Show-Json($obj, $label) {
    Write-Host "`n--- $label ---" -ForegroundColor Magenta
    $obj | ConvertTo-Json -Depth 6 | Write-Host -ForegroundColor Gray
}

# ═══════════════════════════════════════════════
# PART 1: SETUP (Admin)
# ═══════════════════════════════════════════════
Write-Host "`n[PART 1] ADMIN SETUP" -ForegroundColor Yellow
Write-Host "─────────────────────" -ForegroundColor Yellow

# 1.1 Admin Login
Write-Host "`n[1.1] Admin login..." -ForegroundColor White
try {
    $adminRes = Invoke-RestMethod -Uri "$BASE/auth/login" -Method POST `
        -Body (@{ email="admin@example.com"; password="admin123" } | ConvertTo-Json) `
        -ContentType "application/json"
    $ADMIN_TOKEN = $adminRes.accessToken
    Write-Host "  OK - Admin logged in" -ForegroundColor Green
} catch {
    Write-Host "  FAIL - $($_.Exception.Message)" -ForegroundColor Red
    exit
}

$adminH = @{ "Authorization"="Bearer $ADMIN_TOKEN"; "Content-Type"="application/json" }

# 1.2 Initialize questions (idempotent)
Write-Host "[1.2] Initialize planting questions..." -ForegroundColor White
try {
    $null = Invoke-RestMethod -Uri "$BASE/admin/planting-questions/initialize" -Method POST -Headers $adminH
    Write-Host "  OK - Questions initialized" -ForegroundColor Green
} catch { Write-Host "  OK - Already initialized" -ForegroundColor DarkYellow }

# 1.3 Create test plants with minTemp/maxTemp
Write-Host "[1.3] Creating test plants with temperature ranges..." -ForegroundColor White

$plants = @(
    @{
        nameAr="نعناع"; nameEn="Mint"; nameScientific="Mentha spicata"
        shortDescriptionAr="عشبة عطرية سهلة"; shortDescriptionEn="Easy aromatic herb"
        lightInfoAr="ضوء جزئي"; lightInfoEn="Partial sun"
        soilInfoAr="تربة رطبة"; soilInfoEn="Moist well-drained soil"
        wateringInfoAr="ري منتظم"; wateringInfoEn="Regular watering"
        wateringIntervalDays=2; careInfoAr="تقليم"; careInfoEn="Trim regularly"
        harvestInfoAr="3 اسابيع"; harvestInfoEn="Harvest after 3 weeks"
        usesInfoAr="شاي"; usesInfoEn="Tea, cooking"
        daysToHarvest=25; germinationDays=10
        minTemp=15; maxTemp=30    # Warm-season herb
        difficultyLevel="EASY"; category="HERBS"
    },
    @{
        nameAr="زعتر"; nameEn="Thyme"; nameScientific="Thymus vulgaris"
        shortDescriptionAr="عشبة جبلية"; shortDescriptionEn="Mountain herb, drought tolerant"
        lightInfoAr="شمس كاملة"; lightInfoEn="Full sun"
        soilInfoAr="تربة جافة"; soilInfoEn="Dry, well-drained soil"
        wateringInfoAr="ري خفيف"; wateringInfoEn="Light watering"
        wateringIntervalDays=5; careInfoAr="لا يحتاج كثير"; careInfoEn="Low maintenance"
        harvestInfoAr="6 اسابيع"; harvestInfoEn="Harvest after 6 weeks"
        usesInfoAr="طبخ"; usesInfoEn="Cooking, tea"
        daysToHarvest=45; germinationDays=14
        minTemp=5; maxTemp=25     # Cool-season, mountain plant
        difficultyLevel="EASY"; category="HERBS"
    },
    @{
        nameAr="طماطم"; nameEn="Tomato"; nameScientific="Solanum lycopersicum"
        shortDescriptionAr="خضار صيفية"; shortDescriptionEn="Summer vegetable"
        lightInfoAr="شمس كاملة"; lightInfoEn="Full sun"
        soilInfoAr="تربة غنية"; soilInfoEn="Rich, well-drained soil"
        wateringInfoAr="ري يومي"; wateringInfoEn="Daily watering in summer"
        wateringIntervalDays=1; careInfoAr="دعم بالعصي"; careInfoEn="Stake the plants"
        harvestInfoAr="60 يوم"; harvestInfoEn="60 days to harvest"
        usesInfoAr="سلطة طبخ"; usesInfoEn="Salads, cooking"
        daysToHarvest=60; germinationDays=7
        minTemp=20; maxTemp=35    # Needs heat
        difficultyLevel="MEDIUM"; category="VEGETABLES"
    },
    @{
        nameAr="ريحان"; nameEn="Basil"; nameScientific="Ocimum basilicum"
        shortDescriptionAr="عشبة عطرية ساحلية"; shortDescriptionEn="Aromatic coastal herb"
        lightInfoAr="شمس كاملة"; lightInfoEn="Full sun to partial shade"
        soilInfoAr="تربة غنية رطبة"; soilInfoEn="Rich moist soil"
        wateringInfoAr="ري منتظم"; wateringInfoEn="Regular watering"
        wateringIntervalDays=2; careInfoAr="قطف الازهار"; careInfoEn="Pinch flowers"
        harvestInfoAr="4 اسابيع"; harvestInfoEn="Harvest after 4 weeks"
        usesInfoAr="بيتزا سلطة"; usesInfoEn="Pizza, salads"
        daysToHarvest=30; germinationDays=7
        minTemp=18; maxTemp=32    # Warm-season
        difficultyLevel="EASY"; category="HERBS"
    }
)

$plantIds = @{}
foreach ($p in $plants) {
    try {
        $res = Invoke-RestMethod -Uri "$BASE/admin/plants" -Method POST -Headers $adminH -Body ($p | ConvertTo-Json)
        $plantIds[$p.nameEn] = $res.id
        Write-Host "  Created: $($p.nameEn) (ID=$($res.id), temp=$($p.minTemp)-$($p.maxTemp)C)" -ForegroundColor Green
    } catch {
        # Find existing
        $all = Invoke-RestMethod -Uri "$BASE/admin/plants?page=0&size=100" -Headers $adminH
        $ex = $all.content | Where-Object { $_.nameEn -eq $p.nameEn } | Select-Object -First 1
        if ($ex) {
            $plantIds[$p.nameEn] = $ex.id
            Write-Host "  Exists: $($p.nameEn) (ID=$($ex.id))" -ForegroundColor DarkYellow
        }
    }
}

# 1.4 Get question options & set suitability scores
Write-Host "`n[1.4] Setting suitability scores for all plants..." -ForegroundColor White
$questions = Invoke-RestMethod -Uri "$BASE/admin/planting-questions/active/with-options" -Headers $adminH

$optionMap = @{}
foreach ($q in $questions) {
    foreach ($opt in $q.options) {
        $optionMap["$($q.questionKey)_$($opt.optionKey)"] = $opt.id
    }
}

# Suitability: each plant gets high scores for indoor/partial-sun/medium-pot/every-2-3-days/potting-mix
$scoreKeys = @("site_indoor","light_partial_sun","container_medium_pot","water_every_2_3","soil_potting_mix")

foreach ($pName in $plantIds.Keys) {
    $pId = $plantIds[$pName]
    $created = 0
    foreach ($sk in $scoreKeys) {
        $oId = $optionMap[$sk]
        if ($oId -and $pId) {
            try {
                $body = @{ plantId=$pId; optionId=$oId; score=90; adjustmentTipAr=""; adjustmentTipEn="" } | ConvertTo-Json
                $null = Invoke-RestMethod -Uri "$BASE/admin/plant-suitability" -Method POST -Headers $adminH -Body $body
                $created++
            } catch {}
        }
    }
    Write-Host "  $pName : $created suitability scores" -ForegroundColor Green
}

# ═══════════════════════════════════════════════
# PART 2: USER FLOW
# ═══════════════════════════════════════════════
Write-Host "`n[PART 2] USER FLOW" -ForegroundColor Yellow
Write-Host "─────────────────────" -ForegroundColor Yellow

# 2.1 Register + Login user
Write-Host "`n[2.1] User register & login..." -ForegroundColor White
try {
    $null = Invoke-RestMethod -Uri "$BASE/auth/register" -Method POST `
        -Body (@{ fullName="Weather Tester"; email="weather-test@test.com"; password="test1234" } | ConvertTo-Json) `
        -ContentType "application/json"
} catch {}

$userRes = Invoke-RestMethod -Uri "$BASE/auth/login" -Method POST `
    -Body (@{ email="weather-test@test.com"; password="test1234" } | ConvertTo-Json) `
    -ContentType "application/json"
$USER_TOKEN = $userRes.accessToken
Write-Host "  OK - Logged in as Weather Tester" -ForegroundColor Green

$userH = @{ "Authorization"="Bearer $USER_TOKEN"; "Content-Type"="application/json" }

# 2.2 Get questions and build answers (pick first option for each required question)
Write-Host "[2.2] Getting planting questions..." -ForegroundColor White
$userQ = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/questions" -Headers $userH
Write-Host "  Required: $($userQ.totalRequired), Optional: $($userQ.totalOptional)" -ForegroundColor Green

$answers = @()
foreach ($q in $userQ.requiredQuestions) {
    if ($q.options -and $q.options.Count -gt 0) {
        $answers += @{ questionId=$q.questionId; selectedOptionIds=@($q.options[0].optionId) }
    }
}

# ════════════════════════════════════════════════════
# TEST A: Submit WITHOUT GPS (should use default Nablus)
# ════════════════════════════════════════════════════
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  TEST A: No GPS - Default Location (Nablus)" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

$submitA = @{ answers = $answers } | ConvertTo-Json -Depth 5

try {
    $recA = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/submit-answers" `
        -Method POST -Headers $userH -Body $submitA

    Write-Host "`n  Session:    $($recA.sessionId)" -ForegroundColor White
    Write-Host "  Total recs: $($recA.totalRecommendations)" -ForegroundColor White

    if ($recA.weatherContext) {
        $wc = $recA.weatherContext
        Write-Host "`n  --- Weather Context ---" -ForegroundColor Magenta
        Write-Host "  Location:     $($wc.locationEn) / $($wc.locationAr)" -ForegroundColor Gray
        Write-Host "  Temperature:  $($wc.temperature) C" -ForegroundColor Gray
        Write-Host "  Season:       $($wc.seasonEn) / $($wc.seasonAr)" -ForegroundColor Gray
        Write-Host "  Climate Zone: $($wc.climateZoneEn) / $($wc.climateZoneAr)" -ForegroundColor Gray
        Write-Host "  GPS Used:     $($wc.locationUsed)" -ForegroundColor Gray
        Write-Host "  Weather:      $($wc.weatherDescriptionEn)" -ForegroundColor Gray

        if ($wc.locationUsed -eq $false) {
            Write-Host "  >> PASS: locationUsed=false (no GPS sent)" -ForegroundColor Green
        } else {
            Write-Host "  >> INFO: locationUsed=true (user has saved location)" -ForegroundColor DarkYellow
        }
    } else {
        Write-Host "  >> FAIL: No weatherContext in response!" -ForegroundColor Red
    }

    Write-Host "`n  --- Recommendations ---" -ForegroundColor Magenta
    foreach ($r in $recA.recommendations) {
        $seasonal = if ($r.seasonalMatch) { " [SEASONAL]" } else { "" }
        Write-Host "  $($r.nameEn): $($r.matchPercentage)% ($($r.matchLevel))$seasonal" -ForegroundColor Cyan
        if ($r.seasonalNoteEn) {
            Write-Host "    Note: $($r.seasonalNoteEn)" -ForegroundColor DarkGray
        }
    }
} catch {
    Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = [System.IO.StreamReader]::new($stream)
        Write-Host "  Body: $($reader.ReadToEnd())" -ForegroundColor Red
    } catch {}
}

# ════════════════════════════════════════════════════
# TEST B: Submit WITH GPS (Jericho - Valley/Hot zone)
# ════════════════════════════════════════════════════
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  TEST B: With GPS - Jericho (Valley, Hot Zone)" -ForegroundColor Cyan
Write-Host "  Lat: 31.8611, Lon: 35.4534" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

$submitB = @{
    answers   = $answers
    latitude  = 31.8611    # Jericho
    longitude = 35.4534
} | ConvertTo-Json -Depth 5

try {
    $recB = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/submit-answers" `
        -Method POST -Headers $userH -Body $submitB

    Write-Host "`n  Session:    $($recB.sessionId)" -ForegroundColor White
    Write-Host "  Total recs: $($recB.totalRecommendations)" -ForegroundColor White

    if ($recB.weatherContext) {
        $wc = $recB.weatherContext
        Write-Host "`n  --- Weather Context ---" -ForegroundColor Magenta
        Write-Host "  Location:     $($wc.locationEn) / $($wc.locationAr)" -ForegroundColor Gray
        Write-Host "  Temperature:  $($wc.temperature) C" -ForegroundColor Gray
        Write-Host "  Season:       $($wc.seasonEn) / $($wc.seasonAr)" -ForegroundColor Gray
        Write-Host "  Climate Zone: $($wc.climateZoneEn) / $($wc.climateZoneAr)" -ForegroundColor Gray
        Write-Host "  GPS Used:     $($wc.locationUsed)" -ForegroundColor Gray

        if ($wc.locationUsed -eq $true) {
            Write-Host "  >> PASS: locationUsed=true (GPS sent)" -ForegroundColor Green
        } else {
            Write-Host "  >> FAIL: locationUsed should be true!" -ForegroundColor Red
        }

        if ($wc.climateZoneEn -eq "valley") {
            Write-Host "  >> PASS: Climate zone = valley (Jericho)" -ForegroundColor Green
        } else {
            Write-Host "  >> INFO: Climate zone = $($wc.climateZoneEn) (expected valley)" -ForegroundColor DarkYellow
        }
    }

    Write-Host "`n  --- Recommendations ---" -ForegroundColor Magenta
    foreach ($r in $recB.recommendations) {
        $seasonal = if ($r.seasonalMatch) { " [SEASONAL]" } else { "" }
        Write-Host "  $($r.nameEn): $($r.matchPercentage)% ($($r.matchLevel))$seasonal" -ForegroundColor Cyan
        if ($r.seasonalNoteEn) {
            Write-Host "    Note: $($r.seasonalNoteEn)" -ForegroundColor DarkGray
        }
    }
} catch {
    Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = [System.IO.StreamReader]::new($stream)
        Write-Host "  Body: $($reader.ReadToEnd())" -ForegroundColor Red
    } catch {}
}

# ════════════════════════════════════════════════════
# TEST C: Submit WITH GPS (Haifa - Coastal Zone)
# ════════════════════════════════════════════════════
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  TEST C: With GPS - Haifa (Coastal Zone)" -ForegroundColor Cyan
Write-Host "  Lat: 32.7940, Lon: 34.9896" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

$submitC = @{
    answers   = $answers
    latitude  = 32.7940     # Haifa
    longitude = 34.9896
} | ConvertTo-Json -Depth 5

try {
    $recC = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/submit-answers" `
        -Method POST -Headers $userH -Body $submitC

    Write-Host "`n  Session:    $($recC.sessionId)" -ForegroundColor White
    Write-Host "  Total recs: $($recC.totalRecommendations)" -ForegroundColor White

    if ($recC.weatherContext) {
        $wc = $recC.weatherContext
        Write-Host "`n  --- Weather Context ---" -ForegroundColor Magenta
        Write-Host "  Location:     $($wc.locationEn) / $($wc.locationAr)" -ForegroundColor Gray
        Write-Host "  Temperature:  $($wc.temperature) C" -ForegroundColor Gray
        Write-Host "  Season:       $($wc.seasonEn) / $($wc.seasonAr)" -ForegroundColor Gray
        Write-Host "  Climate Zone: $($wc.climateZoneEn) / $($wc.climateZoneAr)" -ForegroundColor Gray
        Write-Host "  GPS Used:     $($wc.locationUsed)" -ForegroundColor Gray

        if ($wc.locationUsed -eq $true) {
            Write-Host "  >> PASS: locationUsed=true" -ForegroundColor Green
        }

        if ($wc.climateZoneEn -eq "coastal") {
            Write-Host "  >> PASS: Climate zone = coastal (Haifa)" -ForegroundColor Green
        } else {
            Write-Host "  >> INFO: Climate zone = $($wc.climateZoneEn) (expected coastal)" -ForegroundColor DarkYellow
        }
    }

    Write-Host "`n  --- Recommendations ---" -ForegroundColor Magenta
    foreach ($r in $recC.recommendations) {
        $seasonal = if ($r.seasonalMatch) { " [SEASONAL]" } else { "" }
        Write-Host "  $($r.nameEn): $($r.matchPercentage)% ($($r.matchLevel))$seasonal" -ForegroundColor Cyan
        if ($r.seasonalNoteEn) {
            Write-Host "    Note: $($r.seasonalNoteEn)" -ForegroundColor DarkGray
        }
    }
} catch {
    Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = [System.IO.StreamReader]::new($stream)
        Write-Host "  Body: $($reader.ReadToEnd())" -ForegroundColor Red
    } catch {}
}

# ════════════════════════════════════════════════════
# TEST D: Set saved location (Hebron) then submit without GPS
# ════════════════════════════════════════════════════
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  TEST D: Save Location (Hebron) then Submit WITHOUT GPS" -ForegroundColor Cyan
Write-Host "  Should use saved location instead of default" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

# D.1 Save location
Write-Host "`n  [D.1] Saving location = Hebron..." -ForegroundColor White
try {
    $locRes = Invoke-RestMethod -Uri "$BASE/user/preferences/location" -Method POST -Headers $userH `
        -Body (@{ city="Hebron" } | ConvertTo-Json)
    Write-Host "  OK - Location saved: $($locRes.city) ($($locRes.latitude), $($locRes.longitude))" -ForegroundColor Green
} catch {
    Write-Host "  WARN: $($_.Exception.Message)" -ForegroundColor DarkYellow
}

# D.2 Verify saved location
Write-Host "  [D.2] Verifying saved location..." -ForegroundColor White
try {
    $savedLoc = Invoke-RestMethod -Uri "$BASE/user/preferences/location" -Headers $userH
    Write-Host "  OK - Saved: $($savedLoc.city) ($($savedLoc.latitude), $($savedLoc.longitude))" -ForegroundColor Green
} catch {
    Write-Host "  WARN: $($_.Exception.Message)" -ForegroundColor DarkYellow
}

# D.3 Submit answers WITHOUT GPS (should pick up Hebron)
Write-Host "  [D.3] Submitting answers without GPS..." -ForegroundColor White
$submitD = @{ answers = $answers } | ConvertTo-Json -Depth 5

try {
    $recD = Invoke-RestMethod -Uri "$BASE/user/plant-recommendation/submit-answers" `
        -Method POST -Headers $userH -Body $submitD

    if ($recD.weatherContext) {
        $wc = $recD.weatherContext
        Write-Host "`n  --- Weather Context ---" -ForegroundColor Magenta
        Write-Host "  Location:     $($wc.locationEn) / $($wc.locationAr)" -ForegroundColor Gray
        Write-Host "  Temperature:  $($wc.temperature) C" -ForegroundColor Gray
        Write-Host "  Climate Zone: $($wc.climateZoneEn) / $($wc.climateZoneAr)" -ForegroundColor Gray
        Write-Host "  GPS Used:     $($wc.locationUsed)" -ForegroundColor Gray

        if ($wc.locationUsed -eq $true) {
            Write-Host "  >> PASS: locationUsed=true (used saved Hebron location)" -ForegroundColor Green
        } else {
            Write-Host "  >> FAIL: Should have used saved location" -ForegroundColor Red
        }

        if ($wc.climateZoneEn -eq "mountain") {
            Write-Host "  >> PASS: Climate zone = mountain (Hebron)" -ForegroundColor Green
        } else {
            Write-Host "  >> INFO: Climate zone = $($wc.climateZoneEn) (expected mountain)" -ForegroundColor DarkYellow
        }
    }

    Write-Host "`n  --- Recommendations ---" -ForegroundColor Magenta
    foreach ($r in $recD.recommendations) {
        $seasonal = if ($r.seasonalMatch) { " [SEASONAL]" } else { "" }
        Write-Host "  $($r.nameEn): $($r.matchPercentage)% ($($r.matchLevel))$seasonal" -ForegroundColor Cyan
        if ($r.seasonalNoteEn) {
            Write-Host "    Note: $($r.seasonalNoteEn)" -ForegroundColor DarkGray
        }
    }
} catch {
    Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = [System.IO.StreamReader]::new($stream)
        Write-Host "  Body: $($reader.ReadToEnd())" -ForegroundColor Red
    } catch {}
}

# ════════════════════════════════════════════════════
# TEST E: Compare rankings (same answers, different locations)
# ════════════════════════════════════════════════════
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  TEST E: COMPARISON - Same Answers, Different Locations" -ForegroundColor Cyan
Write-Host "  Showing how location changes plant rankings" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

Write-Host "`n  Location     | Top Plant       | Match%  | Zone" -ForegroundColor White
Write-Host "  -------------|-----------------|---------|--------" -ForegroundColor White

# Results from previous tests
if ($recA -and $recA.recommendations.Count -gt 0) {
    $topA = $recA.recommendations[0]
    $zoneA = if ($recA.weatherContext) { $recA.weatherContext.climateZoneEn } else { "?" }
    Write-Host ("  Default      | {0,-15} | {1,5}%  | {2}" -f $topA.nameEn, $topA.matchPercentage, $zoneA) -ForegroundColor Gray
}
if ($recB -and $recB.recommendations.Count -gt 0) {
    $topB = $recB.recommendations[0]
    $zoneB = $recB.weatherContext.climateZoneEn
    Write-Host ("  Jericho      | {0,-15} | {1,5}%  | {2}" -f $topB.nameEn, $topB.matchPercentage, $zoneB) -ForegroundColor Gray
}
if ($recC -and $recC.recommendations.Count -gt 0) {
    $topC = $recC.recommendations[0]
    $zoneC = $recC.weatherContext.climateZoneEn
    Write-Host ("  Haifa        | {0,-15} | {1,5}%  | {2}" -f $topC.nameEn, $topC.matchPercentage, $zoneC) -ForegroundColor Gray
}
if ($recD -and $recD.recommendations.Count -gt 0) {
    $topD = $recD.recommendations[0]
    $zoneD = $recD.weatherContext.climateZoneEn
    Write-Host ("  Hebron       | {0,-15} | {1,5}%  | {2}" -f $topD.nameEn, $topD.matchPercentage, $zoneD) -ForegroundColor Gray
}

# ════════════════════════════════════════════════════
# TEST F: Verify available cities endpoint
# ════════════════════════════════════════════════════
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  TEST F: Available Cities for Location Selection" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

try {
    $cities = Invoke-RestMethod -Uri "$BASE/user/preferences/cities" -Headers $userH
    Write-Host "  Total cities: $($cities.Count)" -ForegroundColor Green
    Write-Host "  Cities:" -ForegroundColor White
    foreach ($c in $cities) {
        Write-Host "    $($c.nameEn) / $($c.nameAr) ($($c.latitude), $($c.longitude))" -ForegroundColor Gray
    }
} catch {
    Write-Host "  FAIL: $($_.Exception.Message)" -ForegroundColor Red
}

# ════════════════════════════════════════════════════
# SUMMARY
# ════════════════════════════════════════════════════
Write-Host "`n================================================================" -ForegroundColor Green
Write-Host "  ALL TESTS COMPLETE" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  What was tested:" -ForegroundColor White
Write-Host "    A. No GPS sent -> fallback to default/saved location" -ForegroundColor Gray
Write-Host "    B. GPS sent (Jericho) -> valley climate zone, hot weather" -ForegroundColor Gray
Write-Host "    C. GPS sent (Haifa) -> coastal climate zone" -ForegroundColor Gray
Write-Host "    D. Saved location (Hebron) -> mountain climate zone" -ForegroundColor Gray
Write-Host "    E. Ranking comparison across locations" -ForegroundColor Gray
Write-Host "    F. Available cities list" -ForegroundColor Gray
Write-Host ""
Write-Host "  Weather integration features:" -ForegroundColor White
Write-Host "    - Real-time GPS from mobile app" -ForegroundColor Gray
Write-Host "    - Fallback: GPS > saved preference > default (Nablus)" -ForegroundColor Gray
Write-Host "    - Seasonal bonus from MonthPlant data (+8%)" -ForegroundColor Gray
Write-Host "    - Temperature matching with plant minTemp/maxTemp (+7%/-10%)" -ForegroundColor Gray
Write-Host "    - Climate zone bonus (coastal/mountain/valley/desert) (+5%)" -ForegroundColor Gray
Write-Host "    - WeatherContext returned in every response" -ForegroundColor Gray
Write-Host ""

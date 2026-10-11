$base = "http://localhost:8080"

Write-Host "=== ADMIN LOGIN ==="
$adminBody = @{username='admin'; password='admin123'} | ConvertTo-Json
$adminLogin = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -Body $adminBody -ContentType 'application/json'
$atoken = $adminLogin.token
Write-Host "  Token: $($atoken.Substring(0,20))..."
Write-Host "  Username: $($adminLogin.username)"
Write-Host "  Role: $($adminLogin.role)"
Write-Host ""

Write-Host "=== ADMIN ACCESS TO ALL ADMIN ENDPOINTS (expect 200/201) ==="
$endpoints = @(
    @{Method='GET'; EP='/api/collaborators'},
    @{Method='GET'; EP='/api/teams'},
    @{Method='GET'; EP='/api/rituals'},
    @{Method='GET'; EP='/api/velocities'},
    @{Method='GET'; EP='/api/team-velocities'},
    @{Method='GET'; EP='/api/reports/velocity?teamId=1&year=2026&month=9'},
    @{Method='GET'; EP='/api/reports/velocity/export/excel?teamId=1&year=2026&month=9'}
)

foreach ($e in $endpoints) {
    $headers = @{Authorization="Bearer $atoken"}
    try {
        $r = Invoke-WebRequest -Uri "$base$($e.EP)" -Method $e.Method -Headers $headers -ErrorAction Stop
        $code = $r.StatusCode
        $ct = $r.Headers['Content-Type']
        if ($e.EP -like '*/export/excel*') {
            $ok = ($code -eq 200 -and $ct -like '*spreadsheetml.sheet*')
            $result = if ($ok) { "PASS" } else { "FAIL" }
            Write-Host "  [$result] ADMIN $($e.Method) $($e.EP) -> $code (Content-Type: $ct)"
        } else {
            $ok = ($code -eq 200)
            $result = if ($ok) { "PASS" } else { "FAIL" }
            Write-Host "  [$result] ADMIN $($e.Method) $($e.EP) -> $code"
        }
    } catch [System.Net.WebException] {
        $code = $_.Exception.Response.StatusCode.value__
        Write-Host "  [FAIL] ADMIN $($e.Method) $($e.EP) -> $code"
    }
}

Write-Host ""
Write-Host "=== COLLABORATOR TOKEN (should be 403 on admin endpoints) ==="
$collabBody = @{username='collaborator'; password='collaborator123'} | ConvertTo-Json
try {
    $collabLogin = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -Body $collabBody -ContentType 'application/json'
    $ctoken = $collabLogin.token
    foreach ($e in $endpoints) {
        $headers = @{Authorization="Bearer $ctoken"}
        try {
            $r = Invoke-WebRequest -Uri "$base$($e.EP)" -Method $e.Method -Headers $headers -ErrorAction Stop
            $code = $r.StatusCode
            Write-Host "  [FAIL] COLLAB $($e.Method) $($e.EP) -> $code (expected 403)"
        } catch [System.Net.WebException] {
            $code = $_.Exception.Response.StatusCode.value__
            $ok = ($code -eq 403)
            $result = if ($ok) { "PASS" } else { "FAIL" }
            Write-Host "  [$result] COLLAB $($e.Method) $($e.EP) -> $code (expected 403)"
        }
    }
} catch {
    Write-Host "  Collaborator login failed (user may not exist): $($_.Exception.Message)"
}

Write-Host ""
Write-Host "=== UNAUTHENTICATED (should be 401) ==="
foreach ($e in $endpoints) {
    try {
        $r = Invoke-WebRequest -Uri "$base$($e.EP)" -Method $e.Method -ErrorAction Stop
        $code = $r.StatusCode
        Write-Host "  [FAIL] ANON $($e.Method) $($e.EP) -> $code (expected 401)"
    } catch [System.Net.WebException] {
        $code = $_.Exception.Response.StatusCode.value__
        $ok = ($code -eq 401)
        $result = if ($ok) { "PASS" } else { "FAIL" }
        Write-Host "  [$result] ANON $($e.Method) $($e.EP) -> $code (expected 401)"
    }
}
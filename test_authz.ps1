$base = "http://localhost:8080"

$loginBody = @{username='collab'; password='collab123'} | ConvertTo-Json
$loginResp = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -Body $loginBody -ContentType 'application/json'
$ctoken = $loginResp.token
$chdr = @{Authorization="Bearer $ctoken"; 'Content-Type'='application/json'}

$adminLoginBody = @{username='admin'; password='admin123'} | ConvertTo-Json
$adminLogin = Invoke-RestMethod -Uri "$base/api/auth/login" -Method Post -Body $adminLoginBody -ContentType 'application/json'
$atoken = $adminLogin.token
$ahdr = @{Authorization="Bearer $atoken"; 'Content-Type'='application/json'}

Write-Host "=== COLLABORATOR: POST/PUT/PATCH/DELETE (expect 403) ==="
$tests = @(
    @{Method='POST'; EP='/api/teams'; Body='{"name":"T"}'},
    @{Method='PUT'; EP='/api/teams/1'; Body='{"name":"T"}'},
    @{Method='PATCH'; EP='/api/teams/1/activate'; Body='{}'},
    @{Method='DELETE'; EP='/api/teams/1'; Body='{}'},
    @{Method='POST'; EP='/api/collaborators'; Body='{"firstName":"A","lastName":"B"}'},
    @{Method='POST'; EP='/api/collaborators/1'; Body='{"firstName":"A","lastName":"B"}'},
    @{Method='PUT'; EP='/api/collaborators/1'; Body='{"firstName":"A","lastName":"B"}'},
    @{Method='PATCH'; EP='/api/collaborators/1/activate'; Body='{}'},
    @{Method='DELETE'; EP='/api/collaborators/1'; Body='{}'},
    @{Method='POST'; EP='/api/velocities'; Body='{}'},
    @{Method='PUT'; EP='/api/velocities/1'; Body='{}'},
    @{Method='PATCH'; EP='/api/velocities/1/validate'; Body='{}'},
    @{Method='DELETE'; EP='/api/velocities/1'; Body='{}'},
    @{Method='POST'; EP='/api/rituals'; Body='{"name":"R"}'},
    @{Method='PUT'; EP='/api/rituals/1'; Body='{"name":"R"}'},
    @{Method='DELETE'; EP='/api/rituals/1'; Body='{}'},
    @{Method='POST'; EP='/api/team-velocities'; Body='{}'},
    @{Method='DELETE'; EP='/api/team-velocities/1'; Body='{}'},
    @{Method='GET'; EP='/api/reports/velocity?teamId=1&year=2026&month=9'; Body='{}'}
)

foreach ($t in $tests) {
    $status = try {
        $r = Invoke-WebRequest -Uri "$base$($t.EP)" -Method $t.Method -Headers $chdr -ErrorAction Stop
        $r.StatusCode
    } catch [System.Net.WebException] {
        $_.Exception.Response.StatusCode.value__
    } catch {
        "ERROR: $($_.Exception.Message)"
    }
    $expect = if ($t.Method -eq 'GET' -and $t.EP -like '/api/reports/*') { 403 } else { 403 }
    $result = if ($status -eq $expect) { "PASS" } else { "FAIL" }
    Write-Host "  [$result] COLLABORATOR $($t.Method) $($t.EP) -> $status (expected $expect)"
}

Write-Host ""
Write-Host "=== UNAUTHENTICATED: all endpoints (expect 401) ==="
$noAuthTests = @(
    @{Method='GET'; EP='/api/teams'},
    @{Method='POST'; EP='/api/teams'; Body='{"name":"T"}'},
    @{Method='GET'; EP='/api/velocities'},
    @{Method='POST'; EP='/api/velocities'; Body='{}'},
    @{Method='GET'; EP='/api/rituals'},
    @{Method='GET'; EP='/api/reports/velocity?teamId=1&year=2026&month=9'},
    @{Method='GET'; EP='/api/collaborators'},
    @{Method='GET'; EP='/api/team-velocities'}
)

foreach ($t in $noAuthTests) {
    $status = try {
        $r = Invoke-WebRequest -Uri "$base$($t.EP)" -Method $t.Method -Body $($t.Body) -ContentType 'application/json' -ErrorAction Stop
        $r.StatusCode
    } catch [System.Net.WebException] {
        $_.Exception.Response.StatusCode.value__
    } catch {
        "ERROR: $($_.Exception.Message)"
    }
    $result = if ($status -eq 401) { "PASS" } else { "FAIL" }
    Write-Host "  [$result] NOAUTH $($t.Method) $($t.EP) -> $status (expected 401)"
}

Write-Host ""
Write-Host "=== ADMIN: all endpoints (expect 200/201) ==="
$adminTests = @(
    @{Method='GET'; EP='/api/teams'},
    @{Method='POST'; EP='/api/teams'; Body='{"name":"A-Team"}'},
    @{Method='GET'; EP='/api/velocities'},
    @{Method='GET'; EP='/api/rituals'},
    @{Method='GET'; EP='/api/reports/velocity?teamId=1&year=2026&month=9'}
)

foreach ($t in $adminTests) {
    $status = try {
        if ($t.Body) {
            $r = Invoke-WebRequest -Uri "$base$($t.EP)" -Method $t.Method -Body $t.Body -ContentType 'application/json' -Headers $ahdr -ErrorAction Stop
        } else {
            $r = Invoke-WebRequest -Uri "$base$($t.EP)" -Method $t.Method -Headers $ahdr -ErrorAction Stop
        }
        $r.StatusCode
    } catch [System.Net.WebException] {
        $_.Exception.Response.StatusCode.value__
    } catch {
        "ERROR: $($_.Exception.Message)"
    }
    $result = if ($status -eq 200 -or $status -eq 201) { "PASS" } else { "FAIL" }
    Write-Host "  [$result] ADMIN $($t.Method) $($t.EP) -> $status"
}

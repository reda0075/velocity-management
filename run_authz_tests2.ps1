$BASE = "http://localhost:8080"

Write-Host "=== Login ==="
$adminLogin = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method Post -Body (@{username='admin';password='admin123'} | ConvertTo-Json) -ContentType 'application/json'
$collabLogin = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method Post -Body (@{username='collab';password='collab123'} | ConvertTo-Json) -ContentType 'application/json'
$atoken = $adminLogin.token
$ctoken = $collabLogin.token

function Test-Endpoint($token, $method, $path, $body, $expected) {
    $headers = @{Authorization="Bearer $token"}
    try {
        $params = @{Uri="$BASE$path"; Method=$method; Headers=$headers; ErrorAction='Stop'; MaximumRedirection=0}
        if ($body -ne $null -and $method -ne 'GET') {
            $params.Body = $body | ConvertTo-Json
            $params.ContentType = 'application/json'
        }
        $r = Invoke-WebRequest @params
        $code = $r.StatusCode
    } catch [System.Net.WebException] {
        $code = $_.Exception.Response.StatusCode.value__
    } catch {
        if ($_.Exception.Response) {
            $code = $_.Exception.Response.StatusCode.value__
        } else {
            $code = "ERR"
        }
    }
    $result = if ($code -eq $expected) { "PASS" } else { "FAIL" }
    Write-Host "  [$result] $method $path -> $code (expected $expected)"
}

Write-Host ""
Write-Host "=== COLLABORATOR: denied (expect 403) ==="
Test-Endpoint $ctoken 'POST' '/api/teams' @{name='X'} 403
Test-Endpoint $ctoken 'PUT' '/api/teams/1' @{name='X'} 403
Test-Endpoint $ctoken 'DELETE' '/api/teams/1' $null 403
Test-Endpoint $ctoken 'POST' '/api/collaborators' @{firstName='A';lastName='B'} 403
Test-Endpoint $ctoken 'PUT' '/api/collaborators/1' @{firstName='A';lastName='B'} 403
Test-Endpoint $ctoken 'DELETE' '/api/collaborators/1' $null 403
Test-Endpoint $ctoken 'POST' '/api/velocities' @{actualHours=1;plannedHours=1} 403
Test-Endpoint $ctoken 'PUT' '/api/velocities/1' @{actualHours=1;plannedHours=1} 403
Test-Endpoint $ctoken 'PATCH' '/api/velocities/1/validate' $null 403
Test-Endpoint $ctoken 'DELETE' '/api/velocities/1' $null 403
Test-Endpoint $ctoken 'POST' '/api/rituals' @{name='R'} 403
Test-Endpoint $ctoken 'GET' '/api/rituals' $null 403
Test-Endpoint $ctoken 'PUT' '/api/rituals/1' @{name='R'} 403
Test-Endpoint $ctoken 'DELETE' '/api/rituals/1' $null 403
Test-Endpoint $ctoken 'POST' '/api/team-velocities' $null 403
Test-Endpoint $ctoken 'DELETE' '/api/team-velocities/1' $null 403
Test-Endpoint $ctoken 'GET' '/api/reports/velocity?teamId=1&year=2026&month=9' $null 403

Write-Host ""
Write-Host "=== COLLABORATOR: allowed GET (expect 200) ==="
Test-Endpoint $ctoken 'GET' '/api/teams' $null 200
Test-Endpoint $ctoken 'GET' '/api/teams/1' $null 200
Test-Endpoint $ctoken 'GET' '/api/teams/1/members' $null 200
Test-Endpoint $ctoken 'GET' '/api/velocities' $null 200
Test-Endpoint $ctoken 'GET' '/api/collaborators' $null 200
Test-Endpoint $ctoken 'GET' '/api/collaborators/1' $null 200
Test-Endpoint $ctoken 'GET' '/api/team-velocities' $null 200

Write-Host ""
Write-Host "=== ADMIN: all allowed (expect 200/201) ==="
Test-Endpoint $atoken 'GET' '/api/teams' $null 200
Test-Endpoint $atoken 'POST' '/api/teams' @{name='Admin Team'} 201
Test-Endpoint $atoken 'GET' '/api/collaborators' $null 200
Test-Endpoint $atoken 'GET' '/api/rituals' $null 200
Test-Endpoint $atoken 'GET' '/api/velocities' $null 200
Test-Endpoint $atoken 'GET' '/api/team-velocities' $null 200
Test-Endpoint $atoken 'GET' '/api/reports/velocity?teamId=1&year=2026&month=9' $null 200

Write-Host ""
Write-Host "=== UNAUTHENTICATED: all blocked (expect 401) ==="
Test-Endpoint $null 'GET' '/api/teams' $null 401
Test-Endpoint $null 'POST' '/api/teams' @{name='X'} 401
Test-Endpoint $null 'GET' '/api/rituals' $null 401
Test-Endpoint $null 'GET' '/api/reports/velocity?teamId=1&year=2026&month=9' $null 401
Test-Endpoint $null 'GET' '/api/velocities' $null 401

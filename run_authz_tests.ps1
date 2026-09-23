param($ConfigFile = "C:\Users\pc\Desktop\velocity-management\authz_tests.json")

$BASE = "http://localhost:8080"

$adminBody = @{username='admin'; password='admin123'} | ConvertTo-Json
$adminLogin = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method Post -Body $adminBody -ContentType 'application/json'
$atoken = $adminLogin.token

$collabBody = @{username='collab'; password='collab123'} | ConvertTo-Json
$collabLogin = Invoke-RestMethod -Uri "$BASE/api/auth/login" -Method Post -Body $collabBody -ContentType 'application/json'
$ctoken = $collabLogin.token

$tests = (Get-Content $ConfigFile | ConvertFrom-Json).tests

$pass = 0
$fail = 0

foreach ($t in $tests) {
    $url = "$BASE$($t.url)"
    $method = $t.method
    $body = if ($t.body -ne $null) { $t.body | ConvertTo-Json } else { $null }
    $expected = $t.expect

    $headers = @{}
    switch ($t.role) {
        "ADMIN" { $headers.Authorization = "Bearer $atoken" }
        "COLLABORATOR" { $headers.Authorization = "Bearer $ctoken" }
    }

    $status = try {
        $params = @{Uri=$url; Method=$method; Headers=$headers; ErrorAction='Stop'}
        if ($body -and $body -ne '{}') {
            $params.Body = $body
            $params.ContentType = 'application/json'
        }
        $r = Invoke-WebRequest @params
        $r.StatusCode
    } catch [System.Net.WebException] {
        $_.Exception.Response.StatusCode.value__
    } catch {
        $err = $_.Exception.Message
        if ($err -like "*401*") { 401 } elseif ($err -like "*403*") { 403 } else { "ERR:$err" }
    }

    $result = if ($status -eq $expected) { "PASS" } else { "FAIL" }
    if ($result -eq "PASS") { $pass++ } else { $fail++ }
    Write-Host "  [$result] $($t.role) $($t.method) $($t.url) -> $status (expected $expected)"
}

Write-Host ""
Write-Host "========================================="
Write-Host "  Results: $pass passed, $fail failed"
Write-Host "========================================="

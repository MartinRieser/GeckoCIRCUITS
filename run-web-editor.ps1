# GeckoCIRCUITS Web Editor - Native Desktop Launcher (PowerShell)
# Launches the backend server and opens the web editor in a standalone desktop window.

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

$RestJar = Join-Path $ScriptDir "src\modules\gecko-rest-api\target\gecko-rest-api-1.0.0.jar"
$Port = 8080
$Url = "http://localhost:$Port/gecko/"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  GeckoCIRCUITS Web Editor" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# 1. Check Java
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "[ERROR] Java is not found in PATH. Please install Java 25 or later." -ForegroundColor Red
    exit 1
}

# 2. Check and build REST JAR if needed
if (-not (Test-Path $RestJar)) {
    Write-Host "[INFO] Building GeckoCIRCUITS Web Editor package..." -ForegroundColor Yellow
    & mvn -pl src/modules/gecko-rest-api -am package -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Build failed. Please verify Maven and JDK." -ForegroundColor Red
        exit 1
    }
}

# 3. Check if server is already running
$serverRunning = $false
try {
    $r = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$Port/gecko/api/v1/circuits/catalog" -TimeoutSec 1 -ErrorAction SilentlyContinue
    if ($r.StatusCode -eq 200) {
        $serverRunning = $true
    }
} catch {}

if (-not $serverRunning) {
    Write-Host "[INFO] Starting GeckoCIRCUITS Server..." -ForegroundColor Green
    Start-Process -FilePath "javaw" -ArgumentList "-Xmx2g", "-jar", "`"$RestJar`"" -WindowStyle Hidden
    
    # Wait for server to become ready
    $ready = $false
    for ($i = 1; $i -le 30; $i++) {
        Start-Sleep -Seconds 1
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$Port/gecko/api/v1/circuits/catalog" -TimeoutSec 1 -ErrorAction SilentlyContinue
            if ($r.StatusCode -eq 200) {
                $ready = $true
                break
            }
        } catch {}
    }
    if (-not $ready) {
        Write-Host "[WARNING] Server startup timed out, attempting to open anyway..." -ForegroundColor Yellow
    }
} else {
    Write-Host "[INFO] Server is already running." -ForegroundColor Green
}

# 4. Launch in Native App Window mode
Write-Host "[INFO] Launching GeckoCIRCUITS window..." -ForegroundColor Cyan

$browserPaths = @(
    "${env:ProgramFiles}\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "${env:LOCALAPPDATA}\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "${env:ProgramFiles}\Microsoft\Edge\Application\msedge.exe",
    "${env:LOCALAPPDATA}\Microsoft\Edge\Application\msedge.exe"
)

$launched = $false
foreach ($path in $browserPaths) {
    if (Test-Path $path) {
        Start-Process -FilePath $path -ArgumentList "--app=`"$Url`"", "--app-window-size=1400,900"
        $launched = $true
        break
    }
}

if (-not $launched) {
    # Fallback to default browser
    Start-Process $Url
}

Write-Host "[INFO] GeckoCIRCUITS Web Editor started." -ForegroundColor Green

param(
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

$RestJar = Join-Path $ScriptDir "src\modules\gecko-rest-api\target\gecko-rest-api-1.0.0.jar"
$Port = 8080
$Url = "http://localhost:$Port/gecko/"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  GeckoCIRCUITS Web Editor" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# 1. Check Java (prefer JAVA_HOME if set, otherwise PATH)
$javaExe = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) { $javaExe = $javaCmd.Source }
}

if (-not $javaExe) {
    Write-Host "[ERROR] Java is not found in PATH or JAVA_HOME. Please install Java 25 or later." -ForegroundColor Red
    exit 1
}

# Verify Java version >= 25
$psiVer = New-Object System.Diagnostics.ProcessStartInfo
$psiVer.FileName = $javaExe
$psiVer.Arguments = "-version"
$psiVer.UseShellExecute = $false
$psiVer.RedirectStandardError = $true
$pVer = [System.Diagnostics.Process]::Start($psiVer)
$rawVersion = $pVer.StandardError.ReadLine()
$pVer.WaitForExit()

$versionMatch = [regex]::Match($rawVersion, 'version "(\d+)')
if ($versionMatch.Success) {
    $major = [int]$versionMatch.Groups[1].Value
    if ($major -lt 25) {
        Write-Host "[ERROR] Java 25 or later is required. Found: Java $major ($javaExe)" -ForegroundColor Red
        Write-Host "Please set JAVA_HOME or update PATH to point to JDK 25+." -ForegroundColor Red
        exit 1
    }
}

# Ensure JAVA_HOME matches the validated JDK for downstream tools (mvn)
$jdkRoot = Split-Path (Split-Path $javaExe)
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $env:JAVA_HOME = $jdkRoot
}

# Configure Guice to use child class loaders instead of deprecated sun.misc.Unsafe
if (-not $env:MAVEN_OPTS -or $env:MAVEN_OPTS -notmatch "guice_custom_class_loading") {
    $env:MAVEN_OPTS = "-Dguice_custom_class_loading=CHILD $env:MAVEN_OPTS".Trim()
}

# 2. Check and build REST JAR if needed (or if -Rebuild specified)
if ($Rebuild) {
    Write-Host "[INFO] Stopping running server on port $Port for rebuild..." -ForegroundColor Yellow
    try {
        $p = (Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue).OwningProcess
        if ($p) { Stop-Process -Id $p -Force -ErrorAction SilentlyContinue }
    } catch {}
    if (Test-Path (Join-Path $ScriptDir "frontend\package.json")) {
        Write-Host "[INFO] Building latest frontend static assets..." -ForegroundColor Yellow
        Push-Location (Join-Path $ScriptDir "frontend")
        & npm run build:spring
        Pop-Location
    }
    Write-Host "[INFO] Packaging GeckoCIRCUITS REST JAR..." -ForegroundColor Yellow
    & mvn -pl src/modules/gecko-rest-api -am package -DskipTests -q
} elseif (-not (Test-Path $RestJar)) {
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
    $javawExe = Join-Path (Split-Path $javaExe) "javaw.exe"
    if (-not (Test-Path $javawExe)) { $javawExe = "javaw" }
    Start-Process -FilePath $javawExe -ArgumentList "-Xmx2g", "-jar", "`"$RestJar`"" -WindowStyle Hidden
    
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

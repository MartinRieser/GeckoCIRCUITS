# Creates a Windows Desktop shortcut to launch GeckoCIRCUITS Web Editor as a standalone desktop app

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$repoRoot = Split-Path -Parent $scriptDir
$batPath = Join-Path $repoRoot "run-web-editor.bat"
$desktop = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktop "GeckoCIRCUITS Web Editor.lnk"

$WshShell = New-Object -ComObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut($shortcutPath)
$Shortcut.TargetPath = $batPath
$Shortcut.WorkingDirectory = $repoRoot
$Shortcut.Description = "GeckoCIRCUITS Web Editor (Native Desktop App)"
$Shortcut.WindowStyle = 7 # Minimized launch window

# Use Gecko icon if available
$iconPath = Join-Path $repoRoot "src\modules\gecko-gui\src\main\resources\gecko\geckocircuits\allg\icons\gecko_large.png"
if (Test-Path $iconPath) {
    $Shortcut.IconLocation = $iconPath
}

$Shortcut.Save()
Write-Host "Desktop shortcut created at: $shortcutPath"

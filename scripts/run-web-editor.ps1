# GeckoCIRCUITS Web Editor - Native Desktop Launcher (PowerShell)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$repoRoot = Split-Path -Parent $scriptDir
& "$repoRoot\run-web-editor.ps1"

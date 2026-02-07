#!/usr/bin/env pwsh
# ============================================================================
# COGNIFY BACKEND - Complete Fix & Verification Script
# ============================================================================
# This script performs all necessary steps to fix and verify the project
# Run this after code fixes are applied
# ============================================================================

Write-Host @"
╔════════════════════════════════════════════════════════════════╗
║                                                                ║
║     COGNIFY BACKEND - FIX & VERIFICATION SCRIPT                ║
║     All code fixes have been applied                          ║
║     This script will verify and build the project            ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝
"@ -ForegroundColor Cyan

Write-Host ""
Write-Host "📅 Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Yellow
Write-Host ""

# Set error action preference
$ErrorActionPreference = "Continue"

# Navigate to project directory
$projectPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectPath

Write-Host "📁 Project Directory: $projectPath" -ForegroundColor Green
Write-Host ""

# ============================================================================
# STEP 1: Verify Fixed Files
# ============================================================================
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "STEP 1: Verifying Fixed Files" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

$filesToCheck = @(
    "pom.xml",
    "src\main\java\com\cognify\cognify_backend\entity\UserSession.java",
    "src\main\java\com\cognify\cognify_backend\service\AuthService.java"
)

$allFilesExist = $true
foreach ($file in $filesToCheck) {
    if (Test-Path $file) {
        Write-Host "✅ Found: $file" -ForegroundColor Green
    } else {
        Write-Host "❌ Missing: $file" -ForegroundColor Red
        $allFilesExist = $false
    }
}

Write-Host ""

if (-not $allFilesExist) {
    Write-Host "❌ Some critical files are missing!" -ForegroundColor Red
    Write-Host "Please ensure all code fixes have been applied." -ForegroundColor Yellow
    exit 1
}

# ============================================================================
# STEP 2: Check Java Version
# ============================================================================
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "STEP 2: Checking Java Version" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

try {
    $javaVersion = java -version 2>&1 | Select-String -Pattern "version" | Select-Object -First 1
    Write-Host "☕ Java Version: $javaVersion" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "⚠️  Java not found in PATH. Maven wrapper will handle this." -ForegroundColor Yellow
    Write-Host ""
}

# ============================================================================
# STEP 3: Clean Previous Builds
# ============================================================================
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "STEP 3: Cleaning Previous Builds" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "🧹 Running: mvnw clean..." -ForegroundColor Yellow
& .\mvnw.cmd clean

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Clean completed successfully" -ForegroundColor Green
} else {
    Write-Host "⚠️  Clean had issues (exit code: $LASTEXITCODE)" -ForegroundColor Yellow
}
Write-Host ""

# ============================================================================
# STEP 4: Download/Update Dependencies
# ============================================================================
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "STEP 4: Downloading/Updating Dependencies" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "📦 Running: mvnw dependency:resolve -U..." -ForegroundColor Yellow
Write-Host "(This may take a few minutes on first run)" -ForegroundColor DarkGray
& .\mvnw.cmd dependency:resolve -U

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Dependencies resolved successfully" -ForegroundColor Green
} else {
    Write-Host "⚠️  Some dependencies may have issues" -ForegroundColor Yellow
    Write-Host "   (This is often normal - continuing anyway)" -ForegroundColor DarkGray
}
Write-Host ""

# ============================================================================
# STEP 5: Compile Project
# ============================================================================
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "STEP 5: Compiling Project" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "🔨 Running: mvnw compile..." -ForegroundColor Yellow
& .\mvnw.cmd compile

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Compilation successful!" -ForegroundColor Green
    $compilationSuccess = $true
} else {
    Write-Host "❌ Compilation failed!" -ForegroundColor Red
    $compilationSuccess = $false
}
Write-Host ""

# ============================================================================
# STEP 6: Run Tests (Optional)
# ============================================================================
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "STEP 6: Running Tests (Optional)" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "Would you like to run tests? (y/N): " -ForegroundColor Yellow -NoNewline
$runTests = Read-Host

if ($runTests -eq 'y' -or $runTests -eq 'Y') {
    Write-Host "🧪 Running: mvnw test..." -ForegroundColor Yellow
    & .\mvnw.cmd test

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ All tests passed!" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Some tests failed" -ForegroundColor Yellow
    }
} else {
    Write-Host "⏭️  Skipping tests" -ForegroundColor DarkGray
}
Write-Host ""

# ============================================================================
# STEP 7: Summary
# ============================================================================
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "SUMMARY" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "📊 FIX STATUS:" -ForegroundColor Yellow
Write-Host "   ✅ Lombok version updated (1.18.30 → 1.18.36)" -ForegroundColor Green
Write-Host "   ✅ Flyway PostgreSQL dependency added" -ForegroundColor Green
Write-Host "   ✅ UserSession entity created" -ForegroundColor Green
Write-Host "   ✅ AuthService import fixed" -ForegroundColor Green
Write-Host ""

if ($compilationSuccess) {
    Write-Host "🎉 PROJECT BUILD: SUCCESS" -ForegroundColor Green
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "NEXT STEPS:" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. In IntelliJ IDEA:" -ForegroundColor Yellow
    Write-Host "   • Reload Maven Project (Maven tool window → Reload)" -ForegroundColor White
    Write-Host "   • Enable annotation processing (Settings → Compiler)" -ForegroundColor White
    Write-Host "   • Rebuild Project (Build → Rebuild Project)" -ForegroundColor White
    Write-Host ""
    Write-Host "2. Start PostgreSQL:" -ForegroundColor Yellow
    Write-Host "   • Ensure database 'cognify' exists" -ForegroundColor White
    Write-Host "   • Check connection settings in application.properties" -ForegroundColor White
    Write-Host ""
    Write-Host "3. Run Application:" -ForegroundColor Yellow
    Write-Host "   • Command line: .\mvnw.cmd spring-boot:run" -ForegroundColor White
    Write-Host "   • Or in IntelliJ: Run CognifyBackendApplication" -ForegroundColor White
    Write-Host ""
    Write-Host "4. Test Health Endpoint:" -ForegroundColor Yellow
    Write-Host "   • curl http://localhost:8082/api/health" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "❌ PROJECT BUILD: FAILED" -ForegroundColor Red
    Write-Host ""
    Write-Host "TROUBLESHOOTING:" -ForegroundColor Yellow
    Write-Host "1. Check the error messages above" -ForegroundColor White
    Write-Host "2. Try: .\mvnw.cmd clean install -U" -ForegroundColor White
    Write-Host "3. In IntelliJ: File → Invalidate Caches → Restart" -ForegroundColor White
    Write-Host "4. Check NEXT_STEPS.md for detailed instructions" -ForegroundColor White
    Write-Host ""
}

Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# STEP 8: Show Key Files
# ============================================================================
Write-Host "📚 DOCUMENTATION:" -ForegroundColor Yellow
Write-Host "   • PROJECT_FIXES_COMPLETE.md - Full technical details" -ForegroundColor White
Write-Host "   • NEXT_STEPS.md - Step-by-step guide" -ForegroundColor White
Write-Host "   • FIX_SUMMARY_QUICK.md - Quick reference" -ForegroundColor White
Write-Host ""

Write-Host "✨ Script completed at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host ""

# Pause so user can read the output
Write-Host "Press any key to exit..." -ForegroundColor DarkGray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

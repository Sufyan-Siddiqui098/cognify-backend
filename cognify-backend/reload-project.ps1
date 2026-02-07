# Cognify Backend - Project Reload Script
# This script helps reload Maven dependencies and rebuild the project

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  COGNIFY BACKEND - Maven Reload & Build" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Set location to project directory
$projectPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectPath

Write-Host "📁 Current directory: $projectPath" -ForegroundColor Yellow
Write-Host ""

# Step 1: Clean Maven cache for this project
Write-Host "🧹 Step 1: Cleaning previous builds..." -ForegroundColor Green
.\mvnw.cmd clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Clean failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Clean completed" -ForegroundColor Green
Write-Host ""

# Step 2: Download dependencies
Write-Host "📦 Step 2: Downloading dependencies..." -ForegroundColor Green
.\mvnw.cmd dependency:resolve
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Some dependencies might have issues, but continuing..." -ForegroundColor Yellow
}
Write-Host "✅ Dependencies resolved" -ForegroundColor Green
Write-Host ""

# Step 3: Compile project
Write-Host "🔨 Step 3: Compiling project..." -ForegroundColor Green
.\mvnw.cmd compile
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Compilation failed!" -ForegroundColor Red
    Write-Host "Please check the errors above and fix them." -ForegroundColor Yellow
    exit 1
}
Write-Host "✅ Compilation successful" -ForegroundColor Green
Write-Host ""

# Step 4: Install (package)
Write-Host "📦 Step 4: Installing/Packaging..." -ForegroundColor Green
.\mvnw.cmd install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Install failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Install completed" -ForegroundColor Green
Write-Host ""

# Step 5: Show dependency tree
Write-Host "🌳 Step 5: Dependency Tree (first 50 lines)..." -ForegroundColor Green
.\mvnw.cmd dependency:tree | Select-Object -First 50
Write-Host ""

# Success message
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  ✅ BUILD SUCCESSFUL!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Yellow
Write-Host "  1. In IntelliJ: File → Invalidate Caches → Invalidate and Restart" -ForegroundColor White
Write-Host "  2. Enable Annotation Processing: Settings → Compiler → Annotation Processors" -ForegroundColor White
Write-Host "  3. Reload Maven Project: Right-click pom.xml → Maven → Reload Project" -ForegroundColor White
Write-Host "  4. Start PostgreSQL and Redis" -ForegroundColor White
Write-Host "  5. Run: .\mvnw.cmd spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "📖 For more details, see: VERSION_UPDATE_GUIDE.md" -ForegroundColor Cyan
Write-Host ""

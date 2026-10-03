@echo off
setlocal
if "%~1"=="" (
  echo Usage: PUSH_TO_GITHUB.bat https://github.com/USERNAME/REPOSITORY.git
  exit /b 1
)
where git >nul 2>nul || (
  echo Git is not installed or not available in PATH.
  exit /b 1
)
if not exist .git git init

git checkout -B main
git add .
git diff --cached --quiet || git commit -m "Initial Quran Premium Android project"
git remote remove origin >nul 2>nul
git remote add origin "%~1"
git push -u origin main
if errorlevel 1 exit /b 1

echo.
echo Upload complete. Open GitHub Actions and run: Build Android APK
endlocal

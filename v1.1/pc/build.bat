@echo off
setlocal
cd /d "%~dp0"

echo ==========================================
echo   NovelAI Prompt Tool - Build Script
echo ==========================================
echo.

echo [1/3] Installing/Updating PyInstaller...
pip install --upgrade pyinstaller
if errorlevel 1 (
    echo Failed to install PyInstaller!
    pause
    exit /b 1
)

echo.
echo [2/3] Cleaning old build...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist

echo.
echo [3/3] Building directory mode (fastest startup)...
pyinstaller --clean -D -w -n NovelAIPromptTool ^
  --add-data "index.html;." ^
  --add-data "css;css" ^
  --add-data "script;script" ^
  main.py

echo.
if exist "dist\NovelAIPromptTool\NovelAIPromptTool.exe" (
    echo.
    echo Build SUCCESS! Folder is at: dist\NovelAIPromptTool\
) else (
    echo.
    echo Build FAILED, please check the log above.
)
pause

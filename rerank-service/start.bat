@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ============================================
echo Python Rerank Microservice
echo ============================================
echo.

cd /d "%~dp0"

REM Check Python
echo Checking Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo Error: Python not found! Please install Python 3.10+ first.
    echo Download from: https://www.python.org/downloads/
    pause
    exit /b 1
)

for /f "tokens=2" %%i in ('python --version 2^>^&1') do set PYVER=%%i
echo Python version: %PYVER%
echo.

REM Create virtual environment if not exists
if not exist "venv" (
    echo Creating virtual environment...
    python -m venv venv
    if errorlevel 1 (
        echo Error: Failed to create virtual environment!
        pause
        exit /b 1
    )
)

REM Activate virtual environment
echo Activating virtual environment...
call venv\Scripts\activate.bat

REM Install dependencies
echo Installing dependencies...
pip install -r requirements.txt -q
if errorlevel 1 (
    echo Warning: Some dependencies may have failed to install.
)

echo.
echo ============================================
echo Starting Rerank service on port 8082...
echo Model will be downloaded automatically on first run.
echo ============================================
echo.

python main.py

pause

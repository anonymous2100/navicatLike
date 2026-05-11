@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "ARTIFACT_ID=navicatLike"
set "VERSION=0.0.1"
set "JAR_NAME=%ARTIFACT_ID%-%VERSION%.jar"
set "APP_NAME=LightDBViewer"
set "DIST_DIR=%PROJECT_DIR%dist-exe"
set "RUNTIME_DIR=%DIST_DIR%\runtime"

echo ============================================
echo  %APP_NAME% EXE Build Script
echo  Output: EXE with bundled JRE (no JDK required)
echo ============================================
echo.

:: ---------- Step 1: Maven Build ----------
echo [1/4] Building fat JAR...
cd /d "%PROJECT_DIR%"
call mvn.cmd clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed.
    pause
    exit /b 1
)
echo [OK] JAR built: target\%JAR_NAME%
echo.

:: ---------- Step 2: Create bundled JRE via jlink ----------
echo [2/4] Creating bundled JRE (jlink)...
if exist "%RUNTIME_DIR%" rmdir /s /q "%RUNTIME_DIR%"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

call jlink --no-header-files --no-man-pages --strip-debug ^
    --add-modules java.base,java.compiler,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.security.sasl,java.sql,jdk.crypto.ec,jdk.unsupported ^
    --output "%RUNTIME_DIR%"
if %errorlevel% neq 0 (
    echo [ERROR] jlink failed.
    pause
    exit /b 1
)
echo [OK] Bundled JRE created: %RUNTIME_DIR%
echo.

:: ---------- Step 3: Launch4j (EXE) ----------
echo [3/4] Generating EXE via Launch4j...
cd /d "%PROJECT_DIR%"
call mvn.cmd launch4j:launch4j
if %errorlevel% neq 0 (
    echo [ERROR] Launch4j failed.
    pause
    exit /b 1
)
echo [OK] EXE created: %DIST_DIR%\%APP_NAME%.exe
echo.

:: ---------- Step 4: Copy resources ----------
echo [4/4] Copying resources...
if exist "%PROJECT_DIR%lightdbviewer.properties" (
    copy /y "%PROJECT_DIR%lightdbviewer.properties" "%DIST_DIR%\" >nul
)
if not exist "%DIST_DIR%\logs" mkdir "%DIST_DIR%\logs"
echo [OK] Resources copied.
echo.

echo ============================================
echo  Build complete!
echo.
echo  Output: %DIST_DIR%\
echo    %APP_NAME%.exe    - Application launcher
echo    runtime\          - Bundled JRE (Java 21)
echo.
echo  To run: double-click %APP_NAME%.exe
echo  To distribute: zip the entire dist-exe folder
echo ============================================
pause

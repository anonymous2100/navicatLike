@echo off
chcp 65001 >nul
setlocal

set PROJECT_DIR=%~dp0
set ARTIFACT_ID=navicatLike
set VERSION=0.0.1
set JAR_NAME=%ARTIFACT_ID%-%VERSION%.jar
set APP_NAME=LightDBViewer
set DIST_DIR=%PROJECT_DIR%dist
set ZIP_NAME=%PROJECT_DIR%%APP_NAME%-v%VERSION%.zip

echo ============================================
echo  NavicatLike 打包脚本
echo ============================================
echo.

:: ---------- Step 1: Maven Build ----------
echo [1/4] 清理并构建 fat JAR...
cd /d "%PROJECT_DIR%"
call mvn.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo [错误] Maven 构建失败。
    pause
    exit /b 1
)
echo [完成] 构建成功。
echo.

:: ---------- Step 2: Verify JAR ----------
echo [2/4] 验证 fat JAR...
if not exist "%PROJECT_DIR%target\%JAR_NAME%" (
    echo [错误] 未找到 %JAR_NAME%
    pause
    exit /b 1
)
echo [完成] JAR 文件确认存在。
echo.

:: ---------- Step 3: Create dist ----------
echo [3/4] 创建 dist 目录并生成启动脚本...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"
mkdir "%DIST_DIR%\logs"

copy "%PROJECT_DIR%target\%JAR_NAME%" "%DIST_DIR%\%JAR_NAME%" >nul
if %errorlevel% neq 0 (
    echo [错误] 复制 JAR 失败。
    pause
    exit /b 1
)

if exist "%PROJECT_DIR%lightdbviewer.properties" copy "%PROJECT_DIR%lightdbviewer.properties" "%DIST_DIR%\" >nul
if exist "%PROJECT_DIR%%APP_NAME%.properties" copy "%PROJECT_DIR%%APP_NAME%.properties" "%DIST_DIR%\" >nul

:: 用 Python 生成 start.bat，彻底避免 cmd 转义问题
set SB=%DIST_DIR%\start.bat
python -c "lines=['@echo off\n','chcp 65001 >nul\n','title %APP_NAME%\n','\n','cd /d \"%%~dp0\"\n','\n','where java >nul 2>&1\n','if %%ERRORLEVEL%% NEQ 0 (\n','    echo [ERROR] Java runtime not found.\n','    pause\n','    exit /b 1\n',')\n','\n','echo Starting %APP_NAME% v%VERSION% ...\n','start \"\" javaw -Dfile.encoding=UTF-8 -jar \"%JAR_NAME%\"\n','exit\n'];open(r'%SB%','w',encoding='gbk').writelines(lines)"

if not exist "%SB%" (
    echo [错误] start.bat 生成失败。
    pause
    exit /b 1
)
echo [完成] dist 目录创建完成，start.bat 已生成。
echo.

:: ---------- Step 4: ZIP ----------
echo [4/4] 生成 ZIP 包...
if exist "%ZIP_NAME%" del "%ZIP_NAME%"

set PS1=%PROJECT_DIR%_zip_tmp.ps1
echo Compress-Archive -Path "%DIST_DIR%\*" -DestinationPath "%ZIP_NAME%" -Force>"%PS1%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
del "%PS1%" >nul 2>&1

if %errorlevel% neq 0 (
    echo [错误] ZIP 打包失败。
    pause
    exit /b 1
)
echo [完成] ZIP 包已生成。
echo.

echo ============================================
echo  打包完成！
echo  dist: %DIST_DIR%\
echo  ZIP:  %ZIP_NAME%
echo  运行: %DIST_DIR%\start.bat
echo ============================================
pause
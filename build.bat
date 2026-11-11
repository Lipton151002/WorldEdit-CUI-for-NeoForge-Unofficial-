@echo off
setlocal
set "GRADLE_USER_HOME=%USERPROFILE%\Desktop\worldgui-dev\gradle-home"
cd /d "%~dp0"
call gradlew.bat clean build %*
if errorlevel 1 (
    echo Build failed
    exit /b 1
)
echo.
echo Jars: %~dp0dist

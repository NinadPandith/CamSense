@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title CamSense - build and install
echo.
echo  ============================================
echo   CamSense: build, install and open on phone
echo  ============================================
echo.

rem ---------------------------------------------------------------
rem 1. Java: use JAVA_HOME, else the one bundled with Android Studio
rem ---------------------------------------------------------------
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto java_ok
set "JAVA_HOME="
for %%D in (
    "%ProgramFiles%\Android\Android Studio\jbr"
    "%ProgramFiles%\Android\Android Studio\jre"
    "%LOCALAPPDATA%\Programs\Android Studio\jbr"
    "%ProgramFiles(x86)%\Android\Android Studio\jbr"
) do (
    if not defined JAVA_HOME if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
)
if defined JAVA_HOME goto java_ok
echo  [X] Java not found.
echo      Install Android Studio from https://developer.android.com/studio
echo      and open it once, then run this file again.
goto fail
:java_ok
echo  [OK] Java: %JAVA_HOME%

rem ---------------------------------------------------------------
rem 2. Android SDK: ANDROID_HOME, else Android Studio's default folder
rem ---------------------------------------------------------------
if not defined ANDROID_HOME if defined ANDROID_SDK_ROOT set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if defined ANDROID_HOME if exist "%ANDROID_HOME%\platform-tools\adb.exe" goto sdk_ok
echo  [X] Android SDK not found.
echo      Open Android Studio once and finish the setup wizard
echo      (it downloads the SDK), then run this file again.
goto fail
:sdk_ok
echo  [OK] Android SDK: %ANDROID_HOME%
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"

rem Gradle reads the SDK location from local.properties (backslashes doubled).
set "SDK_ESCAPED=%ANDROID_HOME:\=\\%"
> local.properties echo sdk.dir=%SDK_ESCAPED%

rem ---------------------------------------------------------------
rem 3. Phone connected with USB debugging?
rem ---------------------------------------------------------------
:check_phone
set /a PHONES=0
set "UNAUTHORIZED="
for /f "skip=1 tokens=1,2" %%A in ('call "%ADB%" devices 2^>nul') do (
    if "%%B"=="device" set /a PHONES+=1
    if "%%B"=="unauthorized" set "UNAUTHORIZED=1"
)
if %PHONES% GEQ 1 goto phone_ok
echo.
if defined UNAUTHORIZED (
    echo  [!] Phone found but not allowed yet.
    echo      Unlock your phone and tap "Allow" on the USB debugging prompt.
) else (
    echo  [!] No phone found. On your phone:
    echo      1. Settings - About phone - tap "Build number" 7 times
    echo      2. Settings - Developer options - turn on "USB debugging"
    echo      3. Connect it with a USB cable and tap "Allow"
)
echo.
choice /c RQ /n /m "  Press R to check again, or Q to quit: "
if errorlevel 2 goto fail
goto check_phone
:phone_ok
echo  [OK] Phone connected

rem ---------------------------------------------------------------
rem 4. Build and install (first run downloads Gradle: a few minutes)
rem ---------------------------------------------------------------
echo.
echo  Building and installing. The first run takes 5-10 minutes...
echo  (full log is saved to build-log.txt)
echo.
powershell -NoProfile -ExecutionPolicy Bypass -Command "& '.\gradlew.bat' installDebug --console=plain 2>&1 | Tee-Object -FilePath 'build-log.txt'; exit $LASTEXITCODE"
if errorlevel 1 (
    echo.
    echo  [X] Build failed. Send build-log.txt to Claude
    echo      If it says INSTALL_FAILED_USER_RESTRICTED, turn on "Install via USB"
    echo      in Developer options ^(Xiaomi, Oppo, Vivo phones^).
    echo      ^(the most useful part is near the words "What went wrong"^).
    goto fail
)

rem ---------------------------------------------------------------
rem 5. Open the app and show its log
rem ---------------------------------------------------------------
"%ADB%" shell am start -n com.camsense/.MainActivity >nul
echo.
echo  [OK] CamSense is installed and open on your phone.
echo.
echo  Showing CamSense log messages below (also saved to app-log.txt).
echo  If something goes wrong, send app-log.txt to Claude.
echo  Close this window when you're done.
echo.
"%ADB%" logcat -c
powershell -NoProfile -Command "& '%ADB%' logcat -v time | Select-String -Pattern 'CamSense|AndroidRuntime|FATAL' | Tee-Object -FilePath 'app-log.txt'"
goto end

:fail
echo.
pause
exit /b 1

:end
pause
endlocal

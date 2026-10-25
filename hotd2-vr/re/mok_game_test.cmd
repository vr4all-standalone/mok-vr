@echo off
rem Tests loaded-game selection with standalone policy ON and normal Flycast policy OFF.
rem Usage: standalone_game_test.cmd [output directory]
setlocal
set ROOT=%~dp0..\..
set OUT=%~1
if "%OUT%"=="" set OUT=%ROOT%\build-win\vrtest\mok-game-policy-unit
set VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe
for /f "usebackq delims=" %%i in (`"%VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do set "VSROOT=%%i"
if not defined VSROOT (
    echo Visual Studio with the C++ tools was not found.
    exit /b 1
)
call "%VSROOT%\VC\Auxiliary\Build\vcvars64.bat" >nul || exit /b 1
if not exist "%OUT%" mkdir "%OUT%" || exit /b 1
pushd "%OUT%" || exit /b 1
cl /nologo /std:c++17 /EHsc /O2 /W3 /I"%ROOT%\core" /DVR_STANDALONE_MOK=1 /DEXPECT_STANDALONE_MOK=1 /Festandalone-on.exe "%~dp0mok_game_test.cpp" >build-on.log 2>&1
if errorlevel 1 goto build_failed
standalone-on.exe
if errorlevel 1 goto failed
cl /nologo /std:c++17 /EHsc /O2 /W3 /I"%ROOT%\core" /DEXPECT_STANDALONE_MOK=0 /Festandalone-off.exe "%~dp0mok_game_test.cpp" >build-off.log 2>&1
if errorlevel 1 goto build_failed
standalone-off.exe
if errorlevel 1 goto failed
cl /nologo /std:c++17 /EHsc /O2 /W3 /I"%ROOT%\core" /DVR_STANDALONE_MOK=0 /DEXPECT_STANDALONE_MOK=0 /Festandalone-zero.exe "%~dp0mok_game_test.cpp" >build-zero.log 2>&1
if errorlevel 1 goto build_failed
standalone-zero.exe
if errorlevel 1 goto failed
cl /nologo /std:c++17 /EHsc /O2 /W3 /I"%ROOT%\core" /Femok-staff-cache.exe "%~dp0mok_staff_cache_test.cpp" >build-cache.log 2>&1
if errorlevel 1 goto build_failed
mok-staff-cache.exe
if errorlevel 1 goto failed
cl /nologo /std:c++17 /EHsc /O2 /W3 /I"%ROOT%\core" /Femok-preparation-input.exe "%~dp0mok_preparation_input_test.cpp" >build-prep.log 2>&1
if errorlevel 1 goto build_failed
mok-preparation-input.exe
if errorlevel 1 goto failed
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0..\mok-reload-test.ps1"
if errorlevel 1 goto failed
popd
exit /b 0
:build_failed
type build-on.log
if exist build-off.log type build-off.log
if exist build-zero.log type build-zero.log
:failed
popd
exit /b 1

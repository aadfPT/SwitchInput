@echo off
setlocal EnableDelayedExpansion
set "ROOT=%~dp0.."
set "DIAG=%ROOT%\diag"
if not exist "%DIAG%" mkdir "%DIAG%"
set "FS=192.168.1.141:5555"
set "ADB=adb"
where adb >nul 2>&1
if errorlevel 1 if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "LOG=%DIAG%\probe_log.txt"
echo probe started %DATE% %TIME% > "%LOG%"
"%ADB%" version >> "%LOG%" 2>&1
"%ADB%" connect %FS% >> "%LOG%" 2>&1
"%ADB%" devices -l >> "%LOG%" 2>&1
set N=0
for /f "usebackq delims=" %%C in ("%ROOT%\tools\probe_cmds.txt") do (
  set /a N+=1
  echo ### !N! %%C> "%DIAG%\out_!N!.txt"
  "%ADB%" -s %FS% shell %%C >> "%DIAG%\out_!N!.txt" 2>&1
)
echo probe finished %DATE% %TIME% >> "%LOG%"

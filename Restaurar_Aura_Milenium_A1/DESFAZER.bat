@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Restaurar.ps1" -Acao Desfazer
if errorlevel 1 pause
endlocal

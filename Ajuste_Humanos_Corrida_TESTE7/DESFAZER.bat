@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Ajustar.ps1" -Acao Desfazer
if errorlevel 1 pause
endlocal

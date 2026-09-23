@echo off
title MongoDB - Apagado
echo ============================================
echo   Apagando MongoDB de forma segura...
echo ============================================

powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 27017 -State Listen -ErrorAction SilentlyContinue) { exit 1 } else { exit 0 }"
if %errorlevel%==0 (
    echo MongoDB ya esta apagado.
    pause
    exit /b
)

rem Apagado limpio: evita corromper los datos WiredTiger
"C:\Users\josue\AppData\Local\Programs\mongosh\mongosh.exe" --quiet mongodb://localhost:27017/admin --eval "db.shutdownServer()" 2>nul

timeout /t 3 /nobreak >nul

powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 27017 -State Listen -ErrorAction SilentlyContinue) { Write-Host 'MongoDB sigue encendido' } else { Write-Host 'MongoDB APAGADO correctamente' }"
pause

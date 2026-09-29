@echo off
title MongoDB - Encendido
echo ============================================
echo   Prendiendo MongoDB en localhost:27017...
echo ============================================

rem Si ya esta corriendo, no hace nada
powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 27017 -State Listen -ErrorAction SilentlyContinue) { exit 0 } else { exit 1 }"
if %errorlevel%==0 (
    echo MongoDB ya esta encendido.
    pause
    exit /b
)

rem Abre MongoDB en una ventana separada con tus datos reales
start "MongoDB (cierra con Apagar-Mongo)" "C:\Program Files\MongoDB\Server\8.2\bin\mongod.exe" --dbpath "C:\data\db" --port 27017 --bind_ip 127.0.0.1

timeout /t 5 /nobreak >nul

powershell -NoProfile -Command "if (Get-NetTCPConnection -LocalPort 27017 -State Listen -ErrorAction SilentlyContinue) { Write-Host 'MongoDB ENCENDIDO correctamente' } else { Write-Host 'ERROR: MongoDB no arranco' }"
pause

@echo off
setlocal enabledelayedexpansion
title IPTV Scraper API Manager - Quendic
set SERVER_IP=89.144.10.224
set SSH_USER=root
set APP_NAME=iptv-scraper-api

:MENU
cls
echo ==========================================
echo    IPTV SCRAPER API MANAGEMENT MENU
echo ==========================================
echo [1] Durum Kontrol (Status)
echo [2] API Yeniden Baslat (Restart)
echo [3] API Durdur (Stop)
echo [4] API Baslat (Start)
echo [5] Canli Log Takibi (Logs)
echo [6] API Verisini Test Et (Curl)
echo [x] Cikis
echo ==========================================
set /p choice="Seciminizi yapin: "

if "%choice%"=="1" goto STATUS
if "%choice%"=="2" goto RESTART
if "%choice%"=="3" goto STOP
if "%choice%"=="4" goto START
if "%choice%"=="5" goto LOGS
if "%choice%"=="6" goto CURL
if "%choice%"=="x" goto EXIT
goto MENU

:STATUS
cls
echo Durum kontrol ediliyor...
ssh %SSH_USER%@%SERVER_IP% "pm2 show %APP_NAME%"
pause
goto MENU

:RESTART
cls
echo API Yeniden baslatiliyor...
ssh %SSH_USER%@%SERVER_IP% "pm2 restart %APP_NAME%"
pause
goto MENU

:STOP
cls
echo API Durduruluyor...
ssh %SSH_USER%@%SERVER_IP% "pm2 stop %APP_NAME%"
pause
goto MENU

:START
cls
echo API Baslatiliyor...
ssh %SSH_USER%@%SERVER_IP% "pm2 start %APP_NAME%"
pause
goto MENU

:LOGS
cls
echo Canli loglar (Cikmak icin Ctrl+C yapin)...
ssh %SSH_USER%@%SERVER_IP% "pm2 logs %APP_NAME% --lines 50"
pause
goto MENU

:CURL
cls
echo API verisi cekiliyor...
ssh %SSH_USER%@%SERVER_IP% "curl -s http://localhost:3000/api/matches | head -c 500"
echo.
echo.
pause
goto MENU

:EXIT
exit

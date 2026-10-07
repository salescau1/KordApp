@echo off
title KordApp Crawler Monitor
cd /d "C:\KordApp-AGY"
echo ========================================================
echo   Menjalankan KordApp Crawler Monitor Dashboard...
echo   Buka browser di: http://localhost:8000
echo ========================================================
python tools\dashboard\server.py
pause

@echo off
setlocal
cd /d "%~dp0"

if "%1"=="" (
  start "" "frontend\index.html"
) else (
  start "" "frontend\index.html?api=http://localhost:%1"
)

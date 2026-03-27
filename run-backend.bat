@echo off
setlocal
cd /d "%~dp0"

if "%1"=="" (
  set PORT=8080
) else (
  set PORT=%1
)

echo Compiling backend...
javac -cp ".;src;lib/*" src\*.java src\org\slf4j\*.java
if errorlevel 1 (
  echo Compilation failed.
  exit /b 1
)

echo Starting API server on port %PORT%...
java -cp "src;lib/sqlite-jdbc.jar" ApiServer %PORT%

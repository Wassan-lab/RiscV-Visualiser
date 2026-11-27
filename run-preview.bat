@echo off
cd /d "%~dp0"
java Build.java run
if errorlevel 1 pause

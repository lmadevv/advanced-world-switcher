@echo off
title RuneLite Dev Launcher - Advanced World Switcher
cd /d "%~dp0"
if exist "%~dp0..\jdk17\jdk-17.0.20.1+1" (
    set "JAVA_HOME=%~dp0..\jdk17\jdk-17.0.20.1+1"
)
set "JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT"
echo Starting RuneLite with Advanced World Switcher...
call gradlew.bat run
pause

@echo off
title ApplyTrack - Desktop Launcher
set "JAVA_HOME=c:\Users\HP\OneDrive\FocusVerse\.tools\jdk21.0.13_0"
set "M2_HOME=c:\Users\HP\OneDrive\FocusVerse\.tools\apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

echo ==============================================================
echo   ApplyTrack - Job Application Intelligence System
echo   Launching JavaFX UI...
echo ==============================================================

call mvn javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited or encountered an issue.
    pause
)


@echo off
title ApplyTrack - Project Builder & Tester
set "JAVA_HOME=c:\Users\HP\OneDrive\FocusVerse\.tools\jdk21.0.13_0"
set "M2_HOME=c:\Users\HP\OneDrive\FocusVerse\.tools\apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

echo ==============================================================
echo   ApplyTrack - Compiling and Running Unit Tests
echo ==============================================================

call mvn clean test

echo.
echo Build finished with exit code %ERRORLEVEL%.
pause


@echo off
cd /d "%~dp0"
if not exist data mkdir data
javac -d . src\QuizCraftServer.java
if errorlevel 1 (echo Compilation failed. Install a JDK 17 or newer and try again.& pause & exit /b 1)
java -cp . QuizCraftServer
pause

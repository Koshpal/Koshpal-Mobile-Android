@echo off
REM Clean build script for Koshpal Mobile Android

echo ========================================
echo Cleaning Gradle Build Cache...
echo ========================================

REM Remove app build directory
if exist "app\build" (
    echo Removing app\build directory...
    rmdir /s /q "app\build"
)

REM Remove root build directory
if exist "build" (
    echo Removing build directory...
    rmdir /s /q "build"
)

REM Remove .gradle cache
if exist ".gradle" (
    echo Removing .gradle cache...
    rmdir /s /q ".gradle"
)

REM Remove .kotlin cache
if exist ".kotlin" (
    echo Removing .kotlin cache...
    rmdir /s /q ".kotlin"
)

REM Remove .idea build cache
if exist ".idea\caches" (
    echo Removing .idea\caches...
    rmdir /s /q ".idea\caches"
)

echo.
echo ========================================
echo Clean Complete!
echo ========================================
echo.
echo Now run: ./gradlew build
echo.
pause

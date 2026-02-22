@echo off
echo ========================================
echo    Lancement de MindCare Application
echo ========================================
echo.

echo Verification de Maven...
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: Maven n'est pas installe ou n'est pas dans le PATH
    echo Veuillez installer Maven depuis https://maven.apache.org/
    pause
    exit /b 1
)

echo Maven trouve!
echo.

echo Compilation du projet...
call mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: La compilation a echoue
    pause
    exit /b 1
)

echo.
echo Compilation reussie!
echo.

echo Lancement de l'application JavaFX...
call mvn javafx:run

pause

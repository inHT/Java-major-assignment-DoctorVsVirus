@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src/game/*.java src/game/audio/*.java src/game/map/*.java src/game/units/*.java src/game/enemies/*.java src/game/system/*.java src/game/ui/*.java src/javazoom/jl/decoder/*.java
if errorlevel 1 (
  echo 编译失败。
  exit /b 1
)
if not exist out\game\assets mkdir out\game\assets
xcopy /Y /Q src\game\assets\*.* out\game\assets\ >nul
if not exist out\javazoom\jl\decoder mkdir out\javazoom\jl\decoder
xcopy /Y /Q src\javazoom\jl\decoder\*.ser out\javazoom\jl\decoder\ >nul
echo 编译成功，正在启动...
java -cp out game.Main

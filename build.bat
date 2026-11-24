@echo off
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac --release 17 -d out -cp "lib/*" @sources.txt
del sources.txt
echo Build finished (see errors above if any).

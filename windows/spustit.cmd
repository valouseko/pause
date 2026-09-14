@echo off
rem Spusti Mezeru (s viditelnym oknem konzole - dobre pro prvni test).
cd /d "%~dp0"
if not exist node_modules (
  echo Instaluji zavislosti, chvili to potrva...
  call npm install
)
call npm start

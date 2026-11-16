@echo off
rem Chave reserva do sistema do salao (#46): cria ou redefine a senha de um administrador
rem sem precisar entrar no sistema. So funciona NESTE computador (precisa do banco e da DB_PASSWORD).
rem
rem   redefinir-senha-dono.cmd            pergunta o login
rem   redefinir-senha-dono.cmd leonardo   ja usa esse login
rem
rem Se o login nao existir, cria um administrador com ele. O sistema pode estar ligado ou desligado.

cd /d "%~dp0"

if not exist target\sistemasalao-0.0.1-SNAPSHOT.jar (
  echo O sistema ainda nao foi gerado. Rode gerar-sistema.cmd primeiro.
  pause
  exit /b 1
)

set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" -version >nul 2>&1 || (
  echo Java nao encontrado. Instale o Java 21 e crie a variavel JAVA_HOME apontando para ele.
  pause
  exit /b 1
)

if "%DB_PASSWORD%"=="" (
  echo DB_PASSWORD nao definida: sem ela nao da para abrir o banco. Ver README.
  pause
  exit /b 1
)

set "OPCAO=--redefinir-senha"
if not "%~1"=="" set "OPCAO=--redefinir-senha=%~1"

rem web-application-type=none: nao abre o site, so roda a troca de senha e fecha
"%JAVA%" -jar target\sistemasalao-0.0.1-SNAPSHOT.jar %OPCAO% --spring.main.web-application-type=none --spring.main.banner-mode=off --logging.level.root=WARN
echo.
pause

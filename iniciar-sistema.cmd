@echo off
rem Liga o sistema do salao. Deixe esta janela aberta enquanto o salao estiver usando.
rem Precisa de: Java 21, PostgreSQL ligado e as variaveis de ambiente DB_PASSWORD e JWT_SECRET
rem (ver README, secao "Colocar em uso no salao").

cd /d "%~dp0"

if not exist target\sistemasalao-0.0.1-SNAPSHOT.jar (
  echo O sistema ainda nao foi gerado. Rode gerar-sistema.cmd primeiro.
  pause
  exit /b 1
)

rem Usa o Java de JAVA_HOME; se ela nao existir, tenta o "java" do PATH
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
"%JAVA%" -version >nul 2>&1 || (
  echo Java nao encontrado. Instale o Java 21 e crie a variavel JAVA_HOME apontando para ele.
  pause
  exit /b 1
)

if "%DB_PASSWORD%"=="" echo AVISO: DB_PASSWORD nao definida; o sistema nao vai conseguir abrir o banco.
if "%JWT_SECRET%"=="" echo AVISO: JWT_SECRET nao definida; todos precisarao entrar de novo quando o sistema reiniciar.

echo.
echo Abrindo o sistema em http://localhost:8080
echo Nos outros aparelhos da rede do salao, use http://IP-DESTE-COMPUTADOR:8080
echo (o IP aparece no comando ipconfig, em "Endereco IPv4").
echo.

"%JAVA%" -jar target\sistemasalao-0.0.1-SNAPSHOT.jar
pause

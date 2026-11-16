@echo off
rem Gera o sistema completo num unico arquivo .jar (telas + API).
rem Precisa de: Java 21 (com a variavel JAVA_HOME) e Node.js 20 ou mais novo. Rode na pasta do projeto.
rem Resultado: target\sistemasalao-0.0.1-SNAPSHOT.jar

cd /d "%~dp0"

rem O mvnw.cmd usa o Windows PowerShell; garante a pasta dele no PATH so durante este script
set "PATH=%PATH%;%SystemRoot%\System32\WindowsPowerShell\v1.0"

if not defined JAVA_HOME (
  where java >nul 2>&1 || (
    echo Java nao encontrado. Instale o Java 21 e crie a variavel JAVA_HOME apontando para ele.
    exit /b 1
  )
)
where npm >nul 2>&1 || (
  echo Node.js nao encontrado. Instale o Node.js 20 ou mais novo.
  exit /b 1
)

echo [1/2] Gerando as telas (React)...
pushd frontend
call npm ci || goto erro
call npm run build || goto erro
popd

echo [2/2] Gerando o .jar (Spring Boot) e rodando os testes...
call "%~dp0mvnw.cmd" -q clean package || goto erro

echo.
echo Pronto: target\sistemasalao-0.0.1-SNAPSHOT.jar
echo Para ligar o sistema, rode iniciar-sistema.cmd
exit /b 0

:erro
echo.
echo Algo deu errado. Leia a mensagem acima.
exit /b 1

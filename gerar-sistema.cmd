@echo off
rem Gera o sistema completo num unico arquivo .jar (telas + API).
rem Precisa de: Java 21 e Node.js 20 ou mais novo. Rode na pasta do projeto.
rem Resultado: target\sistemasalao-0.0.1-SNAPSHOT.jar

cd /d "%~dp0"

echo [1/2] Gerando as telas (React)...
pushd frontend
call npm ci || goto erro
call npm run build || goto erro
popd

echo [2/2] Gerando o .jar (Spring Boot) e rodando os testes...
call mvnw.cmd -q clean package || goto erro

echo.
echo Pronto: target\sistemasalao-0.0.1-SNAPSHOT.jar
echo Para ligar o sistema, rode iniciar-sistema.cmd
exit /b 0

:erro
echo.
echo Algo deu errado. Leia a mensagem acima.
exit /b 1

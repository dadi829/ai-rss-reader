@echo off
REM AI 资讯 RSS 阅读器 · 本地命令行编译环境（Windows CMD / PowerShell）
REM 用法：在项目根目录执行  scripts\env.cmd   然后 gradle :app:assembleDebug

set "JAVA_HOME=D:\agent\tools\jdk-17.0.20.1+1"
set "ANDROID_HOME=D:\agent\tools\android-sdk"
set "ANDROID_SDK_ROOT=D:\agent\tools\android-sdk"
set "GRADLE_HOME=D:\agent\tools\gradle-8.9"
set "PATH=%JAVA_HOME%\bin;%GRADLE_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

echo JAVA_HOME    = %JAVA_HOME%
echo ANDROID_HOME = %ANDROID_HOME%
echo.
java -version

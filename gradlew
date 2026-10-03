#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_URL="https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Gradle wrapper JAR is missing; downloading pinned Gradle 9.6.0 wrapper..."
    mkdir -p "$(dirname "$WRAPPER_JAR")"
    if command -v curl >/dev/null 2>&1; then
        curl -L --fail "$WRAPPER_URL" -o "$WRAPPER_JAR" || exit 1
    elif command -v wget >/dev/null 2>&1; then
        wget "$WRAPPER_URL" -O "$WRAPPER_JAR" || exit 1
    else
        echo "Install curl/wget or open the project in Android Studio and generate the Gradle wrapper."
        exit 1
    fi
fi

CLASSPATH=$WRAPPER_JAR
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD=java
fi
exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

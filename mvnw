#!/bin/sh
# Maven Wrapper script
# Downloads and runs Maven if not already available

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven Wrapper..."
    WRAPPER_URL=$(grep "^wrapperUrl=" "$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties" | cut -d'=' -f2-)
    curl -sS -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL" 2>/dev/null || wget -q -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
fi

MAVEN_OPTS="${MAVEN_OPTS:-"-Xmx1024m"}"
export MAVEN_OPTS

exec java $MAVEN_OPTS \
    -classpath "$MAVEN_WRAPPER_JAR" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"

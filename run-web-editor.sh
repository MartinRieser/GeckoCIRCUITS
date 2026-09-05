#!/usr/bin/env bash
# ============================================================================
# GeckoCIRCUITS Web Editor - Launcher for macOS & Linux
# Launches the backend server and opens the web editor in your browser.
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REST_JAR="$SCRIPT_DIR/src/modules/gecko-rest-api/target/gecko-rest-api-1.0.0.jar"
PORT=8080
URL="http://localhost:${PORT}/gecko/"

echo "============================================"
echo "  GeckoCIRCUITS Web Editor"
echo "============================================"

# 1. Check Java (prefer JAVA_HOME if set, otherwise PATH)
JAVA_BIN="java"
if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
elif ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not found in PATH or JAVA_HOME. Please install Java 25 or later."
    exit 1
fi

JAVA_VERSION=$("$JAVA_BIN" -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 25 ]]; then
    echo "[ERROR] Java 25 or later is required (found: $JAVA_VERSION at $JAVA_BIN)."
    echo "Please set JAVA_HOME or update PATH to point to JDK 25+."
    exit 1
fi

# 2. Check and build REST JAR if missing
if [[ ! -f "$REST_JAR" ]]; then
    echo "[INFO] Building GeckoCIRCUITS Web Editor package..."
    mvn -pl src/modules/gecko-rest-api -am package -DskipTests -q
    if [[ $? -ne 0 ]]; then
        echo "[ERROR] Build failed. Please ensure Maven and JDK are installed."
        exit 1
    fi
fi

# 3. Check if server is already running
SERVER_RUNNING=0
if curl -s -f -m 1 "http://localhost:${PORT}/gecko/api/health" > /dev/null 2>&1; then
    SERVER_RUNNING=1
fi

if [[ $SERVER_RUNNING -eq 0 ]]; then
    echo "[INFO] Starting GeckoCIRCUITS Server in background..."
    nohup "$JAVA_BIN" -Xmx2g -jar "$REST_JAR" > /dev/null 2>&1 &
    
    # Wait for server to become ready
    READY=0
    for i in $(seq 1 30); do
        sleep 1
        if curl -s -f -m 1 "http://localhost:${PORT}/gecko/api/health" > /dev/null 2>&1; then
            READY=1
            break
        fi
    done
    if [[ $READY -eq 0 ]]; then
        echo "[WARNING] Server startup timed out, attempting to open anyway..."
    fi
else
    echo "[INFO] Server is already running."
fi

# 4. Launch browser
echo "[INFO] Opening GeckoCIRCUITS Web Editor at $URL ..."

# Try app window mode if Google Chrome is installed on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    if [[ -d "/Applications/Google Chrome.app" ]]; then
        open -na "Google Chrome" --args --app="$URL" --app-window-size=1400,900 || open "$URL"
    else
        open "$URL"
    fi
elif command -v xdg-open &> /dev/null; then
    xdg-open "$URL"
else
    echo "[INFO] Please open your browser and navigate to: $URL"
fi

echo "[INFO] GeckoCIRCUITS Web Editor started."

#!/usr/bin/env bash
# ============================================================================
# GeckoCIRCUITS Web Editor - Launcher for macOS & Linux
# Launches the backend server and opens the web editor in a standalone app window.
# ============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REST_JAR="$SCRIPT_DIR/backend/gecko-rest-api/target/gecko-rest-api-1.0.0.jar"
STATIC_INDEX="$SCRIPT_DIR/backend/gecko-rest-api/src/main/resources/static/index.html"
PORT=8080
URL="http://localhost:${PORT}/gecko/"

echo "============================================"
echo "  GeckoCIRCUITS Web Editor"
echo "============================================"

# Stop running server on specified port
stop_server_on_port() {
    local port="$1"
    local pids=""
    if command -v lsof &>/dev/null; then
        pids=$(lsof -ti :"$port" 2>/dev/null || true)
    elif command -v fuser &>/dev/null; then
        pids=$(fuser "$port"/tcp 2>/dev/null || true)
    fi
    if [[ -n "$pids" ]]; then
        echo "[INFO] Stopping running server on port $port (PID: $pids)..."
        for pid in $pids; do
            kill "$pid" 2>/dev/null || true
        done
        sleep 1
        for pid in $pids; do
            kill -9 "$pid" 2>/dev/null || true
        done
    fi
}

# Check if server is running AND serving the web editor
is_server_ready() {
    curl -s -f -m 1 "http://localhost:${PORT}/gecko/api/health" > /dev/null 2>&1 && \
    curl -s -f -m 1 "http://localhost:${PORT}/gecko/" > /dev/null 2>&1
}

# Parse command line options
REBUILD=0
CIRCUIT_FILE=""

for arg in "$@"; do
    case "$arg" in
        -rebuild|--rebuild|-r|--r)
            REBUILD=1
            ;;
        --stop|-stop)
            stop_server_on_port "$PORT"
            echo "[INFO] Server stopped."
            exit 0
            ;;
        *)
            if [[ -z "$CIRCUIT_FILE" ]]; then
                CIRCUIT_FILE="$arg"
            fi
            ;;
    esac
done

# 1. Check Java (prefer JAVA_HOME if set, otherwise PATH or common JDK 25 locations)
check_version() {
    local bin="$1"
    [[ -x "$bin" ]] && "$bin" -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1
}

JAVA_BIN=""
if [[ -n "$JAVA_HOME" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
elif command -v java &> /dev/null; then
    JAVA_BIN="java"
fi

JAVA_VERSION=""
if [[ -n "$JAVA_BIN" ]]; then
    JAVA_VERSION=$(check_version "$JAVA_BIN")
fi

if [[ -z "$JAVA_VERSION" || "$JAVA_VERSION" -lt 25 ]]; then
    for candidate in \
        "/opt/homebrew/opt/openjdk/bin/java" \
        "/opt/homebrew/opt/openjdk@25/bin/java" \
        "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home/bin/java" \
        "/Library/Java/JavaVirtualMachines"/*/Contents/Home/bin/java \
        "/usr/local/opt/openjdk/bin/java" \
        "/usr/local/opt/openjdk@25/bin/java" \
        /usr/lib/jvm/java-25-openjdk*/bin/java \
        /usr/lib/jvm/jdk-25*/bin/java \
        /usr/lib/jvm/*25*/bin/java \
        /usr/java/jdk-25*/bin/java \
        /opt/java/*25*/bin/java \
        "$HOME/.sdkman/candidates/java"/*25*/bin/java \
        "$HOME/.jdks"/jdk-25*/bin/java; do
        if [[ -x "$candidate" ]]; then
            cand_ver=$(check_version "$candidate")
            if [[ -n "$cand_ver" && "$cand_ver" -ge 25 ]]; then
                JAVA_BIN="$candidate"
                JAVA_VERSION="$cand_ver"
                export JAVA_HOME="$(cd "$(dirname "$candidate")/.." && pwd)"
                break
            fi
        fi
    done
fi

if [[ -z "$JAVA_VERSION" || "$JAVA_VERSION" -lt 25 ]]; then
    echo "[ERROR] Java 25 or later is required (found: ${JAVA_VERSION:-none} at ${JAVA_BIN:-PATH})."
    echo "Please set JAVA_HOME or update PATH to point to JDK 25+."
    exit 1
fi

echo "[INFO] Using Java $JAVA_VERSION ($JAVA_BIN)"

# Configure Guice to use child class loaders instead of deprecated sun.misc.Unsafe
if [[ -z "$MAVEN_OPTS" || "$MAVEN_OPTS" != *"guice_custom_class_loading"* ]]; then
    export MAVEN_OPTS="-Dguice_custom_class_loading=CHILD ${MAVEN_OPTS:-}"
fi

if [[ -n "$CIRCUIT_FILE" ]]; then
    echo "[INFO] Target circuit: $CIRCUIT_FILE"
    echo "[INFO] In the web editor window, select File > Open or drag-and-drop the file onto the canvas."
fi

# 2. Build or Rebuild if requested or if missing
if [[ $REBUILD -eq 1 ]]; then
    stop_server_on_port "$PORT"
    echo "[INFO] Building latest frontend static assets..."
    if [[ -f "$SCRIPT_DIR/frontend/package.json" ]]; then
        (cd "$SCRIPT_DIR/frontend" && npm run build:spring)
    fi
    echo "[INFO] Packaging GeckoCIRCUITS REST JAR..."
    mvn -pl backend/gecko-rest-api -am package -DskipTests -q
    if [[ $? -ne 0 ]]; then
        echo "[ERROR] Build failed. Please ensure Maven and JDK are installed."
        exit 1
    fi
else
    # Ensure frontend static assets exist
    if [[ ! -f "$STATIC_INDEX" ]]; then
        echo "[INFO] Frontend static assets missing. Building frontend..."
        if [[ -f "$SCRIPT_DIR/frontend/package.json" ]]; then
            (cd "$SCRIPT_DIR/frontend" && npm run build:spring)
        fi
    fi

    # Ensure backend JAR exists
    if [[ ! -f "$REST_JAR" ]]; then
        echo "[INFO] Building GeckoCIRCUITS Web Editor package..."
        mvn -pl backend/gecko-rest-api -am package -DskipTests -q
        if [[ $? -ne 0 ]]; then
            echo "[ERROR] Build failed. Please ensure Maven and JDK are installed."
            exit 1
        fi
    fi
fi

# 3. Check if server is already running
if is_server_ready; then
    echo "[INFO] Server is already running and ready."
else
    # In case port 8080 is used by a stale/broken process, stop it
    stop_server_on_port "$PORT"

    echo "[INFO] Starting GeckoCIRCUITS Server in background..."
    mkdir -p "$SCRIPT_DIR/logs"
    nohup "$JAVA_BIN" -Duser.language=en -Duser.country=US -Xmx2g -jar "$REST_JAR" > "$SCRIPT_DIR/logs/gecko-web-server.log" 2>&1 &

    # Wait for server to become ready
    READY=0
    for i in $(seq 1 30); do
        sleep 1
        if is_server_ready; then
            READY=1
            break
        fi
    done
    if [[ $READY -eq 0 ]]; then
        echo "[WARNING] Server startup timed out, attempting to open anyway..."
    fi
fi

# 4. Launch in Native App Window mode
echo "[INFO] Launching GeckoCIRCUITS window..."

LAUNCHED=0

if [[ "$OSTYPE" == "darwin"* ]]; then
    PROFILE_DIR="$HOME/Library/Application Support/GeckoCIRCUITS/web-editor-profile"
    mkdir -p "$PROFILE_DIR"

    MACOS_BROWSERS=(
        "/Applications/Google Chrome.app"
        "$HOME/Applications/Google Chrome.app"
        "/Applications/Microsoft Edge.app"
        "$HOME/Applications/Microsoft Edge.app"
        "/Applications/Brave Browser.app"
        "$HOME/Applications/Brave Browser.app"
        "/Applications/Chromium.app"
        "$HOME/Applications/Chromium.app"
    )

    for browser_app in "${MACOS_BROWSERS[@]}"; do
        if [[ -d "$browser_app" ]]; then
            app_name=$(basename "$browser_app" .app)
            echo "[INFO] Launching in standalone app window using $app_name..."
            open -na "$browser_app" --args --app="$URL" --user-data-dir="$PROFILE_DIR" --window-size=1400,900
            LAUNCHED=1
            break
        fi
    done

    if [[ $LAUNCHED -eq 0 ]]; then
        echo "[INFO] Fallback: opening in default browser..."
        open "$URL"
    fi
elif command -v xdg-open &> /dev/null || [[ "$OSTYPE" == "linux"* ]]; then
    PROFILE_DIR="$HOME/.config/geckocircuits/web-editor-profile"
    mkdir -p "$PROFILE_DIR"

    for bin in google-chrome google-chrome-stable chromium chromium-browser brave-browser microsoft-edge-stable; do
        if command -v "$bin" &> /dev/null; then
            echo "[INFO] Launching in standalone app window using $bin..."
            nohup "$bin" --app="$URL" --user-data-dir="$PROFILE_DIR" --window-size=1400,900 > /dev/null 2>&1 &
            LAUNCHED=1
            break
        fi
    done

    if [[ $LAUNCHED -eq 0 ]]; then
        if command -v xdg-open &> /dev/null; then
            xdg-open "$URL"
        else
            echo "[INFO] Please open your browser and navigate to: $URL"
        fi
    fi
else
    echo "[INFO] Please open your browser and navigate to: $URL"
fi

echo "[INFO] GeckoCIRCUITS Web Editor started."

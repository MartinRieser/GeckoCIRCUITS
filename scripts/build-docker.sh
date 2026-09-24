#!/bin/bash
# Build Docker image for GeckoCIRCUITS REST API
# Uses docker/Dockerfile.api (multi-stage, context: backend) via docker/docker-compose.yml

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "========================================"
echo "Building GeckoCIRCUITS REST API Docker image"
echo "========================================"

# Image tag
IMAGE_NAME="geckocircuits/rest-api"
# Module version from rest-api pom (first <version> after the parent block)
VERSION="$(awk '/<\/parent>/{f=1;next} f && /<version>/{gsub(/.*<version>|<\/version>.*/,""); print; exit}' backend/gecko-rest-api/pom.xml)"
TAG="${IMAGE_NAME}:${VERSION}"
LATEST_TAG="${IMAGE_NAME}:latest"

echo ""
echo "Building image: $TAG"
echo ""

# Build using docker-compose (ensures consistent build)
docker compose -f docker/docker-compose.yml build gecko-api

# Tag the built (latest) image with the module version
docker tag "$LATEST_TAG" "$TAG"

echo ""
echo "========================================"
echo "Build complete!"
echo "========================================"
echo "Image tags:"
echo "  - $TAG"
echo "  - $LATEST_TAG"
echo ""
echo "Image size:"
docker images "$IMAGE_NAME" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
echo ""
echo "To run the container:"
echo "  docker compose -f docker/docker-compose.yml up -d"
echo ""
echo "Or manually:"
echo "  docker run -p 8080:8080 $TAG"
echo ""

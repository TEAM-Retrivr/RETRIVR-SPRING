#!/bin/bash
set -e

case "$1" in
  build)
    ./gradlew bootJar
    ;;
  up)
    docker compose up -d
    ;;
  rebuild)
    ./gradlew bootJar
    docker compose up -d --build
    ;;
  down)
    docker compose down
    ;;
  logs)
    docker compose logs -f app
    ;;
  prune)
    docker builder prune -f
    ;;
  clean)
    docker system prune -f
    ;;
  *)
    echo "Usage: ./execute.sh {build|up|rebuild|down|logs|prune|clean}"
    ;;
esac

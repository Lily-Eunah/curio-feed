#!/bin/bash
set -e

# Path to the deployment directory on Oracle VM
cd /opt/curiofeed

# Pull the latest changes. Set DEPLOY_BRANCH env var to deploy a non-main branch.
# This script is intended for production use after feature branches are merged to main.
BRANCH=${DEPLOY_BRANCH:-main}
git pull origin "$BRANCH"

# Rebuild and restart the backend container
docker compose -f infra/docker-compose.prod.yml build
docker compose -f infra/docker-compose.prod.yml up -d

# Prune unused docker images to save space
docker image prune -f

# Check container status
docker compose -f infra/docker-compose.prod.yml ps

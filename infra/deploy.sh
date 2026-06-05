#!/bin/bash
set -e

# Path to the deployment directory on Oracle VM
cd /opt/curiofeed

# Pull the latest changes from main branch
git pull origin main

# Rebuild and restart the backend container
docker compose -f infra/docker-compose.prod.yml build
docker compose -f infra/docker-compose.prod.yml up -d

# Prune unused docker images to save space
docker image prune -f

# Check container status
docker compose -f infra/docker-compose.prod.yml ps

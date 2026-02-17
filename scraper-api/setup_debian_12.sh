#!/bin/bash
# Debian 12 (Bookworm) Environment Setup Script
# This script prepares a fresh Debian 12 server for running the scraping API.
# It installs Node.js 20, system dependencies for Puppeteer, and Process Manager (PM2).

set -e

echo "--------------------------------------------------"
echo "🚀 Starting Setup for Debian 12 (Bookworm)..."
echo "--------------------------------------------------"

# 1. Update System
echo "📦 Updating system packages..."
sudo apt-get update && sudo apt-get upgrade -y

# 2. Install Node.js 20 LTS
if ! command -v node &> /dev/null; then
    echo "🟢 Installing Node.js 20 (LTS)..."
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y nodejs build-essential
else
    echo "✅ Node.js is already installed: $(node -v)"
fi

# 3. Install Libraries Required for Puppeteer/Chrome
echo "🔧 Installing system dependencies for Puppeteer..."
# Comprehensive list for Debian 12
sudo apt-get install -y \
    ca-certificates \
    fonts-liberation \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libc6 \
    libcairo2 \
    libcups2 \
    libdbus-1-3 \
    libexpat1 \
    libfontconfig1 \
    libgbm1 \
    libgcc1 \
    libglib2.0-0 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libstdc++6 \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxi6 \
    libxrandr2 \
    libxrender1 \
    libxss1 \
    libxtst6 \
    lsb-release \
    wget \
    xdg-utils

# 4. Install Project Dependencies
echo "📚 Installing project dependencies..."
# We use 'clean-install' if package-lock exists for stability
if [ -f "package-lock.json" ]; then
    npm ci
else
    npm install
fi

# 5. Global Tools (PM2)
echo "⚡ Installing PM2 for process management..."
sudo npm install -g pm2

echo "--------------------------------------------------"
echo "✅ Setup Complete!"
echo "--------------------------------------------------"
echo "To start the application in background:"
echo "   pm2 start ecosystem.config.js"
echo "   (or: pm2 start server.js --name scraper-api)"
echo "--------------------------------------------------"

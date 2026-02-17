# Base on Debian 12 (Bookworm) via Node 20
FROM node:20-bookworm-slim

# Install latest chrome dev package and fonts to support major charsets (Chinese, Japanese, Arabic, Hebrew, Thai and a few others)
# Note: this installs the necessary libs to make the bundled version of Chromium that Puppeteer
# installs, work.
RUN apt-get update \
    && apt-get install -y wget gnupg \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/googlechrome-linux-keyring.gpg \
    && sh -c 'echo "deb [arch=amd64 signed-by=/usr/share/keyrings/googlechrome-linux-keyring.gpg] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
    && apt-get update \
    && apt-get install -y google-chrome-stable fonts-ipafont-gothic fonts-wqy-zenhei fonts-thai-tlwg fonts-kacst fonts-freefont-ttf libxss1 \
      --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# If running Docker, we can skip the local chromium download and use the installed google-chrome-stable
# But for maximum compatibility with specific Puppeteer versions, we usually let it download its preferred revision.
# However, to keep image small and stable, we can point to the system one if versions align.
# For this setup, trusting Puppeteer's downloaded chrome is safer for "no extra steps" stability.
# But we need the libs, which are installed above.

WORKDIR /usr/src/app

# Copy package files
COPY package*.json ./

# Install dependencies
# Using npm ci for reproducible builds
RUN npm ci --only=production

# Copy app source
COPY . .

# Expose port
EXPOSE 3000

# Start command
CMD [ "node", "server.js" ]

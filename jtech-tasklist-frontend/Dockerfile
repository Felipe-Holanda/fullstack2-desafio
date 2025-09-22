# Frontend (Vue 3 + Vite) production image
# Multi-stage: build with Node, serve with Nginx

FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies first (better cache)
COPY package.json package-lock.json* pnpm-lock.yaml* yarn.lock* .npmrc* ./
RUN npm ci || npm install

# Copy source and build
COPY . .
RUN npm run build

# --- Runtime stage ---
FROM nginx:alpine AS runtime
WORKDIR /usr/share/nginx/html

# Remove default Nginx static assets
RUN rm -rf ./*

# Copy built assets
COPY --from=build /app/dist/ ./

# Nginx config for SPA fallback
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

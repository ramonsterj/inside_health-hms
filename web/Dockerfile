# Stage 1: Build
FROM --platform=linux/amd64 node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Serve (non-root nginx)
FROM --platform=linux/amd64 nginxinc/nginx-unprivileged:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

ENV BACKEND_URL=http://backend.internal:8081
ENV NGINX_ENVSUBST_FILTER=BACKEND_URL

EXPOSE 8080

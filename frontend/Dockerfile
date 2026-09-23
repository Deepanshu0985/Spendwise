FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Serves the static build only; the edge Caddy (../Caddyfile) handles TLS and
# reverse-proxies /api to the backend container. This container never faces the internet directly.
FROM caddy:2-alpine
COPY --from=build /build/dist /srv
COPY Caddyfile.static /etc/caddy/Caddyfile
EXPOSE 80

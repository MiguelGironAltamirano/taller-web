FROM node:24-alpine AS build
WORKDIR /app
RUN corepack enable
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginx:1.28-alpine
ENV BACKEND_UPSTREAM=http://backend:8080 \
    NGINX_ENVSUBST_FILTER='^(BACKEND_UPSTREAM|NGINX_RESOLVER)$'
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY nginx-resolver.envsh /docker-entrypoint.d/15-nginx-resolver.envsh
RUN chmod +x /docker-entrypoint.d/15-nginx-resolver.envsh
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

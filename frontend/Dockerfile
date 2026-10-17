FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY nginx/05-api-upstream.envsh /docker-entrypoint.d/05-api-upstream.envsh
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
ENV PORT=8080 \
    API_UPSTREAM=http://backend:8080 \
    NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1 \
    NGINX_ENVSUBST_FILTER="^(PORT|API_UPSTREAM|NGINX_LOCAL_RESOLVERS)$"
EXPOSE 8080

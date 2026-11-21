FROM node:22-bookworm-slim AS build
ARG APP_DIR
WORKDIR /src
COPY ${APP_DIR}/package*.json ./
RUN npm ci
COPY ${APP_DIR}/ ./
RUN npm run build

FROM nginx:alpine
COPY --from=build /src/dist/ /usr/share/nginx/html/
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

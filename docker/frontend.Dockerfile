FROM node:20-alpine AS build
WORKDIR /app
COPY admin-panel/package*.json ./
RUN npm ci
COPY admin-panel/ .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/psychosim-admin-panel/browser /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

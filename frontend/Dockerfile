# syntax=docker/dockerfile:1

# Estagio 1: gera os arquivos estaticos com o Vite
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# o Vite grava a URL da API no JS durante o build; a URL e a que o navegador enxerga
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Estagio 2: Nginx servindo so a pasta dist
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

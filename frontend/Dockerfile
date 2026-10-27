# Stage 1: Build React static assets
FROM node:20-alpine AS build
WORKDIR /app

ARG VITE_API_URL
ARG VITE_ML_API_URL
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_ML_API_URL=$VITE_ML_API_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2: Serve using Nginx
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

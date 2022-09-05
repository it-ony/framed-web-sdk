FROM nginx:alpine

WORKDIR /app
COPY index.html .
COPY dist/ ./dist

COPY ./nginx.conf /etc/nginx/nginx.conf

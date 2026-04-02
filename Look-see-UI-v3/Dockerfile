### STAGE 1: Build ###
FROM node:18.19.1 AS build
RUN npm install -g npm@10.2.4
RUN npm install -g typescript@5.5.4
RUN npm install -g @angular/cli@18.1.4
WORKDIR /usr/src/app
COPY package.json package-lock.json ./
RUN npm install --force
COPY . .
# Build happens here - environment variables from the container will be read by webpack
# and process.env references in environment.ts will be replaced with actual values
# Environment variables are passed from Terraform when the container runs
RUN npm run build:direct

### STAGE 2: Run ###
FROM nginx:latest
COPY nginx.cloudrun.conf /etc/nginx/nginx.conf
COPY --from=build /usr/src/app/dist/look-see-ui-v3/browser /usr/share/nginx/html
RUN mkdir -p /var/www/assets
COPY --from=build /usr/src/app/dist/look-see-ui-v3/browser/assets /var/www/assets
COPY scripts/generate-env.sh /usr/local/bin/generate-env.sh
COPY scripts/generate-config.sh /usr/local/bin/generate-config.sh
RUN chmod +x /usr/local/bin/generate-env.sh /usr/local/bin/generate-config.sh

# Generate .env file from system environment variables, then generate config.json from .env
# This runs before nginx starts serving the Angular app
CMD /usr/local/bin/generate-env.sh && /usr/local/bin/generate-config.sh && nginx -g 'daemon off;'
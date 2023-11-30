# Stage 1 - 
FROM node:16.19-alpine as base
LABEL base-for=ui
RUN apk update \
    && apk add --no-cache --virtual build-dependencies \
    git
RUN mkdir -p /usr/src/app && mkdir -p /opt/mount
WORKDIR /usr/src/app
COPY tsconfig.json /usr/src/app/tsconfig.json
COPY package.json /usr/src/app/package.json
COPY package-lock.json /usr/src/app/package-lock.json
ENV PATH /usr/src/app/node_modules/.bin:$PATH
ENV NODE_OPTIONS="--max_old_space_size=8192"
RUN npm config set unsafe-perm true
ARG HARNESS_GITHUB_TOKEN
RUN npm config set @harness:registry=https://npm.pkg.github.com
RUN npm config set //npm.pkg.github.com/:_authToken=${HARNESS_GITHUB_TOKEN}
RUN npm cache clean --force && npm --version && npm install --legacy-peer-deps

# Stage 2 - Compile Less & NPM Test
FROM base as tester
ARG build
LABEL build=${build}
LABEL stage=ui-tester
RUN mkdir -p /usr/src/app/src/assets
COPY style-guide /usr/src/app/style-guide
COPY src/assets/sass /usr/src/app/src/assets/sass
RUN lessc /usr/src/app/style-guide/less/main.less /usr/src/app/src/assets/sass/main.css && \
    CI=true SKIP_PREFLIGHT_CHECK=true npm test -- --coverage --watchAll=false --verbose=true --coverageReporters=cobertura || true

# Stage 4 - NPM build
FROM tester as builder
ARG ga_tracker
ARG bug_snag
ENV REACT_APP_GA_TRACKER $ga_tracker
ENV REACT_APP_BUG_SNAG $bug_snag
LABEL ui-stage=builder
LABEL build=${build}
COPY tsconfig.json /usr/src/app/tsconfig.json
COPY babel.config.js /usr/src/app/babel.config.js
COPY .npmrc /usr/src/app/.npmrc
COPY .env /usr/src/app/.env
COPY src /usr/src/app/src
COPY config /usr/src/app/config
WORKDIR /usr/src/app
RUN npm version $build
RUN npm run build

# Stage 5 - the production environment
FROM nginx:alpine as deploy
LABEL stage=ui-deploy
RUN mkdir /opt/scripts

RUN rm -rf /usr/share/nginx/html/*

COPY --from=builder /usr/src/app/dist /usr/share/nginx/html
RUN mkdir /usr/share/nginx/html/static
# COPY --from=builder /usr/src/app/dist/static /usr/share/nginx/html
COPY src/static /usr/share/nginx/html/static
COPY nginx.conf /etc/nginx/nginx.conf
# COPY default.conf /etc/nginx/conf.d/default.conf
COPY deploy /opt/scripts
RUN sed -i 's/{{ui_version}}/'${build}'/g' /etc/nginx/nginx.conf
RUN chmod 777 /opt/scripts/*.sh
EXPOSE 80
ENTRYPOINT ["./opt/scripts/cmd.sh"]

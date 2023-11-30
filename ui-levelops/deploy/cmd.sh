#!/bin/sh

./opt/scripts/env.sh
ls -l /opt/scripts
cp ./env-config.js /usr/share/nginx/html/ 
nginx -g "daemon off;"

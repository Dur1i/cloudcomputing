#!/bin/bash
set -euo pipefail

chown -R ubuntu:ubuntu /opt/tc-social
chmod 750 /opt/tc-social /opt/tc-social/run_application.sh
chmod 640 /opt/tc-social/websocial.jar

rm -f /etc/nginx/sites-enabled/default
ln -sfn /etc/nginx/sites-available/nginx-tcblog.conf /etc/nginx/sites-enabled/nginx-tcblog.conf

nginx -t
systemctl daemon-reload


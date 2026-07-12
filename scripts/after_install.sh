#!/bin/bash
set -euo pipefail

chown -R ubuntu:ubuntu /opt/tcblog
chmod 750 /opt/tcblog /opt/tcblog/run_application.sh
chmod 640 /opt/tcblog/websocial.jar

rm -f /etc/nginx/sites-enabled/default
ln -sfn /etc/nginx/sites-available/nginx-tcblog.conf /etc/nginx/sites-enabled/nginx-tcblog.conf

nginx -t
systemctl daemon-reload

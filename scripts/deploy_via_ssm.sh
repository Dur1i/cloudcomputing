#!/bin/bash
set -euo pipefail

BUNDLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

systemctl stop tc-social.service 2>/dev/null || true

mkdir -p /opt/tc-social /etc/tc-social
install -o ubuntu -g ubuntu -m 640 "$BUNDLE_DIR/websocial.jar" /opt/tc-social/websocial.jar
install -o ubuntu -g ubuntu -m 750 "$BUNDLE_DIR/scripts/run_application.sh" /opt/tc-social/run_application.sh
install -o root -g root -m 644 "$BUNDLE_DIR/deploy/tc-social.service" /etc/systemd/system/tc-social.service
install -o root -g root -m 644 "$BUNDLE_DIR/deploy/nginx-tcblog.conf" /etc/nginx/sites-available/nginx-tcblog.conf

rm -f /etc/nginx/sites-enabled/default
ln -sfn /etc/nginx/sites-available/nginx-tcblog.conf /etc/nginx/sites-enabled/nginx-tcblog.conf

nginx -t
systemctl daemon-reload
systemctl enable nginx tc-social.service
systemctl restart nginx
systemctl restart tc-social.service

for attempt in $(seq 1 45); do
    if curl --fail --silent --show-error http://127.0.0.1:8080/login >/dev/null; then
        curl --fail --silent --show-error http://127.0.0.1/login >/dev/null
        echo "TCBlog deployment completed successfully"
        exit 0
    fi
    sleep 2
done

journalctl -u tc-social.service -n 150 --no-pager
exit 1

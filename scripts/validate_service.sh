#!/bin/bash
set -euo pipefail

for attempt in $(seq 1 30); do
    if curl --fail --silent --show-error http://127.0.0.1:8080/login >/dev/null; then
        curl --fail --silent --show-error http://127.0.0.1/login >/dev/null
        exit 0
    fi
    sleep 2
done

journalctl -u tcblog.service -n 100 --no-pager
exit 1

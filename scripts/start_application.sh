#!/bin/bash
set -euo pipefail

systemctl enable nginx tcblog.service
systemctl restart nginx
systemctl restart tcblog.service

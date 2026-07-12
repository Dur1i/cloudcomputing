#!/bin/bash
set -euo pipefail

systemctl enable nginx tc-social.service
systemctl restart nginx
systemctl restart tc-social.service


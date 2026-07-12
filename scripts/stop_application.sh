#!/bin/bash
set -euo pipefail

systemctl stop tc-social.service 2>/dev/null || true


#!/bin/bash
systemctl daemon-reload
systemctl enable tc-social
systemctl restart tc-social
#!/bin/bash
set -euo pipefail

REGION="ap-southeast-1"

read_parameter() {
    aws ssm get-parameter \
        --region "$REGION" \
        --name "$1" \
        --with-decryption \
        --query 'Parameter.Value' \
        --output text
}

export DB_URL="$(read_parameter /tcblog/prod/db-url)"
export DB_USERNAME="$(read_parameter /tcblog/prod/db-username)"
export DB_PASSWORD="$(read_parameter /tcblog/prod/db-password)"
export AWS_REGION="$(read_parameter /tcblog/prod/aws-region)"
export S3_BUCKET="$(read_parameter /tcblog/prod/s3-bucket)"

exec /usr/bin/java \
    -Xms128m \
    -Xmx320m \
    -XX:MaxMetaspaceSize=160m \
    -XX:MaxDirectMemorySize=64m \
    -XX:+UseSerialGC \
    -XX:+ExitOnOutOfMemoryError \
    -jar /opt/tc-social/websocial.jar \
    --spring.profiles.active=prod


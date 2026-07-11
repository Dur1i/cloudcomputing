#!/bin/bash
mkdir -p /opt/tc-social
mv /opt/tc-social/websocial-1.0-SNAPSHOT.jar /opt/tc-social/websocial.jar
chown -R ec2-user:ec2-user /opt/tc-social
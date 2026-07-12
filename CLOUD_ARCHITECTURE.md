# TCBlog Cloud Computing Architecture

## 1. Current Deployment Flow

```text
User
  -> ALB DNS
  -> Application Load Balancer
  -> Target Group
  -> EC2 t3.micro
  -> Nginx
  -> Spring Boot application
  -> Amazon RDS MySQL

Spring Boot application
  -> Amazon S3 media bucket

CloudWatch
  -> Alarms
  -> SNS topic
  -> Email notification

Administrator
  -> AWS Systems Manager Session Manager
  -> EC2
```

## 2. Services Used

| Service | Role in project | Current status |
|---|---|---|
| EC2 t3.micro | Runs Ubuntu, Nginx, Java, and the Spring Boot app | Done |
| Application Load Balancer | Public entry point for the web app | Done |
| Target Group | Routes ALB traffic to EC2 on HTTP port 80 | Done |
| Nginx | Reverse proxy from port 80 to Spring Boot port 8080 | Done |
| Spring Boot | Main social platform backend and server-rendered UI | Done |
| Amazon RDS MySQL | Managed database for users, posts, stories, chat, likes, comments | Done |
| Amazon S3 | Stores post media, stories, avatars, chat files, and deployment jar | Done |
| IAM Role | Gives EC2 permission to access S3, SSM, and Parameter Store | Done |
| Parameter Store | Stores production environment values such as DB URL and S3 bucket | Done |
| Session Manager | Access EC2 without SSH key/public SSH port | Done |
| CloudWatch Dashboard | Visualizes EC2, ALB, and RDS metrics | Done |
| CloudWatch Alarms | Monitors EC2 CPU, ALB 5xx errors, and RDS CPU | Done |
| SNS | Sends alarm notifications to email | Done |
| Route 53 Domain | Custom domain, optional for final demo | Later |
| ACM | HTTPS certificate, optional after buying/using a domain | Later |
| CI/CD | Automated deployment from GitHub | Later |

## 3. Runtime Architecture

The user opens the ALB DNS URL in a browser. The Application Load Balancer receives HTTP traffic on port 80 and forwards it to the target group. The target group sends traffic to the EC2 instance. On EC2, Nginx receives the request and proxies it to the Spring Boot application running on port 8080.

Spring Boot handles authentication, posts, stories, comments, likes, profiles, messaging, and realtime WebSocket features. Application data is stored in Amazon RDS MySQL. Uploaded media is stored in Amazon S3 instead of the EC2 local disk, which makes the app more cloud-native and prevents uploaded files from being lost when the EC2 instance is replaced.

## 4. S3 Media Layout

| Feature | S3 folder |
|---|---|
| Post images/videos | `posts/` |
| Stories | `stories/` |
| Profile avatars | `avatars/` |
| Chat files | `chat/` |
| Generic upload endpoint | `uploads/` |
| Manual deployment jar | `deploy/` |

## 5. Production Configuration

The EC2 service runs the jar from:

```text
/opt/tcblog/websocial.jar
```

The systemd service is:

```text
tcblog.service
```

The environment file is:

```text
/opt/tcblog/.env
```

Required environment values:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<rds-endpoint>:3306/websocial_after?sslMode=REQUIRED&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=tcblogadmin
DB_PASSWORD=<rds-password>
AWS_REGION=ap-southeast-1
S3_BUCKET=tcblog-media-51379345
SERVER_PORT=8080
COOKIE_SECURE=false
```

`COOKIE_SECURE=false` is used because the current demo uses HTTP through the ALB DNS. When HTTPS is added with ACM, change it to:

```env
COOKIE_SECURE=true
```

## 6. Monitoring Setup

### SNS

Topic:

```text
tcblog-alerts
```

Subscription:

```text
Email subscription confirmed
```

### CloudWatch Alarms

| Alarm | Condition |
|---|---|
| `tcblog-ec2-high-cpu` | EC2 CPUUtilization > 70 for 5 minutes |
| `tcblog-alb-5xx-errors` | ALB Target 5XX count > 0 within 5 minutes |
| `tcblog-rds-high-cpu` | RDS CPUUtilization > 70 for 5 minutes |

### CloudWatch Dashboard

Dashboard:

```text
tcblog-dashboard
```

Widgets:

```text
EC2 CPU Utilization
ALB Request Count
ALB Target 5XX Errors
RDS CPU Utilization
RDS Database Connections
```

## 7. Demo Checklist

1. Open the ALB DNS URL and show the web app.
2. Login with an existing user.
3. Create a post with an image.
4. Show the uploaded image object in S3 under `posts/`.
5. Create a story and show the object in S3 under `stories/`.
6. Update avatar and show the object in S3 under `avatars/`.
7. Open CloudWatch Dashboard and show EC2, ALB, and RDS metrics.
8. Open CloudWatch Alarms and show the three alarms.
9. Open SNS topic and show confirmed email subscription.
10. Open Session Manager and explain EC2 access without SSH.

## 8. Cost Control Notes

For waiting until report day, stop or scale down expensive runtime resources:

| Resource | Suggested action |
|---|---|
| EC2 / Auto Scaling Group | Set desired capacity to 0 if not demoing |
| RDS | Stop database when not needed, then start before demo |
| ALB | Delete only if you want to avoid ALB hourly cost |
| S3 | Keep bucket, storage cost is low for small media |
| CloudWatch alarms/dashboard | Can keep; monitor cost if many metrics/logs are added |
| Domain/ACM | Skip until final demo if budget is tight |

## 9. Later Improvements

| Feature | Benefit |
|---|---|
| Route 53 custom domain | Cleaner URL for presentation |
| ACM HTTPS certificate | Secure HTTPS access |
| HTTP to HTTPS redirect | Production-like web behavior |
| CI/CD with CodeBuild + SSM | Automated deployment without CodeDeploy |
| CloudWatch log groups for app logs | Easier debugging from AWS Console |
| RDS backup/snapshot plan | Safer database operations |


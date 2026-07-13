from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import textwrap
import html


OUT = Path("report-diagrams")
OUT.mkdir(exist_ok=True)

W, H = 1600, 920
BG = "#f8fafc"
INK = "#102033"
MUTED = "#5b6b80"
BORDER = "#bfd0e6"
BLUE = "#2563eb"
BLUE_LIGHT = "#dbeafe"
GREEN = "#16a34a"
GREEN_LIGHT = "#dcfce7"
AMBER = "#d97706"
AMBER_LIGHT = "#fef3c7"
PURPLE = "#7c3aed"
PURPLE_LIGHT = "#ede9fe"
RED = "#dc2626"
RED_LIGHT = "#fee2e2"
GRAY = "#e5e7eb"


def font(size=28, bold=False):
    candidates = [
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
    ]
    for path in candidates:
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default()


F_TITLE = font(40, True)
F_SUB = font(22)
F_NODE = font(23, True)
F_SMALL = font(19)
F_TINY = font(17)


def wrap_text(text, max_chars):
    lines = []
    for part in str(text).split("\n"):
        lines.extend(textwrap.wrap(part, width=max_chars) or [""])
    return lines


def draw_text_center(draw, box, text, fnt, fill=INK, max_chars=18, line_gap=4):
    x, y, w, h = box
    lines = wrap_text(text, max_chars)
    heights = [draw.textbbox((0, 0), line, font=fnt)[3] for line in lines]
    total = sum(heights) + line_gap * (len(lines) - 1)
    yy = y + (h - total) / 2
    for line, lh in zip(lines, heights):
        bbox = draw.textbbox((0, 0), line, font=fnt)
        tw = bbox[2] - bbox[0]
        draw.text((x + (w - tw) / 2, yy), line, font=fnt, fill=fill)
        yy += lh + line_gap


def rounded(draw, box, fill, outline=BORDER, radius=22, width=3):
    x, y, w, h = box
    draw.rounded_rectangle([x, y, x + w, y + h], radius=radius, fill=fill, outline=outline, width=width)


def node(draw, box, title, subtitle="", fill=BLUE_LIGHT, outline=BLUE, icon=None):
    rounded(draw, box, fill, outline)
    x, y, w, h = box
    if icon:
        draw.ellipse([x + 16, y + 18, x + 54, y + 56], fill="white", outline=outline, width=2)
        draw_text_center(draw, (x + 16, y + 18, 38, 38), icon, font(18, True), fill=outline, max_chars=2)
        tx = x + 66
        tw = w - 78
    else:
        tx = x + 18
        tw = w - 36
    if subtitle:
        draw_text_center(draw, (tx, y + 18, tw, 34), title, F_NODE, max_chars=max(12, tw // 14))
        draw_text_center(draw, (tx, y + 56, tw, h - 68), subtitle, F_SMALL, fill=MUTED, max_chars=max(14, tw // 11))
    else:
        draw_text_center(draw, (tx, y, tw, h), title, F_NODE, max_chars=max(12, tw // 13))


def arrow(draw, start, end, color=INK, width=4, label=None):
    x1, y1 = start
    x2, y2 = end
    draw.line([x1, y1, x2, y2], fill=color, width=width)
    import math
    ang = math.atan2(y2 - y1, x2 - x1)
    size = 16
    pts = [
        (x2, y2),
        (x2 - size * math.cos(ang - 0.45), y2 - size * math.sin(ang - 0.45)),
        (x2 - size * math.cos(ang + 0.45), y2 - size * math.sin(ang + 0.45)),
    ]
    draw.polygon(pts, fill=color)
    if label:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        bbox = draw.textbbox((0, 0), label, font=F_TINY)
        tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
        draw.rounded_rectangle([mx - tw / 2 - 8, my - th / 2 - 6, mx + tw / 2 + 8, my + th / 2 + 6],
                               radius=8, fill=BG, outline=BORDER)
        draw.text((mx - tw / 2, my - th / 2 - 1), label, font=F_TINY, fill=MUTED)


def title(draw, main, sub):
    draw.text((60, 42), main, font=F_TITLE, fill=INK)
    draw.text((62, 92), sub, font=F_SUB, fill=MUTED)


def save(img, name):
    png = OUT / f"{name}.png"
    img.save(png)
    # A simple SVG wrapper embedding the PNG keeps Word insertion flexible.
    svg = OUT / f"{name}.svg"
    import base64
    data = base64.b64encode(png.read_bytes()).decode("ascii")
    svg.write_text(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'
        f'<image href="data:image/png;base64,{data}" width="{W}" height="{H}"/></svg>',
        encoding="utf-8",
    )


def canvas():
    return Image.new("RGB", (W, H), BG), ImageDraw.Draw(Image.new("RGB", (1, 1)))


def make_total_architecture():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "Tổng kiến trúc triển khai TCBlog trên AWS", "CloudFront HTTPS, ALB cân bằng tải 2 EC2, RDS/S3 dùng chung và CI/CD bằng CodeBuild + SSM")
    boxes = {
        "user": (70, 210, 190, 90),
        "cf": (330, 205, 220, 100),
        "alb": (620, 205, 230, 100),
        "tg": (920, 205, 210, 100),
        "ec21": (760, 390, 250, 115),
        "ec22": (1060, 390, 250, 115),
        "rds": (820, 620, 230, 100),
        "s3": (1090, 620, 230, 100),
        "cw": (1380, 390, 170, 115),
        "cicd": (70, 620, 320, 100),
        "ssm": (450, 620, 230, 100),
    }
    node(d, boxes["user"], "User / Browser", "PC, laptop, mobile", GREEN_LIGHT, GREEN, "U")
    node(d, boxes["cf"], "CloudFront", "HTTPS public URL", BLUE_LIGHT, BLUE, "CF")
    node(d, boxes["alb"], "Application Load Balancer", "Public entry point", BLUE_LIGHT, BLUE, "ALB")
    node(d, boxes["tg"], "Target Group", "Health check port 80", BLUE_LIGHT, BLUE, "TG")
    node(d, boxes["ec21"], "EC2 #1", "Nginx + Spring Boot\nUbuntu t3.micro", GREEN_LIGHT, GREEN, "1")
    node(d, boxes["ec22"], "EC2 #2", "Nginx + Spring Boot\nUbuntu t3.micro", GREEN_LIGHT, GREEN, "2")
    node(d, boxes["rds"], "Amazon RDS MySQL", "websocial_after", AMBER_LIGHT, AMBER, "DB")
    node(d, boxes["s3"], "Amazon S3", "media + artifact", AMBER_LIGHT, AMBER, "S3")
    node(d, boxes["cw"], "CloudWatch + SNS", "Dashboard, alarms, email", PURPLE_LIGHT, PURPLE, "M")
    node(d, boxes["cicd"], "GitHub + CodeBuild", "Build Maven JAR\nUpload latest.zip", PURPLE_LIGHT, PURPLE, "CI")
    node(d, boxes["ssm"], "Systems Manager", "Run Command deploy", PURPLE_LIGHT, PURPLE, "SSM")
    arrow(d, (260, 255), (330, 255), label="HTTPS")
    arrow(d, (550, 255), (620, 255))
    arrow(d, (850, 255), (920, 255))
    arrow(d, (1025, 305), (900, 390))
    arrow(d, (1025, 305), (1185, 390))
    arrow(d, (885, 505), (930, 620), label="JDBC")
    arrow(d, (1185, 505), (930, 620))
    arrow(d, (930, 505), (1180, 620), label="media")
    arrow(d, (1185, 505), (1180, 620))
    arrow(d, (390, 670), (450, 670), label="artifact")
    arrow(d, (680, 670), (760, 450), label="deploy")
    arrow(d, (680, 670), (1060, 450))
    arrow(d, (1010, 445), (1380, 445), label="metrics/logs")
    arrow(d, (1310, 445), (1380, 445))
    save(img, "01-tong-kien-truc")


def make_user_flow():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "Luồng người dùng", "Request HTTPS đi qua CloudFront, ALB, Target Group và được phân phối đến một trong hai EC2 healthy")
    xs = [70, 330, 590, 850, 1130]
    y = 260
    nodes = [
        ((xs[0], y, 190, 100), "Browser", "User gửi request", GREEN_LIGHT, GREEN, "U"),
        ((xs[1], y, 210, 100), "CloudFront", "HTTPS edge", BLUE_LIGHT, BLUE, "CF"),
        ((xs[2], y, 220, 100), "ALB", "Load balancing", BLUE_LIGHT, BLUE, "ALB"),
        ((xs[3], y, 220, 100), "Target Group", "2 healthy targets", BLUE_LIGHT, BLUE, "TG"),
    ]
    for b, a, s, fill, out, ico in nodes:
        node(d, b, a, s, fill, out, ico)
    node(d, (1110, 180, 260, 105), "EC2 #1", "Nginx -> Spring Boot", GREEN_LIGHT, GREEN, "1")
    node(d, (1110, 360, 260, 105), "EC2 #2", "Nginx -> Spring Boot", GREEN_LIGHT, GREEN, "2")
    node(d, (760, 590, 230, 100), "RDS MySQL", "Dữ liệu quan hệ", AMBER_LIGHT, AMBER, "DB")
    node(d, (1040, 590, 230, 100), "S3 Bucket", "Ảnh, story, avatar", AMBER_LIGHT, AMBER, "S3")
    arrow(d, (260, 310), (330, 310), label="HTTPS")
    arrow(d, (540, 310), (590, 310))
    arrow(d, (810, 310), (850, 310))
    arrow(d, (1070, 310), (1110, 235), label="route")
    arrow(d, (1070, 310), (1110, 415), label="route")
    arrow(d, (1240, 285), (910, 590), label="SQL")
    arrow(d, (1240, 465), (910, 590))
    arrow(d, (1240, 285), (1155, 590), label="media")
    arrow(d, (1240, 465), (1155, 590))
    # response lane
    d.line([1370, 520, 70, 520], fill=MUTED, width=3)
    d.polygon([(70, 520), (90, 510), (90, 530)], fill=MUTED)
    d.text((555, 535), "Response trả ngược về Browser qua cùng đường đi", font=F_SMALL, fill=MUTED)
    save(img, "02-luong-nguoi-dung")


def make_cicd():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "Luồng CI/CD", "CodeBuild build một artifact và dùng SSM Run Command deploy đồng thời lên cả hai EC2 trong Auto Scaling Group")
    y = 250
    boxes = [
        ((70, y, 190, 100), "Developer", "git push / manual build", GREEN_LIGHT, GREEN, "DEV"),
        ((320, y, 190, 100), "GitHub", "main branch", PURPLE_LIGHT, PURPLE, "GH"),
        ((570, y, 220, 100), "CodeBuild", "mvn package\nzip deploy", PURPLE_LIGHT, PURPLE, "CB"),
        ((850, y, 230, 100), "S3 Artifact", "releases/latest.zip", AMBER_LIGHT, AMBER, "S3"),
        ((1140, y, 230, 100), "SSM Run Command", "deploy script", BLUE_LIGHT, BLUE, "SSM"),
    ]
    for b, a, s, fill, out, ico in boxes:
        node(d, b, a, s, fill, out, ico)
    node(d, (760, 500, 250, 110), "EC2 #1", "copy JAR\nrestart tcblog", GREEN_LIGHT, GREEN, "1")
    node(d, (1060, 500, 250, 110), "EC2 #2", "copy JAR\nrestart tcblog", GREEN_LIGHT, GREEN, "2")
    node(d, (360, 500, 250, 110), "Buildspec", "lấy tất cả InService instances", BLUE_LIGHT, BLUE, "YML")
    for i in range(len(boxes) - 1):
        b1, b2 = boxes[i][0], boxes[i + 1][0]
        arrow(d, (b1[0] + b1[2], b1[1] + 50), (b2[0], b2[1] + 50))
    arrow(d, (680, 350), (485, 500), label="query ASG")
    arrow(d, (1255, 350), (885, 500), label="same command")
    arrow(d, (1255, 350), (1185, 500), label="same command")
    save(img, "03-luong-cicd")


def make_monitoring():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "Luồng Monitoring và cảnh báo", "CloudWatch thu thập metric từ EC2, ALB, RDS rồi kích hoạt alarm và SNS email khi vượt ngưỡng")
    node(d, (90, 230, 220, 105), "EC2 Metrics", "CPU, status check", GREEN_LIGHT, GREEN, "EC2")
    node(d, (90, 390, 220, 105), "ALB Metrics", "RequestCount\nTarget 5XX", BLUE_LIGHT, BLUE, "ALB")
    node(d, (90, 550, 220, 105), "RDS Metrics", "CPU, connections", AMBER_LIGHT, AMBER, "RDS")
    node(d, (440, 360, 250, 115), "CloudWatch Dashboard", "tcblog-dashboard", PURPLE_LIGHT, PURPLE, "D")
    node(d, (800, 360, 250, 115), "CloudWatch Alarm", "CPU high\nALB 5XX\nRDS CPU", RED_LIGHT, RED, "A")
    node(d, (1140, 360, 220, 115), "SNS Topic", "tcblog-alerts", AMBER_LIGHT, AMBER, "SNS")
    node(d, (1420, 360, 130, 115), "Email", "alert", GREEN_LIGHT, GREEN, "@")
    for sy in [282, 442, 602]:
        arrow(d, (310, sy), (440, 418))
    arrow(d, (690, 418), (800, 418), label="threshold")
    arrow(d, (1050, 418), (1140, 418), label="publish")
    arrow(d, (1360, 418), (1420, 418), label="notify")
    save(img, "04-luong-monitoring")


def make_failover():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "Demo Load Balancer và failover", "Khi một EC2 bị stop service, Target Group đánh dấu Unhealthy và ALB chuyển request sang EC2 còn lại")
    node(d, (80, 230, 220, 110), "User", "truy cập web", GREEN_LIGHT, GREEN, "U")
    node(d, (390, 230, 260, 110), "ALB", "nhận request", BLUE_LIGHT, BLUE, "ALB")
    node(d, (760, 160, 280, 115), "EC2 #1", "tcblog stopped\nUnhealthy", RED_LIGHT, RED, "X")
    node(d, (760, 390, 280, 115), "EC2 #2", "tcblog running\nHealthy", GREEN_LIGHT, GREEN, "OK")
    node(d, (1160, 390, 230, 115), "Web vẫn chạy", "ALB route sang target healthy", GREEN_LIGHT, GREEN, "✓")
    arrow(d, (300, 285), (390, 285), label="request")
    # dashed-ish blocked line
    d.line([650, 285, 760, 218], fill=RED, width=4)
    d.line([650, 285, 760, 448], fill=GREEN, width=5)
    d.polygon([(760, 448), (742, 436), (746, 462)], fill=GREEN)
    d.text((675, 200), "bỏ qua target lỗi", font=F_TINY, fill=RED)
    arrow(d, (1040, 448), (1160, 448), label="serve traffic")
    # Steps
    steps = [
        "1. Target Group ban đầu: 2/2 Healthy",
        "2. Dừng service trên EC2 #1: sudo systemctl stop tcblog",
        "3. Target Group: EC2 #1 Unhealthy, EC2 #2 Healthy",
        "4. Web vẫn truy cập được qua ALB/CloudFront",
        "5. Start lại service: sudo systemctl start tcblog -> 2/2 Healthy",
    ]
    y = 660
    for s in steps:
        d.text((120, y), s, font=F_SMALL, fill=INK)
        y += 38
    save(img, "05-demo-load-balancer-failover")


def make_storage():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "Luồng dữ liệu và lưu trữ", "Hai EC2 dùng chung RDS cho dữ liệu quan hệ và S3 cho media, tránh phụ thuộc dữ liệu cục bộ trên từng server")
    node(d, (100, 230, 220, 105), "Spring Boot EC2 #1", "service tcblog", GREEN_LIGHT, GREEN, "1")
    node(d, (100, 430, 220, 105), "Spring Boot EC2 #2", "service tcblog", GREEN_LIGHT, GREEN, "2")
    node(d, (500, 220, 270, 120), "Amazon RDS MySQL", "users, posts, comments,\nlikes, messages", AMBER_LIGHT, AMBER, "DB")
    node(d, (500, 430, 270, 120), "Amazon S3", "posts, stories,\navatars, chat, uploads", AMBER_LIGHT, AMBER, "S3")
    node(d, (980, 220, 250, 120), "Parameter Store", "DB_URL, DB_USERNAME,\nDB_PASSWORD, S3_BUCKET", BLUE_LIGHT, BLUE, "SSM")
    node(d, (980, 430, 250, 120), "IAM Role", "tcblog-ec2-role\nleast privilege", PURPLE_LIGHT, PURPLE, "IAM")
    arrow(d, (320, 282), (500, 280), label="JDBC")
    arrow(d, (320, 482), (500, 280), label="JDBC")
    arrow(d, (320, 282), (500, 490), label="upload media")
    arrow(d, (320, 482), (500, 490), label="upload media")
    arrow(d, (980, 280), (770, 280), label="config")
    arrow(d, (980, 490), (770, 490), label="permission")
    save(img, "06-luong-du-lieu-luu-tru")


if __name__ == "__main__":
    make_total_architecture()
    make_user_flow()
    make_cicd()
    make_monitoring()
    make_failover()
    make_storage()
    print(OUT.resolve())

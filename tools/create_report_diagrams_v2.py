from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math
import textwrap


OUT = Path("report-diagrams-v2")
OUT.mkdir(exist_ok=True)

W, H = 2400, 1350
BG = "#f8fbff"
INK = "#0f172a"
MUTED = "#475569"
BORDER = "#b8c7da"
AWS_BORDER = "#9db4d0"

BLUE, BLUE_BG = "#2563eb", "#eff6ff"
GREEN, GREEN_BG = "#16a34a", "#f0fdf4"
ORANGE, ORANGE_BG = "#f59e0b", "#fff7ed"
PURPLE, PURPLE_BG = "#7c3aed", "#f5f3ff"
CYAN, CYAN_BG = "#0891b2", "#ecfeff"
RED, RED_BG = "#dc2626", "#fef2f2"


def font(size=24, bold=False):
    candidates = [
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for path in candidates:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            pass
    return ImageFont.load_default()


F_TITLE = font(54, True)
F_SUB = font(27)
F_NODE = font(34, True)
F_NODE_SUB = font(24, True)
F_BODY = font(22)
F_SMALL = font(20)
F_STEP = font(19, True)
F_ICON = font(44, True)


def wrap(text, chars):
    result = []
    for part in str(text).split("\n"):
        result.extend(textwrap.wrap(part, width=chars) or [""])
    return result


def draw_center(draw, box, text, fnt, fill=INK, chars=20, gap=6):
    x, y, w, h = box
    wrapped = wrap(text, chars)
    heights = []
    for line in wrapped:
        bb = draw.textbbox((0, 0), line, font=fnt)
        heights.append(bb[3] - bb[1])
    total = sum(heights) + max(0, len(wrapped) - 1) * gap
    yy = y + (h - total) / 2
    for line, lh in zip(wrapped, heights):
        bb = draw.textbbox((0, 0), line, font=fnt)
        tw = bb[2] - bb[0]
        draw.text((x + (w - tw) / 2, yy), line, font=fnt, fill=fill)
        yy += lh + gap


def title(draw, main, sub):
    draw.text((70, 48), main, font=F_TITLE, fill=INK)
    draw.text((74, 122), sub, font=F_SUB, fill=MUTED)


def aws_frame(draw, box, label):
    x, y, w, h = box
    draw.rounded_rectangle([x, y, x + w, y + h], radius=28, fill="white", outline=AWS_BORDER, width=4)
    draw.text((x + 34, y + 30), "aws", font=font(34, True), fill=INK)
    draw.arc([x + 38, y + 72, x + 110, y + 106], 190, 350, fill=ORANGE, width=4)
    draw.text((x + 132, y + 38), label, font=font(34, True), fill=INK)


def node(draw, box, heading, body, fill, outline, icon):
    x, y, w, h = box
    draw.rounded_rectangle([x, y, x + w, y + h], radius=24, fill=fill, outline=outline, width=4)
    draw_center(draw, (x + 20, y + 28, w - 40, 56), icon, F_ICON, fill=outline, chars=10)
    draw_center(draw, (x + 18, y + 98, w - 36, 48), heading, F_NODE, fill=outline, chars=max(10, w // 14))
    draw_center(draw, (x + 24, y + 156, w - 48, h - 178), body, F_BODY, fill=MUTED, chars=max(14, w // 12))


def arrow(draw, start, end, color=BLUE, width=6, dashed=False):
    x1, y1 = start
    x2, y2 = end
    if dashed:
        parts = 22
        for i in range(parts):
            if i % 2 == 0:
                sx = x1 + (x2 - x1) * i / parts
                sy = y1 + (y2 - y1) * i / parts
                ex = x1 + (x2 - x1) * (i + 1) / parts
                ey = y1 + (y2 - y1) * (i + 1) / parts
                draw.line([sx, sy, ex, ey], fill=color, width=width)
    else:
        draw.line([x1, y1, x2, y2], fill=color, width=width)
    angle = math.atan2(y2 - y1, x2 - x1)
    size = 22
    points = [
        (x2, y2),
        (x2 - size * math.cos(angle - 0.45), y2 - size * math.sin(angle - 0.45)),
        (x2 - size * math.cos(angle + 0.45), y2 - size * math.sin(angle + 0.45)),
    ]
    draw.polygon(points, fill=color)


def step(draw, n, text, x, y, color=BLUE, w=250):
    draw.rounded_rectangle([x, y, x + w, y + 54], radius=27, fill="white", outline=color, width=3)
    draw.ellipse([x + 12, y + 11, x + 44, y + 43], fill=color)
    draw_center(draw, (x + 12, y + 11, 32, 32), str(n), F_STEP, fill="white", chars=2)
    draw.text((x + 56, y + 15), text, font=F_STEP, fill=INK)


def note(draw, text, x, y, w=360, color=BLUE):
    draw.rounded_rectangle([x, y, x + w, y + 82], radius=16, fill="#ffffff", outline=color, width=2)
    draw_center(draw, (x + 18, y + 10, w - 36, 62), text, F_SMALL, fill=MUTED, chars=max(20, w // 11))


def save(img, name):
    img.save(OUT / f"{name}.png")


def diagram_23():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "2.3 - Kiến trúc Web App nhiều lớp", "User -> CloudFront -> ALB -> EC2/Nginx -> Spring Boot -> RDS/S3")
    aws_frame(d, (540, 210, 1780, 960), "AWS Cloud - TCBlog")

    node(d, (85, 500, 280, 320), "CLIENT", "Người dùng\nPC / Laptop / Mobile", GREEN_BG, GREEN, "USER")
    node(d, (620, 500, 270, 320), "CLOUDFRONT", "HTTPS public URL\nRedirect HTTP sang HTTPS", BLUE_BG, BLUE, "HTTPS")
    node(d, (995, 500, 270, 320), "ALB", "Load balancer\nChỉ route target healthy", BLUE_BG, BLUE, "ALB")
    node(d, (1390, 345, 285, 290), "EC2 #1", "Nginx 80\nSpring Boot 8080", GREEN_BG, GREEN, "EC2-1")
    node(d, (1390, 745, 285, 290), "EC2 #2", "Nginx 80\nSpring Boot 8080", GREEN_BG, GREEN, "EC2-2")
    node(d, (1870, 345, 285, 290), "RDS MYSQL", "Database\nwebsocial_after", ORANGE_BG, ORANGE, "DB")
    node(d, (1870, 745, 285, 290), "S3", "Ảnh bài viết\nstory, avatar, chat", ORANGE_BG, ORANGE, "S3")

    step(d, 1, "Truy cập HTTPS", 80, 405, BLUE, 260)
    step(d, 2, "Edge nhận request", 610, 405, BLUE, 280)
    step(d, 3, "Cân bằng tải", 985, 405, BLUE, 250)
    step(d, 4, "Xử lý app", 1390, 255, GREEN, 220)
    step(d, 5, "Lưu dữ liệu", 1870, 255, ORANGE, 220)
    step(d, 6, "Trả response", 630, 900, GREEN, 230)

    arrow(d, (365, 660), (620, 660), BLUE)
    arrow(d, (890, 660), (995, 660), BLUE)
    arrow(d, (1265, 660), (1390, 490), GREEN)
    arrow(d, (1265, 660), (1390, 890), GREEN)
    arrow(d, (1675, 490), (1870, 490), ORANGE)
    arrow(d, (1675, 890), (1870, 890), ORANGE)
    arrow(d, (620, 940), (365, 800), GREEN, dashed=True)

    note(d, "Security Group chỉ mở các port cần thiết giữa ALB, EC2 và RDS.", 620, 1015, 520, BLUE)
    note(d, "Media được tách khỏi database và lưu trên S3.", 1580, 1060, 500, ORANGE)
    save(img, "02-3-kien-truc-web-app")


def diagram_24():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "2.4 - CloudFront -> ALB -> Nginx -> Spring Boot", "CloudFront cấp HTTPS, ALB cân bằng tải, Nginx proxy vào ứng dụng")
    aws_frame(d, (410, 210, 1880, 930), "AWS Network & Compute")

    node(d, (85, 520, 250, 300), "USER", "Gửi HTTPS request", GREEN_BG, GREEN, "USER")
    node(d, (500, 520, 280, 300), "CLOUDFRONT", "Viewer HTTPS\nOrigin là ALB DNS", BLUE_BG, BLUE, "CF")
    node(d, (900, 520, 280, 300), "ALB", "Listener HTTP 80\nTarget Group", BLUE_BG, BLUE, "ALB")
    node(d, (1320, 340, 285, 280), "NGINX #1", "Reverse proxy\n127.0.0.1:8080", GREEN_BG, GREEN, "N")
    node(d, (1320, 760, 285, 280), "NGINX #2", "Reverse proxy\n127.0.0.1:8080", GREEN_BG, GREEN, "N")
    node(d, (1780, 340, 285, 280), "SPRING #1", "tcblog.service\nport 8080", PURPLE_BG, PURPLE, "APP")
    node(d, (1780, 760, 285, 280), "SPRING #2", "tcblog.service\nport 8080", PURPLE_BG, PURPLE, "APP")

    step(d, 1, "HTTPS", 90, 420, BLUE, 165)
    step(d, 2, "Forward ALB", 500, 420, BLUE, 230)
    step(d, 3, "Route target", 900, 420, BLUE, 230)
    step(d, 4, "Proxy pass", 1320, 250, GREEN, 220)
    step(d, 5, "Ứng dụng xử lý", 1780, 250, PURPLE, 260)

    arrow(d, (335, 670), (500, 670), BLUE)
    arrow(d, (780, 670), (900, 670), BLUE)
    arrow(d, (1180, 670), (1320, 480), GREEN)
    arrow(d, (1180, 670), (1320, 900), GREEN)
    arrow(d, (1605, 480), (1780, 480), PURPLE)
    arrow(d, (1605, 900), (1780, 900), PURPLE)
    note(d, "CloudFront dùng HTTPS phía người dùng, còn origin về ALB đang dùng HTTP để phù hợp cấu hình hiện tại.", 500, 930, 700, BLUE)
    save(img, "02-4-cloudfront-alb-nginx")


def diagram_32():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "3.2 - Luồng Người Dùng", "Browser -> CloudFront HTTPS -> ALB -> EC2/Nginx -> Spring Boot -> RDS/S3")
    aws_frame(d, (430, 210, 1860, 930), "AWS Runtime")

    node(d, (85, 520, 250, 300), "BROWSER", "Người dùng thao tác\ntrên web", GREEN_BG, GREEN, "WEB")
    node(d, (520, 520, 270, 300), "CLOUDFRONT", "Nhận request HTTPS", BLUE_BG, BLUE, "CF")
    node(d, (910, 520, 270, 300), "ALB", "Chọn EC2 healthy", BLUE_BG, BLUE, "ALB")
    node(d, (1320, 350, 270, 280), "APP #1", "Nginx + Spring Boot", GREEN_BG, GREEN, "EC2-1")
    node(d, (1320, 770, 270, 280), "APP #2", "Nginx + Spring Boot", GREEN_BG, GREEN, "EC2-2")
    node(d, (1780, 350, 270, 280), "RDS", "Dữ liệu nghiệp vụ", ORANGE_BG, ORANGE, "DB")
    node(d, (1780, 770, 270, 280), "S3", "Tệp hình ảnh", ORANGE_BG, ORANGE, "S3")

    step(d, 1, "Mở URL", 90, 420, BLUE, 190)
    step(d, 2, "Forward", 520, 420, BLUE, 190)
    step(d, 3, "Load balance", 910, 420, BLUE, 230)
    step(d, 4, "CRUD / Media", 1780, 260, ORANGE, 245)
    step(d, 5, "Response", 520, 930, GREEN, 200)

    arrow(d, (335, 670), (520, 670), BLUE)
    arrow(d, (790, 670), (910, 670), BLUE)
    arrow(d, (1180, 670), (1320, 490), GREEN)
    arrow(d, (1180, 670), (1320, 910), GREEN)
    arrow(d, (1590, 490), (1780, 490), ORANGE)
    arrow(d, (1590, 910), (1780, 910), ORANGE)
    arrow(d, (520, 960), (335, 820), GREEN, dashed=True)
    save(img, "03-2-luong-nguoi-dung")


def diagram_33():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "3.3 - Các bước triển khai trên AWS Console", "Luồng cấu hình từng dịch vụ: EC2/ASG -> ALB -> RDS -> S3 -> CloudFront -> CodeBuild -> CloudWatch")

    aws_frame(d, (100, 205, 2200, 1010), "AWS Console Deployment Flow")

    steps = [
        ("1", "EC2 / AMI", "Tạo EC2 Ubuntu\ncài Java, Nginx\nđóng AMI", GREEN_BG, GREEN, "EC2"),
        ("2", "Launch Template", "Dùng AMI đã tạo\nIAM role tcblog\nSecurity Group", BLUE_BG, BLUE, "LT"),
        ("3", "Auto Scaling Group", "Desired = 2\n2 EC2 InService\nkhác AZ", BLUE_BG, BLUE, "ASG"),
        ("4", "Target Group", "Register 2 EC2\nHealth check /\nport 80", CYAN_BG, CYAN, "TG"),
        ("5", "ALB", "Public endpoint\nroute đến TG\nHTTP 80", CYAN_BG, CYAN, "ALB"),
        ("6", "RDS MySQL", "Database\nwebsocial_after\nSG chỉ cho EC2", ORANGE_BG, ORANGE, "DB"),
        ("7", "S3 Bucket", "Lưu media\nvà artifact\nreleases/latest.zip", ORANGE_BG, ORANGE, "S3"),
        ("8", "CloudFront", "HTTPS URL\nOrigin trỏ ALB\ncache disabled", PURPLE_BG, PURPLE, "CF"),
        ("9", "CodeBuild + SSM", "Build JAR\nupload S3\nrestart 2 EC2", PURPLE_BG, PURPLE, "CI"),
        ("10", "CloudWatch", "Dashboard\nAlarm\nSNS email", RED_BG, RED, "MON"),
    ]

    x0, y0 = 170, 365
    card_w, card_h = 360, 230
    gap_x, gap_y = 65, 150
    positions = []

    for i in range(5):
        positions.append((x0 + i * (card_w + gap_x), y0))
    for i in range(5):
        positions.append((x0 + (4 - i) * (card_w + gap_x), y0 + card_h + gap_y))

    for idx, (num, heading, body, fill, outline, icon) in enumerate(steps):
        x, y = positions[idx]
        d.rounded_rectangle([x, y, x + card_w, y + card_h], radius=26, fill=fill, outline=outline, width=4)
        d.ellipse([x + 22, y + 22, x + 66, y + 66], fill=outline)
        draw_center(d, (x + 22, y + 22, 44, 44), num, F_STEP, fill="white", chars=2)
        d.text((x + 84, y + 24), heading, font=F_NODE_SUB, fill=outline)
        draw_center(d, (x + 24, y + 78, card_w - 48, 50), icon, F_ICON, fill=outline, chars=8)
        draw_center(d, (x + 36, y + 132, card_w - 72, 78), body, F_SMALL, fill=MUTED, chars=22)

    # Top row arrows.
    for i in range(4):
        x, y = positions[i]
        nx, ny = positions[i + 1]
        arrow(d, (x + card_w, y + card_h / 2), (nx, ny + card_h / 2), BLUE, width=5)

    # Turn down from step 5 to step 6.
    x5, y5 = positions[4]
    x6, y6 = positions[5]
    arrow(d, (x5 + card_w / 2, y5 + card_h), (x6 + card_w / 2, y6), BLUE, width=5)

    # Bottom row arrows go right-to-left visually.
    for i in range(5, 9):
        x, y = positions[i]
        nx, ny = positions[i + 1]
        arrow(d, (x, y + card_h / 2), (nx + card_w, ny + card_h / 2), BLUE, width=5)

    note(d, "Ảnh thật cần chụp cho mục 3.3: EC2/ASG, Target Group healthy, RDS, S3, CloudFront, CodeBuild và CloudWatch Dashboard.", 560, 1085, 1250, BLUE)
    save(img, "03-3-cac-buoc-aws-console")


def diagram_34():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "3.4 - Tổng Kiến Trúc Hệ Thống", "Runtime, CI/CD, storage và monitoring của TCBlog trên AWS")
    aws_frame(d, (410, 190, 1900, 1000), "AWS ap-southeast-1")

    node(d, (70, 520, 230, 290), "USER", "Trình duyệt web", GREEN_BG, GREEN, "USER")
    node(d, (480, 520, 250, 290), "CloudFront", "HTTPS endpoint", BLUE_BG, BLUE, "CF")
    node(d, (820, 520, 250, 290), "ALB", "Load balancer", BLUE_BG, BLUE, "ALB")
    node(d, (1170, 340, 250, 260), "EC2 #1", "Nginx\nSpring Boot", GREEN_BG, GREEN, "EC2-1")
    node(d, (1170, 760, 250, 260), "EC2 #2", "Nginx\nSpring Boot", GREEN_BG, GREEN, "EC2-2")
    node(d, (1530, 340, 250, 260), "RDS", "MySQL", ORANGE_BG, ORANGE, "DB")
    node(d, (1530, 760, 250, 260), "S3", "Media + artifact", ORANGE_BG, ORANGE, "S3")
    node(d, (1910, 300, 260, 260), "CloudWatch", "Dashboard\nAlarm + SNS", PURPLE_BG, PURPLE, "MON")
    node(d, (1910, 790, 260, 260), "CodeBuild\n+ SSM", "Build và deploy\n2 EC2", PURPLE_BG, PURPLE, "CI")

    arrow(d, (300, 665), (480, 665), BLUE)
    arrow(d, (730, 665), (820, 665), BLUE)
    arrow(d, (1070, 665), (1170, 470), GREEN)
    arrow(d, (1070, 665), (1170, 890), GREEN)
    arrow(d, (1420, 470), (1530, 470), ORANGE)
    arrow(d, (1420, 890), (1530, 890), ORANGE)
    arrow(d, (1780, 470), (1910, 430), PURPLE)
    arrow(d, (1910, 920), (1420, 890), PURPLE, dashed=True)
    arrow(d, (1910, 920), (1420, 470), PURPLE, dashed=True)

    note(d, "Runtime path", 470, 875, 300, BLUE)
    note(d, "CI/CD deploy artifact từ S3 qua SSM đến các EC2 InService.", 1850, 1080, 390, PURPLE)
    save(img, "03-4-tong-kien-truc")


def diagram_35():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    title(d, "3.5 - Luồng CI/CD", "GitHub -> CodeBuild -> Maven JAR -> S3 releases/latest.zip -> SSM -> EC2 tcblog.service")
    aws_frame(d, (650, 210, 1660, 900), "AWS CI/CD")

    node(d, (70, 530, 250, 290), "DEVELOPER", "Sửa code\ncommit / push", GREEN_BG, GREEN, "DEV")
    node(d, (420, 530, 250, 290), "GITHUB", "Repository\nmain branch", PURPLE_BG, PURPLE, "GH")
    node(d, (760, 530, 270, 290), "CODEBUILD", "mvn package\nzip deploy", PURPLE_BG, PURPLE, "CB")
    node(d, (1120, 530, 270, 290), "S3", "releases/latest.zip", ORANGE_BG, ORANGE, "S3")
    node(d, (1480, 530, 270, 290), "SSM", "Run Command", BLUE_BG, BLUE, "SSM")
    node(d, (1850, 360, 260, 270), "EC2 #1", "Copy JAR\nrestart tcblog", GREEN_BG, GREEN, "EC2-1")
    node(d, (1850, 780, 260, 270), "EC2 #2", "Copy JAR\nrestart tcblog", GREEN_BG, GREEN, "EC2-2")

    step(d, 1, "Push code", 75, 430, GREEN, 200)
    step(d, 2, "Build", 760, 430, PURPLE, 165)
    step(d, 3, "Upload", 1120, 430, ORANGE, 175)
    step(d, 4, "Deploy", 1480, 430, BLUE, 180)
    step(d, 5, "Restart service", 1850, 270, GREEN, 260)

    arrow(d, (320, 675), (420, 675), GREEN)
    arrow(d, (670, 675), (760, 675), PURPLE)
    arrow(d, (1030, 675), (1120, 675), ORANGE)
    arrow(d, (1390, 675), (1480, 675), BLUE)
    arrow(d, (1750, 675), (1850, 495), GREEN)
    arrow(d, (1750, 675), (1850, 915), GREEN)
    note(d, "Webhook chưa bật: có thể Start build thủ công để demo.", 760, 880, 520, PURPLE)
    save(img, "03-5-luong-cicd")


if __name__ == "__main__":
    diagram_23()
    diagram_24()
    diagram_32()
    diagram_33()
    diagram_34()
    diagram_35()
    print(OUT.resolve())

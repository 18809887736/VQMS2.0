FROM python:3.11-slim

ENV TZ=Asia/Shanghai \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY src/ src/
COPY config/ config/

# 常驻调度入口：启动补灌昨日（幂等）→ 每日 DAILY_TIME 为昨日灌数
CMD ["python", "-m", "src.scheduler"]

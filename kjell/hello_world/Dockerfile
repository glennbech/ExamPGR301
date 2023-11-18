FROM python:3.9
WORKDIR /app
COPY . .
RUN pip install -r requirements.txt
ENV AWS_ACCESS_KEY_ID=""
ENV AWS_SECRET_ACCESS_KEY=""
ENV BUCKET_NAME=""
CMD ["python3", "app.py"]
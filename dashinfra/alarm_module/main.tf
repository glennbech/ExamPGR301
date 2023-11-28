resource "aws_cloudwatch_metric_alarm" "threshold" {
  alarm_name  = "${var.prefix}-threshold"
  namespace   = var.prefix
  metric_name = "total_drug_inventory.value"

  comparison_operator = "LessThanThreshold"
  threshold           = var.threshold
  evaluation_periods  = "2"
  period              = "60"
  statistic           = "Maximum"

  alarm_description = "This alarm goes off as soon as the total medical drug inventory is less then a certain amount "
  alarm_actions     = [aws_sns_topic.user_updates.arn]
}

resource "aws_sns_topic" "user_updates" {
  name = "${var.prefix}-alarm-topic"
}

resource "aws_sns_topic_subscription" "user_updates_sqs_target" {
  topic_arn = aws_sns_topic.user_updates.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}



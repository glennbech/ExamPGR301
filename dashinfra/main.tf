resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = var.dashboard_name
  dashboard_body = <<DASHBOARD
{
  "widgets": [
    {
      "type": "metric",
      "x": 0,
      "y": 0,
      "width": 12,
      "height": 6,
      "properties": {
        "metrics": [
          [
            "${var.dashboard_name}",
            "drug_count.value"
          ]
        ],
        "period": 300,
        "stat": "Maximum",
        "region": "eu-west-1",
        "title": "Total number of medical drugs available"
      }
    },
        {
      "type": "metric",
      "x": 12,
      "y": 0,
      "width": 12,
      "height": 6,
      "properties": {
        "metrics": [
          [
            "${var.dashboard_name}",
            "total_drug_inventory.value"
          ]
        ],
        "period": 300,
        "stat": "Maximum",
        "region": "eu-west-1",
        "title": "Total number of medical drug supply"
      }
    },
            {
      "type": "metric",
      "x": 0,
      "y": 6,
      "width": 12,
      "height": 6,
      "properties": {
        "metrics": [
          [
            "${var.dashboard_name}",
            "ppeget.avg",
            "exception", "none",
            "method", "scanForPPE",
            "class", "com.example.s3rekognition.RekognitionController"
          ]
        ],
        "period": 300,
        "stat": "Average",
        "region": "eu-west-1",
        "title": "PPE GET"
      }
    },
            {
      "type": "metric",
      "x": 0,
      "y": 12,
      "width": 12,
      "height": 6,
      "properties": {
        "metrics": [
          [
            "${var.dashboard_name}",
            "ppescanduration.avg"
          ]
        ],
        "period": 300,
        "stat": "Average",
        "region": "eu-west-1",
        "title": "PPE Method Duration"
      }
    },
                {
      "type": "metric",
      "x": 12,
      "y": 6,
      "width": 12,
      "height": 6,
      "properties": {
        "metrics": [
          [
            "${var.dashboard_name}",
            "handcoverget.avg",
            "exception", "none",
            "method", "scanForHandCover",
            "class", "com.example.s3rekognition.RekognitionController"
          ]
        ],
        "period": 300,
        "stat": "Average",
        "region": "eu-west-1",
        "title": "PPE Hand Cover GET"
      }
    },
            {
      "type": "metric",
      "x": 12,
      "y": 12,
      "width": 12,
      "height": 6,
      "properties": {
        "metrics": [
          [
            "${var.dashboard_name}",
            "handcoverscanduration.avg"
          ]
        ],
        "period": 300,
        "stat": "Average",
        "region": "eu-west-1",
        "title": "PPE Hand Cover Method Duration"
      }
    }
  ]
}
DASHBOARD
}

module "alarm" {
  source = "./alarm_module"
  alarm_email = var.alarm_email
  prefix = var.dashboard_name
}
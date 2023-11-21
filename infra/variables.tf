variable "service_prefix" {
  type = string
  default ="candidate2043ar"
}

variable "access_role_arn_prefix" {
  type = string
  default ="arn:aws:iam::244530008913:role/service-role/AppRunnerECRAccessRole"
}

variable "image" {
  type = string
  default ="244530008913.dkr.ecr.eu-west-1.amazonaws.com/candidate2043ecr:latest"
}

variable "iam_role_name" {
  type        = string
  default     = "candidate2043-iam-role-name"
}

variable "iam_policy_name" {
  type        = string
  default     = "candidate2043-iam-policy-name"
}

variable "cpu" {
  type        = string
  default     = "256"
}

variable "memory" {
  type        = string
  default     = "1024"
}

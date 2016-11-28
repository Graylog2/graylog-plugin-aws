# AWS Plugin For Graylog

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-aws.svg)](https://travis-ci.org/Graylog2/graylog-plugin-aws)

This plugin provides the following Graylog modules:

* Input plugin for [AWS Flow Logs](http://docs.aws.amazon.com/AmazonVPC/latest/UserGuide/flow-logs.html) network interface connection logs
* Input plugin for [AWS CloudTrail](http://aws.amazon.com/cloudtrail/) logs

**Required Graylog version:** v2.1.0 and later (Please use the [v0.6 release](https://github.com/Graylog2/graylog-plugin-aws/releases/tag/0.6.0) of this plugin if you are running Graylog v2.0.x)

## Installation

[Download the plugin](https://github.com/Graylog2/graylog-plugin-aws/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## General setup

After installing the plugin you will have a new cluster configuration section at “System -> Configurations” in your Graylog Web Interface. Make sure to complete the configuration before using any of the modules this plugin provides. You’ll see a lot of warnings in your `graylog-server` log file if you fail to do so.

Note that the AWS access and secret key are currently not stored encrypted. This feature is following shortly and before the final release of v1.0 of this plugin. (in a few days)

### AWS entity translation

The configuration of this plugin has a parameter that controls if AWS entity translations are supposed to be attempted or not. This basically means that the plugin will try to find certain fields like a source IP address and enrich the log message with more information about the AWS entity (like a EC2 box, an ELB instance, a RDS database, …) automatically.

This would look something like this:

[![](https://s3.amazonaws.com/graylog2public/aws_translation.jpg)](https://s3.amazonaws.com/graylog2public/aws_translation.jpg)

Here are required IAM permissions in case you decide to use this feature:

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "Stmt1469415911000",
            "Effect": "Allow",
            "Action": [
                "elasticloadbalancing:DescribeLoadBalancerAttributes",
                "elasticloadbalancing:DescribeLoadBalancers"
            ],
            "Resource": [
                "*"
            ]
        },
        {
            "Sid": "Stmt1469415936000",
            "Effect": "Allow",
            "Action": [
                "ec2:DescribeInstances",
                "ec2:DescribeNetworkInterfaceAttribute",
                "ec2:DescribeNetworkInterfaces"
            ],
            "Resource": [
                "*"
            ]
        }
    ]
}
```

## FlowLogs setup and configuration

The Flow Logs integration and analysis examples are described in [this graylog.org blog post](https://www.graylog.org/blog/62-a-practical-approach-to-open-source-network-security).

### Step 1: Enable Flow Logs

There are two ways to enable Flow Logs for an AWS network interface:

For a specific network interface in your EC2 console, under the “Network Interfaces” main navigation link:

[![](https://s3.amazonaws.com/graylog2public/flowlogs_1.jpg)](https://s3.amazonaws.com/graylog2public/flowlogs_1.jpg)

… or for all network interfaces in your VPC using the VPC console:

[![](https://s3.amazonaws.com/graylog2public/flowlogs_3.jpg)](https://s3.amazonaws.com/graylog2public/flowlogs_3.jpg)

After a few minutes (usually 15 minutes but it can take up to an hour), AWS will start writing Flow Logs and you can view them in your CloudWatch console:

[![](https://s3.amazonaws.com/graylog2public/flowlogs_2.jpg)](https://s3.amazonaws.com/graylog2public/flowlogs_2.jpg)

Now let’s go on and instruct AWS to write the FlowLogs to a [Kinesis](https://aws.amazon.com/kinesis/) stream.

### Steps 2: Set up Kinesis stream

Create a [Kinesis](https://aws.amazon.com/kinesis/) stream using the AWS CLI tools:

    aws kinesis create-stream --stream-name "flowlogs" --shard-count 1

Now get the Stream details:

    aws kinesis describe-stream --stream-name "flowlogs"

**Copy the StreamARN from the output.** We'll need it later.

Next, create a file called _trust_policy.json_ with the following content:

```
{
  "Statement": {
    "Effect": "Allow",
    "Principal": { "Service": "logs.eu-west-1.amazonaws.com" },
    "Action": "sts:AssumeRole"
  }
}
```

**Make sure to change the _Service_ from _eu-west-1_ to the Region you are running in.**

Now create a a new IAM role with the permissions in the file we just created:

    aws iam create-role --role-name CWLtoKinesisRole --assume-role-policy-document file://trust_policy.json

**Copy the ARN of the role you just created.** You'll need it in the next step.

Create a new file called _permissions.json_ and set both ARNs to the ARNs your copied above:

```
{
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "kinesis:PutRecord",
      "Resource": "arn:aws:kinesis:eu-west-1:[YOUR KINESIS STREAM ARN HERE]:stream/flowlogs"
    },
    {
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": "arn:aws:iam::[YOUR IAM ARN HERE]:role/CWLtoKinesisRole"
    }
  ]
}
```

**Make sure to change the AWS regions here if necessary, too.**

Now attach this role:

    aws iam put-role-policy --role-name CWLtoKinesisRole --policy-name Permissions-Policy-For-CWL --policy-document file://permissions.json

The last step is to create the actual subscription that will write the FlowLogs to Kinesis:

```
aws logs put-subscription-filter \
    --filter-name "MatchAllValidFilter" \
    --filter-pattern "OK" \
    --log-group-name "my-flowlogs" \
    --destination-arn "arn:aws:kinesis:eu-west-1:123456789:stream/flowlogs" \
    --role-arn "arn:aws:iam::123456789012:role/CWLtoKinesisRole"
```

You should now see FlowLogs being written into your Kinesis stream.

### Step 4: Launch input

Now go into the Graylog Web Interface and start a new *AWS FlowLogs input*. It will ask you for some simple parameters like the Kinesis Stream name you are writing your FlowLogs to.

**Important: AWS delivers the FlowLogs with a few minutes delay and not always in an ordered fashion. Keep this in mind when searching over messages in a recent time frame.*

## CloudTrail setup and configuration

### Step 1: Enabling CloudTrail for an AWS region

Start by enabling CloudTrail for an AWS region:

![Configuring CloudTrail](https://raw.githubusercontent.com/Graylog2/graylog-plugin-aws/master/images/plugin-aws-input-1.png)

* **Create a new S3 bucket:** Yes
* **S3 bucket:** Choose anything here, you do not need it for configuration of Graylog later
* **Log file prefix:** Optional, not required for Graylog configuration
* **Include global services:** Yes (you might want to change this when using CloudTrail in multiple AWS regions)
  * **SNS notification for every log file delivery:** Yes
  * **SNS topic:** Choose something like *cloudtrail-log-write* here. Remember the name.

### Step 2: Set up SQS for CloudTrail write notifications

Navigate to the AWS SQS service (in the same region as the just enabled CloudTrail) and hit **Create New Queue**.

![Creating a SQS queue](https://raw.githubusercontent.com/Graylog2/graylog-plugin-aws/master/images/plugin-aws-input-2.png)

You can leave all settings on their default values for now but write down the **Queue Name** because you will need it for the Graylog configuration later. Our recommended default value is *cloudtrail-notifications*.

CloudTrail will write notifications about log files it wrote to S3 to this queue and Graylog needs this information. Let’s subscribe the SQS queue to the CloudTrail SNS topic you created in the first step now:

![Subscribing SQS queue to SNS topic](https://raw.githubusercontent.com/Graylog2/graylog-plugin-aws/master/images/plugin-aws-input-3.png)

Right click on the new queue you just created and select *Subscribe Queue to SNS Topic*. Select the SNS topic that you configured in the first step when setting up CloudTrail. **Hit subscribe and you are all done with the AWS configuration.**

### Step 3: Install and configure the Graylog CloudTrail plugin

Copy the `.jar` file that you received to your Graylog plugin directory which is configured in your `graylog.conf` configuration file using the `plugin_dir` variable.

Restart `graylog-server` and you should see the new input type *AWS CloudTrail Input* at *System -> Inputs -> Launch new input*. The required input configuration should be self-explanatory.

**Important:** The IAM user you configured in “System -> Configurations” has to have permissions to read CloudTrail logs from S3 and write notifications from SQS:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Stmt1411854479000",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": [
        "arn:aws:s3:::cloudtrail-logfiles/*"
      ]
    }
  ]
}
```

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Stmt1411834838000",
      "Effect": "Allow",
      "Action": [
        "sqs:DeleteMessage",
        "sqs:ReceiveMessage"
      ],
      "Resource": [
        "arn:aws:sqs:eu-west-1:450000000000:cloudtrail-write"
      ]
    }
  ]
}
```

(Make sure to replace *resource* values with the actual ARNs of your environment)

**More required IAM roles:** The way we communicate with Kinesis requires us to store some metadata in AWS DynamoDB and we are also writing some metrics back to AWS CloudWatch. For this to work you have to attach the following standard AWS IAM policies to your AWS API user:

* CloudWatchFullAccess
* AmazonDynamoDBFullAccess
* AmazonKinesisReadOnlyAccess

**Note that these are very open standard permissions.** We recommend to use them for a test setup but further boil them down to only allow  access (read+write) to the DynamoDB table we automatically created (you'll see it in the list of tables) and also to only call `cloudwatch:PutMetricData`. How to get the ARNs and how to create custom policies would be out of scope for this guide.

### Usage

You should see CloudTrail messages coming in after launching the input. (Note that it can take a few minutes based on how frequent systems are accessing your AWS resource) **You can even stop Graylog and it will catch up with all CloudTrail messages that were written since it was stopped when it is started a!gain.**

**Now do a search in Graylog. Select “Search in all messages” and search for:** `source:"aws-cloudtrail"`


## S3 setup and configuration

### Step 1: Write some logs to S3

Start by enabling something to write log files to S3, like FluentD (http://docs.fluentd.org/articles/out_s3):

The writer could be unique to your setup, so this is left to the reader. The log line contents can be raw text or json. No processing or extraction is done by this plugin. It is left up to you.

### Step 2: Enable S3 Event Notifications to SNS

Create an SNS topic to write the events to, then configure S3 Events for Create and Delete to write to the SNS topic created above (remember the name of the SNS topic for later).

### Step 3: Set up SQS for S3 notifications

Navigate to the AWS SQS service (in the same region as the SNS topic) and hit **Create New Queue**.

You can leave all settings on their default values for now but write down the **Queue Name** because you will need it for the Graylog configuration later. Our recommended default value is *s3-notifications*.

S3 will write notifications about log files that were created or removed. Let’s subscribe the SQS queue to the S3 SNS topic you created in the first step now:

Right click on the new queue you just created and select *Subscribe Queue to SNS Topic*. Select the SNS topic that you configured when setting up your S3 bucket events. **Hit subscribe and you are all done with the AWS configuration.**

### Step 4: Install and configure the Graylog S3 plugin

Copy the `.jar` file that you received to your Graylog plugin directory which is configured in your `graylog.conf` configuration file using the `plugin_dir` variable.

Restart `graylog-server` and you should see the new input type *AWS S3 Input* at *System -> Inputs -> Launch new input*. The required input configuration should be self-explanatory.

**Important:** The IAM user you configured in “System -> Configurations” has to have permissions to read S3 objects and delete and read notifications from SQS:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Stmt1411854479000",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": [
        "arn:aws:s3:::s3-logfiles/*"
      ]
    }
  ]
}
```

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Stmt1411834838000",
      "Effect": "Allow",
      "Action": [
        "sqs:DeleteMessage",
        "sqs:ReceiveMessage"
      ],
      "Resource": [
        "arn:aws:sqs:eu-west-1:450000000000:s3-write"
      ]
    }
  ]
}
```

(Make sure to replace *resource* values with the actual ARNs of your environment)

### Usage

You should see your log messages coming in after launching the input. (Note that it can take a few minutes based on how frequent systems are accessing your AWS resource) **You can even stop Graylog and it will catch up with all S3 log data that have been written since it was stopped when it is started again.**

**Now do a search in Graylog. Select “Search in all messages” and search for:** `source:"aws-s3"`

The raw log lines in your S3 objects should show up in your search results. You can now use Extractors, Plugins, Pipeline, etc to process your raw messages.

For example, if you write json lines to your S3 files, you can use the JSON Extractor to extract all the json attributes.

## Build

This project is using Maven 3 and requires Java 8 or higher.

You can build a plugin (JAR) with `mvn package`.

DEB and RPM packages can be build with `mvn jdeb:jdeb` and `mvn rpm:rpm` respectively.

## Plugin Release

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. Travis CI will build the release artifacts and upload to GitHub automatically.

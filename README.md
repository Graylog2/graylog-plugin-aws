AWS CloudTrail Input Plugin For Graylog
=======================================

[![Build Status](https://travis-ci.org/Graylog2/graylog-plugin-aws.svg)](https://travis-ci.org/Graylog2/graylog-plugin-aws)

This plugin provides an input plugin for AWS CloudTrail. It reads [CloudTrail](http://aws.amazon.com/cloudtrail/) logs from your AWS account.

**Required Graylog version:** 1.0 and later

![Overview Screenshot](https://raw.githubusercontent.com/Graylog2/graylog-plugin-aws/master/images/plugin-aws-input-overview.png)

## Installation

[Download the plugin](https://github.com/Graylog2/graylog-plugin-aws/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Setup

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

**Important:** The access credentials have to belong to a user that has enough permissions for SQS and S3. These are example AWS IAM permission policies to allow reading the CloudTrail log files from S3 and the write notifications form SQS:

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

## Usage

You should see CloudTrail messages coming in after launching the input. (Note that it can take a few minutes based on how frequent systems are accessing your AWS resource) **You can even stop Graylog and it will catch up with all CloudTrail messages that were written since it was stopped when it is started a!gain.**

**Now do a search in Graylog. Select “Search in all messages” and search for:** `source:"aws-cloudtrail"`

## Build

This project is using Maven and requires Java 7 or higher.

You can build a plugin (JAR) with `mvn package`.

DEB and RPM packages can be build with `mvn jdeb:jdeb` and `mvn rpm:rpm` respectively.

## Plugin Release

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. TravisCI will build the release artifacts and upload to GitHub automatically.

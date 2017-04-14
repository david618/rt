# TCP to Elasticsearch

Goal was to measure the max write speed to Elasticsearch in DCOS.  

## Overview
- Input
  - Messages are lines (simFile.json). Each line is a JSONObject to be stored in Elasticsearch
  - Lines written to Kafka Topic
- Processing (none)
- Output
  - Take input from Kafka Topic 
  - Write Messages to Elasticsearch

## Assumptions

You've done something line [IntroTest](IntroTest.md)
- You've already installed DCOS and Kafka. 
- You have a Test Server setup

## Install Elasticsearch

From Universe->Packages

Search for Elasticsearch

Advanced Install 
- Instances: 1




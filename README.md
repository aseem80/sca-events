# SCA Events

This repo contains source code for consuming Canonical Payload from Proton and Producing messages for SCA on SCA Kafka Cluster

## Motivation ##

The motivation is to facilitate SCA SOLR integration for anniversary 2018

# Getting Started ##

This is java based application to be built with maven.

## Prerequisites

Java 8, Maven, KAFKA Cluster URL


 ## Deployment

 ### Local

 1. From IDE just run OrgKafkaApp.
 2. From command prompt java -jar target/sca-events-1.0.0-SNAPSHOT.jar
 3. With Docker mvn docker:build docker:run


 ### Cloud

```html
  This application is dockerized but can be deployed on any VM including AWS beanstalk.
```

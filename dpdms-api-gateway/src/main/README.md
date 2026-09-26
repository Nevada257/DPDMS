DPDMS API Gateway

Project

Rushinga Provincial Disaster Monitoring and Management System (DPDMS)

Purpose

The API Gateway provides a single entry point for clients accessing the DPDMS microservices. It receives requests and routes them to the appropriate backend service.

Gateway Port

The API Gateway runs on port 8080.

Services

Service	Port	API Path
Flood Service	8082	/api/floods/**
Drought Service	8083	/api/droughts/**
Fire Service	8084	/api/fires/**
Mining Accident Service	8085	/api/mining-accidents/**
Zoonotic Disease Service	8086	/api/zoonotic-diseases/**

The service ports are subject to confirmation by the respective group members.

Responsibilities

* Provide a single entry point to the microservices.
* Route requests to the appropriate service.
* Support integration between DPDMS services.
* Provide health and monitoring endpoints through Spring Boot Actuator.
* Prepare the system for future integration with Eureka Service Discovery.

Technology

* Java
* Spring Boot
* Spring Cloud Gateway
* Maven
* Spring Boot Actuator
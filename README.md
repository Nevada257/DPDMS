# Mining Accident Module - Civil Protection System

**Student:** Faye
**Module:** Mining Accident Reporting & Approval
**Port:** 8085

## 1. Project Description
This module allows reporting of mining accidents in Zimbabwe wards and approval by Civil Protection Unit. It is part of the Civil Protection System group project.

## 2. Technologies Used
- Java 17
- Spring Boot 3.x
- MySQL 8.0
- Spring Data JPA
- Swagger / OpenAPI
- Maven

## 3. How to Run
1. Create MySQL database:
   CREATE DATABASE mining_accident_db;

2. Update src/main/resources/application.properties:
   spring.datasource.url=jdbc:mysql://localhost:3306/mining_accident_db
   spring.datasource.username=root
   spring.datasource.password=root
   server.port=8085
   spring.jpa.hibernate.ddl-auto=update

3. Run application:
   ./mvnw spring-boot:run
   OR Run CivilProtectionApplication.java in IntelliJ

4. App runs at: http://localhost:8085

## 4. API Endpoints
Method | Endpoint | Description
POST | /api/mining-accidents | Create new accident (PENDING)
GET | /api/mining-accidents | Get all accidents
GET | /api/mining-accidents/{id} | Get accident by ID
POST | /api/mining-accidents/{id}/approve | Approve accident

## 5. Sample JSON Request
{
"ward": "Ward 12",
"district": "Rushinga",
"province": "Mash Central",
"accidentType": "Shaft Collapse",
"description": "Mine shaft collapsed trapping workers",
"severity": "HIGH"
}

Sample JSON Response:
{
"id": 1,
"ward": "Ward 12",
"district": "Rushinga",
"province": "Mash Central",
"status": "APPROVED"
}

## 6. Verification
- Health: http://localhost:8085/actuator/health -> {"status":"UP"}
- Swagger: http://localhost:8085/swagger-ui.html
- MySQL: SELECT * FROM mining_accident;

## 7. Screenshots Included
1. POST create 200 PENDING
2. GET list 200
3. POST approve 200 APPROVED
4. MySQL Result Grid
5. Health UP
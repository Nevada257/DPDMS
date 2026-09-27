# Zoonotic Disease Service

## 1. Project Overview

The Zoonotic Disease Service is a Spring Boot microservice responsible for
recording, managing, approving and retrieving zoonotic disease incidents.

The service forms part of a larger Disaster and Public Health Management
System (DPDMS), where each hazard is implemented as an independent
microservice.

This service is responsible only for zoonotic disease incidents.

---

## 2. Technologies Used

- Java 21
- Spring Boot 4.0.8
- Maven
- MySQL
- Spring Data JPA / Hibernate
- Spring Security
- JWT authentication
- Swagger / OpenAPI
- Spring Boot Actuator
- JUnit 5
- Mockito

---

## 3. Main Zoonotic Incident Information

Each zoonotic incident contains:

- Ward
- District
- Province
- Occurrence date and time
- Reporter
- Severity
- Disease name
- Animal species affected
- Confirmed human cases
- Confirmed animal cases
- Event classification
- Latitude
- Longitude
- Incident status
- Username of the creator

Event classifications include:

- CLUSTER
- OUTBREAK

Severity levels include:

- LOW
- MEDIUM
- HIGH
- CRITICAL

Incident statuses include:

- PENDING
- APPROVED
- REJECTED
- CORRECTION_REQUIRED

---

## 4. Authentication

The service uses JWT signed-token authentication.

Users log in through the shared auth-service (via the gateway):

POST http://localhost:8080/api/auth/login

The returned JWT carries the user's role, hazardScope and ward. This service
accepts only tokens scoped to ZOONOTIC, or cross-hazard (ALL) tokens held by
NATIONAL or ADMIN users; any other token is rejected with 403.

The token must then be supplied in API requests using:

Authorization: Bearer <JWT_TOKEN>

Passwords are stored using BCrypt hashing.

---

## 5. User Roles

### Ward Recorder

A Ward Recorder:

- Can create incidents
- Can work only within their assigned ward
- Can view incidents within their ward
- Can view their own pending/non-approved incidents
- Cannot approve incidents

### Provincial Supervisor

A Provincial Supervisor:

- Can create incidents within their province
- Can view incidents within their province
- Can approve pending incidents
- Can reject pending incidents
- Can request corrections
- Can view audit trails

### National User

A National User:

- Has read-only access
- Can view approved incidents
- Can view approved incidents across provinces
- Cannot create, update or delete incidents
- Cannot approve or reject incidents

---

## 6. Approval Workflow

When a new incident is created, its status is automatically set to:

PENDING

A Provincial Supervisor can then:

1. Approve the incident
2. Reject the incident
3. Request correction

Approved incidents become available for wider reporting and viewing.

An audit record is created whenever an important incident state transition
occurs.

---

## 7. REST API Endpoints

### Zoonotic Incidents

POST /api/zoonotic-incidents

GET /api/zoonotic-incidents

GET /api/zoonotic-incidents/{id}

PUT /api/zoonotic-incidents/{id}

DELETE /api/zoonotic-incidents/{id}

### Approval Workflow

POST /api/zoonotic-incidents/{id}/approve

POST /api/zoonotic-incidents/{id}/reject

POST /api/zoonotic-incidents/{id}/request-correction

### Audit Trail

GET /api/audit-trails

GET /api/audit-trails/{incidentId}

---

## 8. Validation

The service validates incoming incident data.

Examples include:

- Ward is required
- Disease name is required
- Animal species is required
- Occurrence date and time is required
- Severity is required
- Event classification is required
- Human cases cannot be negative
- Animal cases cannot be negative
- Latitude must be between -90 and 90
- Longitude must be between -180 and 180

Invalid requests are returned with appropriate validation errors.

---

## 9. Audit Trail

The service maintains an audit trail containing:

- Incident ID
- Action performed
- Previous status
- New status
- User who performed the action
- Date and time of the action

This provides traceability of important incident workflow changes.

---

## 10. Frontend

This service has no front end of its own. The group's shared React front end
(`flood-frontend/`, ZoonoticPanel) is used for all hazards.

---

## 11. Database

The service uses MySQL.

Database:

zoonotic_db

The service maintains its own database/schema so that it can operate as an
independent microservice.

---

## 12. Running the Backend

From the project root:

Windows:

    .\mvnw.cmd spring-boot:run

The backend runs on:

    http://localhost:8084

---

## 14. Swagger / OpenAPI

Swagger UI is available at:

    http://localhost:8084/swagger-ui/index.html

The OpenAPI documentation describes the available REST endpoints and supports
JWT bearer authentication.

---

## 15. Health and Monitoring

Spring Boot Actuator provides health and metrics endpoints.

Health:

    http://localhost:8084/actuator/health

Metrics:

    http://localhost:8084/actuator/metrics

---

## 16. Testing

Automated unit tests are implemented using JUnit 5 and Mockito.

The tests cover important authorization and approval rules, including:

- Ward recorder ward scoping
- National user read-only restrictions
- Incident creation authorization
- Supervisor approval
- Prevention of approving already-approved incidents
- Provincial supervisor province scoping

Run the tests using:

    .\mvnw.cmd test

---

## 17. Microservice Integration

This service is designed to operate independently as part of the larger
DPDMS microservices architecture.

For group integration, the service can be connected to:

- Service discovery
- API Gateway
- Central React frontend
- Other hazard microservices
- Central dashboard and map
- Reporting services
- Notification services

The Zoonotic Disease Service exposes REST APIs that allow other components
of the system to communicate with it.

---

## 18. Test Users

The development environment contains the following test accounts:

Ward Recorder:

    Username: recorder1
    Password: password123

Provincial Supervisor:

    Username: supervisor1
    Password: password123

National User:

    Username: national1
    Password: password123
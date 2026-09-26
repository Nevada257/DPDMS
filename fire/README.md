# Fire Service + Alert System

A small Spring Boot project for the Fire Service part of the group assignment.

## Included
- Fire incident CRUD REST API
- Fire-specific fields and GPS
- PENDING / APPROVED / REJECTED / NEEDS_CORRECTION workflow
- Audit trail
- PENDING records hidden from the normal GET endpoint
- Asynchronous email and WhatsApp alert service
- Alert logging
- Swagger/OpenAPI
- Actuator health
- MySQL database
- Basic project structure suitable for IntelliJ IDEA

## Requirements
- Java 17+
- IntelliJ IDEA
- MySQL 8+
- Maven

## MySQL setup

Create the database:

CREATE DATABASE fire_service;

The application creates the tables automatically.

Default connection:
- database: fire_service
- username: root
- password: empty

If your MySQL password is different, edit `application.properties` or set:
DB_URL
DB_USERNAME
DB_PASSWORD

## Run
Open the project in IntelliJ.
Wait for Maven to download dependencies.
Run `FireServiceApplication`.

Then open:
http://localhost:8081/swagger-ui.html

Health:
http://localhost:8081/actuator/health

## Main endpoints

POST /api/fire-incidents
GET /api/fire-incidents
GET /api/fire-incidents/all
GET /api/fire-incidents/{id}
PUT /api/fire-incidents/{id}
DELETE /api/fire-incidents/{id}

PUT /api/fire-incidents/{id}/approve
PUT /api/fire-incidents/{id}/reject?reason=...
PUT /api/fire-incidents/{id}/correction?reason=...

GET /api/audit/{incidentId}

POST /api/alerts/fire/{incidentId}
GET /api/alerts/logs

## Alert note

The email class supports real SMTP when MAIL_HOST, MAIL_USERNAME and
MAIL_PASSWORD are supplied. If MAIL_USERNAME is blank, it prints a demo
email to the IntelliJ console instead.

The WhatsApp class is deliberately kept simple: it prints the outgoing
message to the console. To make WhatsApp genuinely send messages, connect
a WhatsApp Business Cloud API provider and put the API call inside
WhatsAppAlertService.send(). Credentials should be supplied through
environment variables, not hard-coded.

## Example JSON

{
  "ward": "Ward 5",
  "district": "Harare",
  "province": "Harare",
  "occurrenceTime": "2026-09-24T15:30:00",
  "reporter": "Fire Recorder",
  "reporterEmail": "example@email.com",
  "reporterPhone": "+263771234567",
  "severity": "HIGH",
  "latitude": -17.8252,
  "longitude": 31.0335,
  "areaBurned": 2.5,
  "suspectedCause": "ACCIDENTAL",
  "injuriesOrFatalities": 1,
  "structuresDestroyed": 3,
  "active": true
}

New records are automatically PENDING.

For a NEEDS_CORRECTION record, using PUT /api/fire-incidents/{id}
resubmits it as PENDING.

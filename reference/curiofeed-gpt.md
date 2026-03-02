📘 CurioFeed

Article-based English Learning Platform
Designed with scalability and clean architecture in mind.

1. Project Overview

CurioFeed is a content-driven English learning platform where a single article is rewritten into three difficulty levels (Easy / Medium / Hard).

Each version includes:

Structured script

Audio narration

Vocabulary explanations

Comprehension & vocabulary quizzes

This demo focuses on system architecture, clean API boundaries, and scalable content modeling rather than full production automation.

2. Problem Statement

English learners often struggle with:

Articles being too difficult

Lack of structured vocabulary support

No integrated comprehension validation

No controlled difficulty progression

Additionally, most learning platforms tightly couple content delivery and rendering logic, making scalability difficult.

3. Solution Approach

CurioFeed separates:

Content modeling

Difficulty versioning

Media storage

Learning evaluation (quiz)

Presentation layer

The system is designed to support future automation pipelines (LLM-based content transformation, TTS automation) while keeping the demo lightweight.

4. Architecture Overview
High-Level Architecture
┌───────────┐
│   User    │
└─────┬─────┘
      │
      ▼
┌──────────────┐
│  React Front │
└─────┬────────┘
      │ REST API
      ▼
┌──────────────────────┐
│ Spring Boot Backend  │
│ - Controller Layer   │
│ - Service Layer      │
│ - Domain Layer       │
│ - Repository Layer   │
└─────┬────────────────┘
      │
      ▼
┌────────────────┐
│ PostgreSQL DB  │
└────────────────┘

Audio Files → Object Storage (S3-compatible or local volume)
5. Technology Stack
Backend

Java 17

Spring Boot 3

Spring Web

Spring Data JPA

PostgreSQL

Flyway (DB migration)

Swagger / OpenAPI

Docker

Why Spring Boot?

Strong enterprise adoption

Clear separation of concerns

Dependency Injection and transaction management

Mature ecosystem for scaling to microservices

Frontend

React

TypeScript

Axios

Tailwind (or CSS modules)

Why React?

Clear component architecture

Flexible state management

Industry standard for frontend roles

Database

PostgreSQL

Why PostgreSQL?

Strong relational modeling

JSON support for flexible content extensions

Industry standard for SaaS platforms

6. Domain Model
Core Entities
Article

Represents the root content entity.

id

category

originalTitle

createdAt

Relationship:

One Article → Many ArticleVersions

ArticleVersion

Represents difficulty-specific content.

id

articleId

difficulty (EASY, MEDIUM, HARD)

content

audioUrl

Relationship:

One Version → Many Vocabulary entries

One Version → Many Quiz questions

Vocabulary

id

articleVersionId

word

definition

exampleSentence

Quiz

id

articleVersionId

question

explanation

QuizOption

id

quizId

content

isCorrect

7. Backend Architecture

Layered architecture:

controller
   ↓
service
   ↓
domain
   ↓
repository
Design Principles

Separation of concerns

DTO isolation

Domain-driven modeling

Global exception handling

Validation at API boundary

Transactional service layer

8. API Overview
Public APIs

GET /api/articles
GET /api/articles/{id}
GET /api/articles/{id}/versions/{difficulty}
GET /api/versions/{id}/vocabulary
GET /api/versions/{id}/quiz

Admin APIs

POST /api/admin/articles
POST /api/admin/versions
POST /api/admin/vocabulary
POST /api/admin/quiz

Admin endpoints are secured.

9. Local Development
Requirements

Docker

Docker Compose

Run the project
docker-compose up --build

Services:

frontend → http://localhost:3000

backend → http://localhost:8080

postgres → localhost:5432

10. Scalability Strategy (Future Evolution)

This demo is designed with future expansion in mind.

Planned evolution:

1. Automated Content Pipeline

LLM-based article rewriting

Difficulty-level transformation

Vocabulary extraction

Architecture extension:

Article Ingestion
      ↓
Message Queue (Kafka)
      ↓
Content Worker
      ↓
TTS Worker
      ↓
Storage
2. Performance Enhancements

Redis caching layer

CDN for audio delivery

Read-replica database

Horizontal scaling via container orchestration

3. Feature Extensions

User authentication

Learning progress tracking

Personalized difficulty recommendation

Search engine integration (OpenSearch)

Mobile application

11. Non-Functional Considerations

RESTful API design

Stateless backend

Containerized deployment

Database migration management

Clear separation between domain and infrastructure

Future microservice compatibility

12. Roadmap

Phase 1:

Core content modeling

Manual content input

Quiz system

Dockerized environment

Phase 2:

LLM automation pipeline

Audio auto-generation

Caching layer

Authentication

Phase 3:

Personalization engine

Mobile application

Production cloud deployment

13. Design Philosophy

CurioFeed is built to demonstrate:

Backend architecture design

Clean data modeling

API boundary clarity

Scalability-first thinking

Enterprise-ready stack selection

The project prioritizes structural integrity over feature overload.
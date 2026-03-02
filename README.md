# CurioFeed 🚀
Fuel your curiosity, Master your English.

CurioFeed is a content-driven English learning platform that enables users to improve their English skills while reading articles and columns they are genuinely interested in.

By strategically transforming complex articles into optimized learning tiers, CurioFeed bridges the gap between high-level editorial content and individual linguistic capabilities, creating a seamless flow from curiosity to fluency.

## 🌟 Key Features

* **Curated Learning Feed**: A hand-picked selection of high-quality articles across Tech, Business, and Culture—ensuring users learn from the best sources.
* **Tri-Level Content Scaling**: Every article is pre-transformed into **Easy, Medium, and Hard** versions using LLM orchestration, optimizing for both readability and linguistic progression.
* **Optimized Multimodal Experience**: High-fidelity TTS audio is pre-generated and cached for instant playback. This enables simultaneous reading and listening to enhance phonetic awareness and auditory comprehension.
* **Interactive Vocabulary Insights**: A smart tooltip system provides instant English-to-English definitions and contextual examples, fostering an immersive "thinking in English" environment.
* **Contextual Knowledge Validation**: Dynamically generated comprehension and vocabulary quizzes reinforce learning and track progress through data-driven insights.

## 🏗 System Architecture & Tech Stack

This project is structured as a monorepo with clear separation between frontend, backend, and infrastructure components.

### Backend
- Java 17
- Spring Boot 3.x
- Spring Data JPA

### Frontend
- React (Vite)
- TypeScript
- Tailwind CSS

### Infrastructure
- Docker
- Docker Compose
- PostgreSQL (primary relational database, JSONB used for flexible quiz data)


## 🚦 How to run locally

The entire stack is containerized for easy local development.

1. Ensure Docker and Docker Compose are installed.
2. From the root directory, navigate to `/infra` and run the infrastructure:
   ```bash
   cd infra
   docker-compose up -d --build
   ```
3. Access Services:
   - **Frontend**: http://localhost:3000
   - **Backend API Docs (Swagger)**: http://localhost:8080/swagger-ui.html


## 📚 Engineering Deep-Dives
I document the architectural "Why" to share my thought process and engineering journey.

* [**ADR-001: Choosing Java 17 for Long-term Stability**](#)
* [**Optimizing AI API Costs with Content Caching**](#)
* [**Designing Flexible Quiz Schemas using PostgreSQL JSONB**](#)

## 🗺 Future Roadmap

### Engineering & Infrastructure
- Introduce asynchronous processing for AI-based content transformation and TTS generation
- Improve monitoring and metrics collection for API performance and AI usage
- Evolve deployment strategy toward cloud-managed infrastructure

### Intelligent Learning Features
- Implement a spaced repetition system (SRS) for vocabulary retention
- Develop personalized content recommendations based on reading behavior
- Provide progress analytics for comprehension and vocabulary growth

### Multimodal Enhancements
- Automate TTS generation pipeline

## 🤝 Contact
Lily (Eunah Yang) - Software Developer at Samsung Electronics
LinkedIn: [Eunah Yang](https://www.linkedin.com/in/eunah-yang-3a86553a4/)
Email: yua12271109@gmail.com
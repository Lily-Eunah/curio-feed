CurioFeed 🚀
Fuel your curiosity, Master your English.

CurioFeed is an AI-powered personalized language learning platform designed for users to master English while exploring topics they are truly passionate about—such as Tech, Cooking, Travel, and more. By transforming complex original articles into optimized learning materials, CurioFeed bridges the gap between high-quality content and individual linguistic capabilities.

🌟 Key Features
1. Tri-Level Content Scaling (AI-Powered)
CurioFeed leverages advanced LLM orchestration to rewrite a single news article or column into three distinct difficulty levels: Easy, Medium, and Hard. This process involves not just vocabulary substitution but a complete restructuring of syntax and contextual complexity to match the learner's proficiency.

2. Multimodal Learning Experience
Every scaled version of the script is accompanied by high-quality TTS (Text-to-Speech) audio. This allows users to engage in simultaneous reading and listening, a proven method for improving phonetic awareness and comprehension.

3. Interactive Vocabulary & Insights
Integrated within the script viewer is an interactive tooltip system. Clicking on key terms provides instant English-to-English definitions and contextual examples, encouraging an immersive "thinking in English" environment without leaving the flow of reading.

4. Contextual Knowledge Validation
Upon completion of a reading session, the platform dynamically generates comprehension and vocabulary quizzes based on the specific content. This ensures immediate reinforcement of learned material and tracks user progress over time.

🏗 System Architecture
The system is built on a Clean Architecture pattern to ensure separation of concerns and maintainability. It is designed to handle asynchronous AI workloads efficiently.

🛠 Tech Stack & Engineering Standards
Backend: Enterprise-Grade Stability
Java 17 & Spring Boot 3.x: Utilizing the modern Java ecosystem for high performance, type safety, and widespread industry adoption.

Spring Data JPA: Ensuring data integrity and robust object-relational mapping.

Frontend: High-Performance Web
React (Vite) & TypeScript: Implementing a responsive, type-safe UI for a seamless user experience.

Tailwind CSS: Employing a utility-first CSS framework for rapid UI development and consistent design tokens.

Infrastructure: Cloud-Native Readiness
Docker & Docker Compose: Ensuring environment parity across development and production through containerization.

PostgreSQL: Serving as the primary relational store, utilizing JSONB for flexible quiz schema management.

🚦 Getting Started
The entire stack is containerized for easy local development.

Bash
# Clone the repository
git clone https://github.com/your-username/curio-feed.git

# Navigate to the root directory
cd curio-feed

# Spin up the infrastructure (DB, API, Frontend)
docker-compose up -d
Frontend: http://localhost:5173

Backend API Docs (Swagger): http://localhost:8080/swagger-ui.html

🗺 Future Roadmap
[ ] Event-Driven Architecture: Integrating Apache Kafka to handle AI content generation and TTS synthesis as asynchronous background jobs.

[ ] Observability: Implementing Prometheus and Grafana for monitoring system latency and AI API usage.

[ ] Spaced Repetition System (SRS): Adding a Flashcard feature based on the Ebbinghaus forgetting curve.

💡 Engineering Decisions (ADR)
Why Hybrid Data Storage?: We utilize traditional relational tables for core entities while using JSONB for quiz structures to allow for diverse question types (Multiple-choice, Fill-in-the-blanks) without frequent schema migrations.

Async-First Mindset: Although the current demo is synchronous, the internal services are designed to be easily decoupled into a message-driven architecture to ensure scalability.
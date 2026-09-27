# Agent Operational Guidelines & Rules

## Project Context
This repository contains the backend service for the screening platform. This codebase specifically represents **Member 3's implementation**.

## Member 3 Responsibilities
- Screening session lifecycle management
- 15-second stabilization orchestration
- Core Spring service orchestration
- Real-time vital signs ingestion
- WebSocket/STOMP streaming
- Python AI worker integration boundary
- Session-level validation
- Finalization orchestration interface

## Out of Scope (Do Not Implement)
Do not implement unrelated team responsibilities:
- Authentication & authorization
- User management
- Patient management
- Frontend application
- AI model logic & execution
- Medical diagnosis algorithms
- Full clinical report generation logic
- Unrelated backend modules

## Mandatory Technical & Engineering Rules
1. **Scope Boundary**: This is Member 3's implementation. Do not implement unrelated team responsibilities.
2. **REST Endpoints**: Keep all Member 3 REST endpoints organized under `/api/member3/`.
3. **WebSocket Destinations**: Keep all Member 3 WebSocket/STOMP destinations organized under `/topic/member3/` (and application destinations under `/app/member3/` if applicable).
4. **No Thread.sleep**: Do not use `Thread.sleep` for the 15-second stabilization workflow. Use non-blocking, asynchronous, or scheduled orchestration mechanisms (e.g., `ScheduledExecutorService`, `TaskScheduler`, or reactive timers).
5. **Latency Claims**: Do not claim sub-100ms latency without measuring and recording it with benchmark metrics.
6. **Incremental Development**: Work incrementally with small, coherent commits/steps.
7. **Continuous Verification**: Run tests after each major implementation step to guarantee clean builds.
8. **Merge Cleanliness**: Keep all Member 3 code modular and contained within `com.screening.backend.member3` to ensure seamless merging with other team branches.

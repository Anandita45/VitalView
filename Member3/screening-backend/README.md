# Screening Backend — Member 3 Implementation

Backend service for the clinical screening platform. This repository contains **Member 3: Core Service Orchestration & Real-Time Ingestion**.

---

## 1. System Architecture

```text
  +------------------+
  | Python AI Worker |
  +------------------+
           │
           │ HTTP POST /api/member3/sessions/{sessionId}/vitals
           ▼
  +-------------------------------------------------------------+
  |              Spring Boot Backend (Member 3)                 |
  |                                                             |
  |  VitalIngestionController ──> VitalValidationService        |
  |                                         │                   |
  |                                         ▼                   |
  |                               Session Verification          |
  |                                         │                   |
  |                                         ▼                   |
  |                                  VitalPublisher             |
  +-------------------------------------------------------------+
           │
           │ STOMP over WebSocket (/ws)
           ▼
  +-------------------------------------------------------------+
  |                   Broker: In-Memory /topic                  |
  |  - /topic/member3/session/{sessionId}/status                |
  |  - /topic/member3/session/{sessionId}/vitals                |
  +-------------------------------------------------------------+
           │
           ▼
  +------------------+
  | Frontend Client  |
  +------------------+
```

---

## 2. REST APIs

All Member 3 REST endpoints are prefixed under `/api/member3/sessions`:

| Method | Endpoint | Description | Expected Status |
|---|---|---|---|
| `POST` | `/api/member3/sessions` | Create a new session in `CREATED` state | `201 Created` |
| `GET` | `/api/member3/sessions/{id}` | Retrieve session details by UUID | `200 OK` (or `404 Not Found`) |
| `POST` | `/api/member3/sessions/{id}/initialize` | Transition `CREATED -> INITIALIZING` | `200 OK` |
| `POST` | `/api/member3/sessions/{id}/stabilize` | Transition `INITIALIZING -> STABILIZING`, record start | `200 OK` |
| `POST` | `/api/member3/sessions/{id}/start` | Verify $\ge 15$s elapsed, transition `STABILIZING -> RUNNING` | `200 OK` (or `400 Bad Request`) |
| `POST` | `/api/member3/sessions/{id}/vitals` | Ingest real-time vital readings from Python AI worker | `202 Accepted` |
| `POST` | `/api/member3/sessions/{id}/finalize` | Orchestrate finalization: `RUNNING -> FINALIZING -> COMPLETED/FAILED` | `200 OK` (or `400 Bad Request`) |

---

## 3. WebSocket & STOMP Streaming

- **WebSocket Handshake URL**: `/ws` (supports raw WebSocket and SockJS fallback)
- **Application Prefix**: `/app`
- **Broker Prefix**: `/topic`

### WebSocket Destinations
- **Session Status Updates**:
  `/topic/member3/session/{sessionId}/status`
  ```json
  {
    "sessionId": "49b4b087-75d1-419b-a010-09a369cfbb72",
    "status": "RUNNING",
    "timestamp": "2026-09-26T15:00:00Z"
  }
  ```
- **Real-Time Vitals**:
  `/topic/member3/session/{sessionId}/vitals`
  ```json
  {
    "sessionId": "49b4b087-75d1-419b-a010-09a369cfbb72",
    "timestamp": 1727351234000,
    "bpm": 74.5,
    "respiration": 16.2,
    "waveform": [0.12, 0.18, 0.23],
    "signalQuality": 0.96
  }
  ```

---

## 4. Python AI Worker Integration Contract

The Python AI worker transmits vital readings via HTTP POST to:
`POST /api/member3/sessions/{sessionId}/vitals`

### Ingestion Payload:
```json
{
  "timestamp": 1727351234000,
  "bpm": 74.5,
  "respiration": 16.2,
  "waveform": [0.12, 0.18, 0.23],
  "signalQuality": 0.96
}
```

### Ingestion Validation Invariants:
- `timestamp`: Required epoch milliseconds. Rejected if in the future (>10s drift) or excessively old (>24 hours).
- `bpm`: Must be between `1.0` and `300.0`.
- `respiration`: Must be between `1.0` and `100.0`.
- `waveform`: Non-null, non-empty list of points, maximum `1000` points per packet.
- `signalQuality`: Must be between `0.0` and `1.0`.
- `session`: Must exist and be in status `STABILIZING` or `RUNNING`.

### AI Worker Configuration:
Configured via `application.properties`:
```properties
app.ai.base-url=${AI_BASE_URL:http://localhost:8000}
app.ai.api-key=${AI_API_KEY:}
app.ai.timeout-ms=${AI_TIMEOUT_MS:5000}
```

---

## 5. Finalization Integration Boundary

Member 3 defines a pluggable finalization contract:
[`ScreeningFinalizationHandler`](file:///c:/Users/1306s/Desktop/tech_expo/screening-backend/src/main/java/com/screening/backend/member3/ai/handler/ScreeningFinalizationHandler.java)

```java
public interface ScreeningFinalizationHandler {
    boolean finalizeScreening(UUID sessionId);
}
```

### How Team Members Replace the Mock Handler:
A default [`MockScreeningFinalizationHandler`](file:///c:/Users/1306s/Desktop/tech_expo/screening-backend/src/main/java/com/screening/backend/member3/ai/handler/MockScreeningFinalizationHandler.java) is provided for tests and standalone development with `@ConditionalOnMissingBean(name = "customScreeningFinalizationHandler")`.

To connect the actual AI model inference or clinical report generation service, simply declare a Spring bean:
```java
@Component("customScreeningFinalizationHandler")
public class ProductionScreeningFinalizationHandler implements ScreeningFinalizationHandler {
    @Override
    public boolean finalizeScreening(UUID sessionId) {
        // 1. Call Python AI / report generation worker
        // 2. Return true on success, false on failure
    }
}
```
Member 3's `SessionService` will automatically inject and orchestrate this production bean.

---

## 6. Getting Started

### Prerequisites
- JDK 21+

### Build and Test
```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

### Run Locally
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

By default, the server starts on port `8081` (configurable via `PORT` environment variable).
- Health check: `http://localhost:8081/actuator/health`
- Info endpoint: `http://localhost:8081/actuator/info`

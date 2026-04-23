# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey 3.1) and Apache Tomcat for the University of Westminster 5COSC022W coursework.

---

## Technology Stack
- Java 21
- JAX-RS (Jersey 3.1.3)
- Apache Tomcat 11
- Maven
- In-memory storage (ConcurrentHashMap)

---

## How to Build and Run

### Prerequisites
- Java 21+
- Apache Maven
- Apache Tomcat 11
- NetBeans IDE (recommended)

### Steps
1. Clone the repository:
   git clone https://github.com/YOUR_USERNAME/smart-campus-api.git

2. Open in NetBeans: File → Open Project

3. Right-click project → Clean and Build

4. Right-click project → Run (deploys to Tomcat automatically)

5. API is live at: http://localhost:8080/api/v1

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1 | Discovery endpoint |
| GET | /api/v1/rooms | List all rooms |
| POST | /api/v1/rooms | Create a room |
| GET | /api/v1/rooms/{id} | Get a room |
| DELETE | /api/v1/rooms/{id} | Delete a room |
| GET | /api/v1/sensors | List all sensors |
| POST | /api/v1/sensors | Create a sensor |
| GET | /api/v1/sensors/{id} | Get a sensor |
| DELETE | /api/v1/sensors/{id} | Delete a sensor |
| GET | /api/v1/sensors/{id}/readings | Get reading history |
| POST | /api/v1/sensors/{id}/readings | Add a reading |

---

## Sample curl Commands

### 1. Discovery
curl -s http://localhost:8080/api/v1

### 2. Create a Room
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-001","name":"Main Lecture Hall","capacity":200}'

### 3. Filter Sensors by Type
curl -s "http://localhost:8080/api/v1/sensors?type=CO2"

### 4. Add a Reading
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 23.7}'

### 5. Delete Room with Sensors (409 Conflict)
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

### 6. Create Sensor with Invalid roomId (422)
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEST-001","type":"Temperature","roomId":"FAKE-ROOM"}'

### 7. Post Reading to MAINTENANCE Sensor (403)
curl -s -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 5}'

---

## Report: Answers to Coursework Questions

### Part 1.1 — JAX-RS Resource Lifecycle & Thread Safety

By default, JAX-RS creates a brand new instance of every resource class for each incoming HTTP request. This is called request-scoped lifecycle. For example, a new RoomResource object is instantiated when GET /api/v1/rooms arrives and discarded once the response is sent.

If instance fields like a HashMap were stored inside the resource class, they would reset on every request and all data would be lost. This is why all data lives in the DataStore singleton — one object created once and shared across all requests.

Because the Grizzly/Tomcat server is multithreaded, multiple requests can arrive simultaneously. Two threads writing to a plain HashMap at the same time can corrupt it. This project uses ConcurrentHashMap throughout DataStore to prevent this — it handles concurrent reads and writes safely without data corruption. For list mutations, a synchronized block protects the add operation since ConcurrentHashMap only protects map-level operations, not mutations to values inside it.

### Part 1.2 — HATEOAS and Hypermedia in REST

HATEOAS (Hypermedia As The Engine Of Application State) means API responses include navigable links to related resources rather than requiring clients to construct URLs themselves. It is considered advanced REST design because it makes the API self-describing.

In this project, GET /api/v1 returns a resources map with URLs for rooms and sensors. A client can start at /api/v1 and navigate the entire API without reading external documentation.

Benefits over static documentation:
- Discoverability: clients explore the API dynamically
- Reduced coupling: clients follow links so the server can change URLs without breaking clients
- Self-documenting: new developers understand available resources by calling one endpoint
- Reduced errors: clients cannot accidentally construct malformed URLs

### Part 2.1 — IDs Only vs Full Objects in List Responses

Returning only IDs produces a tiny payload but forces the client to make a separate GET request for each item — the N+1 problem. For 100 rooms that means 101 HTTP requests.

Returning full objects gives clients everything in one response with no extra round trips. This is better for small-to-medium collections and critical in high-latency environments like IoT. For large datasets, pagination should be added to balance completeness and bandwidth.

### Part 2.2 — Idempotency of DELETE

Yes, DELETE is idempotent in this implementation. HTTP idempotency means calling the same operation multiple times produces the same server state as calling it once.

First DELETE /api/v1/rooms/HALL-001 removes the room and returns 204 No Content. A second identical call finds the room already gone and returns 404 Not Found. The server state after both calls is identical — the room does not exist — which satisfies idempotency. The different response codes are informational but do not indicate a side effect.

### Part 3.1 — @Consumes and Content-Type Mismatch

When a method is annotated with @Consumes(APPLICATION_JSON), JAX-RS registers it as only accepting JSON. If a client sends Content-Type: text/plain or application/xml, JAX-RS cannot find a MessageBodyReader for that media type and immediately returns 415 Unsupported Media Type without invoking the resource method at all. The annotation acts as a contract enforced entirely by the framework, keeping resource methods clean.

### Part 3.2 — @QueryParam vs @PathParam for Filtering

Path parameter approach /sensors/type/CO2 implies CO2 is a distinct resource, which is semantically wrong. It cannot be made optional and combining filters becomes awkward.

Query parameter approach /sensors?type=CO2 clearly communicates filtering. It is optional by design, composable (?type=CO2&status=ACTIVE), and follows HTTP conventions that clients and proxies understand correctly. The collection URI remains stable regardless of filter combinations.

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator involves a method with @Path but no HTTP annotation. Instead of handling the request it returns an instance of another class. JAX-RS then delegates the actual request to that object.

Benefits:
1. Single Responsibility: SensorResource handles sensor CRUD, SensorReadingResource handles readings
2. Reduced complexity: no need to define every nested path in one massive class
3. Independent testing: SensorReadingResource can be unit tested in isolation
4. Reusability: the same sub-resource class could serve multiple parent resources
5. Maintainability: different developers can own different resource classes without conflicts

### Part 5.2 — Why 422 is More Accurate than 404

404 means the URL requested does not exist. The URL /api/v1/sensors is valid so 404 would mislead the client into thinking they hit a wrong endpoint.

422 Unprocessable Entity means the request was syntactically valid JSON but failed semantic validation. The JSON was well-formed and the endpoint is correct, but the value of roomId inside the body references a resource that does not exist. 422 tells the client precisely to check the value they are sending, not the URL.

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

1. Internal path disclosure: file system paths reveal server directory layout
2. Technology fingerprinting: library names and versions allow attackers to look up known CVEs
3. Business logic exposure: class and method names reveal internal architecture
4. Database schema leakage: failed queries expose table and column names
5. Attack surface mapping: exception types reveal what conditions cause failures

This project logs full traces internally only and returns a generic safe message externally.

### Part 5.5 — Filters vs Manual Logging

1. DRY principle: logging logic exists in one place not dozens of methods
2. Guaranteed coverage: a filter applies to every request automatically and cannot be forgotten
3. Separation of concerns: business logic stays clean, logging is infrastructure
4. Easy to toggle: removing @Provider disables logging globally
5. Consistent format: every log entry has identical structure
6. Pre/post interception: filters access both raw request and final response enabling accurate timing and status logging impossible to achieve inside resource methods

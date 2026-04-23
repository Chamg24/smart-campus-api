# Smart Campus Sensor & Room Management API

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-9-blue?style=flat-square)
![Jersey](https://img.shields.io/badge/Jersey-3.1.3-green?style=flat-square)
![Tomcat](https://img.shields.io/badge/Tomcat-11-red?style=flat-square&logo=apache-tomcat)
![Maven](https://img.shields.io/badge/Maven-Build-purple?style=flat-square&logo=apache-maven)
![REST API](https://img.shields.io/badge/REST-API-yellow?style=flat-square)

A fully functional RESTful API built for the University of Westminster **5COSC022W Client-Server Architectures** coursework. The system manages physical Rooms and Sensors, including nested Sensor Readings, with robust exception mapping, request/response logging, and rule-based validation.

---

##  Table of Contents

- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Data Models](#data-models)
- [API Endpoints](#api-endpoints)
- [Error Handling](#error-handling)
- [Project Structure](#project-structure)
- [How to Build and Run](#how-to-build-and-run)
- [Sample curl Commands](#sample-curl-commands)
- [Conceptual Report — Question Answers](#conceptual-report--question-answers)

---

##  Tech Stack

| Technology | Version | Role |
|---|---|---|
| Java | 21 | Core language |
| JAX-RS / Jersey | 3.1.3 | REST implementation |
| Apache Tomcat | 11 | Application server |
| Maven | 3.6+ | Build and dependency management |
| ConcurrentHashMap | — | Thread-safe in-memory storage |
| Jackson | 2.x | JSON serialisation |

> **Why Tomcat 11?** Jersey 3.x uses the `jakarta.ws.rs.*` namespace (Jakarta EE 9+). Tomcat 9 and 10 only support the legacy `javax.ws.rs.*` namespace and are **not compatible** with this project.

---

## System Architecture

```
HTTP Request
     |
[Apache Tomcat 11]
     |
[Jersey JAX-RS Router]
     |
[Resource Layer]
  - DiscoveryResource     GET /api/v1
  - RoomResource          /api/v1/rooms
  - SensorResource        /api/v1/sensors
  - SensorReadingResource /api/v1/sensors/{id}/readings  ← Sub-Resource Locator
     |
[DataStore Singleton]
  - rooms:    ConcurrentHashMap<String, Room>
  - sensors:  ConcurrentHashMap<String, Sensor>
  - readings: ConcurrentHashMap<String, List<SensorReading>>
     |
[Exception Mappers + Logging Filter]
  - RoomNotEmptyException      -> 409 Conflict
  - LinkedResourceNotFound     -> 422 Unprocessable Entity
  - SensorUnavailable          -> 403 Forbidden
  - GlobalExceptionMapper      -> 500 Internal Server Error
  - LoggingFilter              -> logs all requests and responses
```

---

## Data Models

### Room

| Field | Type | Description |
|---|---|---|
| id | String | Unique identifier e.g. `LIB-301` |
| name | String | Human-readable room name |
| capacity | int | Maximum occupancy |
| sensorIds | List\<String\> | IDs of sensors deployed in this room |

```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 40,
  "sensorIds": ["TEMP-001", "OCC-001"]
}
```

### Sensor

| Field | Type | Description |
|---|---|---|
| id | String | Unique identifier e.g. `TEMP-001` |
| type | String | Category e.g. `Temperature`, `CO2`, `Occupancy` |
| status | String | `ACTIVE`, `MAINTENANCE`, or `OFFLINE` |
| currentValue | double | Most recent measurement |
| roomId | String | Parent room ID |

```json
{
  "id": "TEMP-001",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 21.5,
  "roomId": "LIB-301"
}
```

### SensorReading

| Field | Type | Description |
|---|---|---|
| id | String | UUID auto-generated on creation |
| timestamp | long | Epoch milliseconds when reading was captured |
| value | double | The actual measured value |

```json
{
  "id": "a3f9c1b2-...",
  "timestamp": 1776877483366,
  "value": 23.7
}
```

> `id` and `timestamp` are auto-generated server-side if not provided in the request body.

---

##  API Endpoints

Base URL: `http://localhost:8080/api/v1`

### Discovery

| Method | Endpoint | Description | Status |
|---|---|---|---|
| GET | `/api/v1` | Returns API metadata and resource links (HATEOAS) | 200 |

### Rooms

| Method | Endpoint | Description | Status |
|---|---|---|---|
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a new room | 201 + Location |
| GET | `/api/v1/rooms/{id}` | Get a specific room | 200 / 404 |
| PUT | `/api/v1/rooms/{id}` | Update a room | 200 / 404 |
| DELETE | `/api/v1/rooms/{id}` | Delete a room | 204 / 404 / 409 |

### Sensors

| Method | Endpoint | Description | Status |
|---|---|---|---|
| GET | `/api/v1/sensors` | List all sensors — optional `?type=` filter | 200 |
| POST | `/api/v1/sensors` | Register a new sensor | 201 + Location |
| GET | `/api/v1/sensors/{id}` | Get a specific sensor | 200 / 404 |
| PUT | `/api/v1/sensors/{id}` | Update sensor metadata | 200 / 404 |
| DELETE | `/api/v1/sensors/{id}` | Delete a sensor | 204 / 404 |

### Sensor Readings (Sub-Resource Locator)

| Method | Endpoint | Description | Status |
|---|---|---|---|
| GET | `/api/v1/sensors/{id}/readings` | Get full reading history | 200 / 404 |
| POST | `/api/v1/sensors/{id}/readings` | Add a new reading | 201 / 403 / 404 |

> `SensorResource` uses the JAX-RS sub-resource locator pattern — `@Path("{sensorId}/readings")` on a method with **no HTTP verb annotation** delegates handling to `SensorReadingResource` at runtime.

---

## Error Handling

All error responses return a consistent JSON body with a numeric `status` field:

```json
{
  "status": 409,
  "error": "Room Not Empty",
  "message": "Cannot delete room 'LIB-301' it still has 2 sensor(s). Remove all sensors first."
}
```

| Exception | HTTP Status | When it triggers |
|---|---|---|
| `RoomNotEmptyException` | 409 Conflict | Deleting a room that still has sensors attached |
| `LinkedResourceNotFoundException` | 422 Unprocessable Entity | Creating a sensor with a `roomId` that does not exist |
| `SensorUnavailableException` | 403 Forbidden | Posting a reading to a sensor in `MAINTENANCE` status |
| `GlobalExceptionMapper` | 500 Internal Server Error | Any unexpected uncaught runtime exception |

> Each exception has its own dedicated `@Provider ExceptionMapper` — there is no switch-based global mapper for business exceptions.

---

##  Project Structure

```
src/main/java/com/smartcampus/
├── exception/
│   ├── GlobalExceptionMapper.java
│   ├── LinkedResourceNotFoundException.java
│   ├── RoomNotEmptyException.java
│   └── SensorUnavailableException.java
├── filter/
│   └── LoggingFilter.java
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── resource/
│   ├── DiscoveryResource.java
│   ├── RoomResource.java
│   ├── SensorReadingResource.java
│   └── SensorResource.java
├── store/
│   └── DataStore.java
└── SmartCampusApplication.java
src/main/webapp/
└── WEB-INF/
    └── web.xml
pom.xml
```

---

##  How to Build and Run

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Java JDK | 21 |
| Apache Maven | 3.6+ |
| Apache Tomcat | **11.x** |

>  Tomcat 9 or 10 will not work — this project uses `jakarta.ws.rs.*` which requires Tomcat 11.

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/Chamg24/smart-campus-api.git
   ```

2. Open in NetBeans via **File → Open Project**

3. Right-click the project → **Clean and Build**

4. Right-click the project → **Run** — this deploys the WAR to Tomcat automatically

5. The API is now live at:
   ```
   http://localhost:8080/api/v1
   ```

> **Data is in-memory only** — the DataStore starts empty on every deployment. All data is lost when Tomcat restarts.

---

##  Sample curl Commands

### 1. Discovery endpoint

```bash
curl -X GET http://localhost:8080/api/v1
```

Expected response:

```json
{
  "version": "1.0.0",
  "description": "Smart Campus Sensor & Room Management API",
  "contact": "admin@smartcampus.ac.uk",
  "status": "operational",
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

### 2. Create a Room

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-001","name":"Main Lecture Hall","capacity":200}'
```

Expected: `201 Created` with a `Location` header pointing to `/api/v1/rooms/HALL-001`

### 3. Get all Rooms

```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 4. Update a Room

```bash
curl -X PUT http://localhost:8080/api/v1/rooms/HALL-001 \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated Lecture Hall","capacity":250}'
```

### 5. Register a Sensor

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","roomId":"HALL-001"}'
```

Expected: `201 Created` with a `Location` header pointing to `/api/v1/sensors/TEMP-001`

### 6. Filter Sensors by Type

```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

Expected: JSON array containing only CO2-type sensors

### 7. Add a Sensor Reading

```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 23.7}'
```

Expected: `201 Created`. Also updates `TEMP-001` `currentValue` to `23.7`

### 8. Get all Readings for a Sensor

```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 9. Attempt to Delete a Room with Active Sensors

```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/HALL-001
```

Expected: `409 Conflict` with JSON error message

### 10. Create Sensor with Invalid roomId

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEST-001","type":"Temperature","roomId":"FAKE-ROOM"}'
```

Expected: `422 Unprocessable Entity`

### 11. Post Reading to a MAINTENANCE Sensor

First set the sensor to maintenance:
```bash
curl -X PUT http://localhost:8080/api/v1/sensors/TEMP-001 \
  -H "Content-Type: application/json" \
  -d '{"status":"MAINTENANCE"}'
```

Then attempt a reading:
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 5}'
```

Expected: `403 Forbidden`

### 12. Trigger the Global 500 Exception Handler

```bash
curl -X GET http://localhost:8080/api/v1/crash
```

Expected: `500 Internal Server Error` with a clean JSON response — no stack trace exposed to the client.

---

## 📝 Conceptual Report — Question Answers

### Part 1.1 — JAX-RS Resource Lifecycle and Thread Safety

By default, JAX-RS creates a brand new instance of every resource class for each incoming HTTP request. This is called request-scoped lifecycle. For example, a new `RoomResource` object is instantiated when `GET /api/v1/rooms` arrives and is discarded once the response is sent. This is the default behaviour unless explicitly changed with annotations like `@Singleton`.

This lifecycle decision has a direct impact on how in-memory data must be managed. If data were stored as instance fields inside the resource class, it would be lost after every single request because the object is thrown away. This is why all persistent data in this project lives in the `DataStore` class, which follows the Singleton pattern. `DataStore` is created once when the application starts and the same instance is shared across every request for the entire lifetime of the server.

Because Apache Tomcat is a multithreaded server, multiple HTTP requests can arrive and execute simultaneously on different threads. If two threads both called `rooms.put()` on a plain `HashMap` at the same time, the internal structure of the map could become corrupted, causing data loss or incorrect results. To prevent this, `DataStore` uses `ConcurrentHashMap` for all three collections — rooms, sensors, and readings. `ConcurrentHashMap` is specifically designed for concurrent access and handles multiple simultaneous read and write operations safely without requiring manual synchronisation on every call. For list-level mutations such as adding a new `SensorReading` to a sensor's history list, an additional `synchronized` block is used because `ConcurrentHashMap` only protects the map's own operations, not mutations to the objects stored as values inside it.

### Part 1.2 — HATEOAS and Hypermedia in REST

HATEOAS stands for Hypermedia As The Engine Of Application State. It is the principle that REST API responses should include navigable links to related resources and actions, rather than requiring clients to construct URLs from external documentation or prior knowledge. It is considered a hallmark of advanced RESTful design because it makes an API self-describing, discoverable, and loosely coupled.

In this project the discovery endpoint `GET /api/v1` returns a `resources` map that contains the full URLs for the rooms and sensors collections. A client application can make a single request to `/api/v1`, read the links from the response, and immediately know how to reach every part of the API without any hardcoded paths.

Compared to static documentation, HATEOAS offers several significant benefits. First, discoverability means clients explore the API at runtime rather than relying on documentation that may be outdated. Second, reduced coupling means that if a URL changes on the server side, clients following links from the discovery response adapt automatically without code changes. Third, the API is self-documenting so new developers can understand what resources are available just by calling one endpoint. Fourth, errors are reduced because clients never need to manually construct paths that could be mistyped or versioned incorrectly. The server always provides the canonical link.

### Part 2.1 — Returning IDs Only vs Full Objects in List Responses

When returning a collection from `GET /api/v1/rooms` there are two common design choices. The first is to return only the IDs of each room, producing a very small payload. The second is to return the full Room objects including all fields.

Returning only IDs keeps each response extremely lightweight and reduces bandwidth usage, which matters in constrained network environments. However, the client must then make a separate `GET /api/v1/rooms/{id}` call for every room it wants to display or process. For a list of 100 rooms this produces 101 HTTP requests — one for the list and one hundred for the details. This is known as the N+1 problem and is highly inefficient in terms of latency and server load.

Returning full objects solves the N+1 problem entirely. The client receives everything it needs in a single response and can render or process it immediately without additional round trips. This approach is well suited to small and medium sized collections and is especially important in campus IoT environments where sensor dashboards need to load quickly. The trade-off is a larger payload per response. For very large datasets the best practice is to return full objects combined with server-side pagination, providing the benefits of completeness while keeping individual response sizes manageable.

### Part 2.2 — Idempotency of DELETE

HTTP idempotency means that making the same request multiple times produces the same server state as making it once. DELETE is defined as idempotent in the HTTP specification.

In this implementation the behaviour is as follows. The first call to `DELETE /api/v1/rooms/HALL-001`, assuming the room exists and has no sensors, removes the room from the DataStore and returns `204 No Content`. A second identical call finds the room is already gone and returns `404 Not Found`. The response code is different but crucially the server state is identical after both calls — the room does not exist in either case. No additional data is removed or changed by the second call, which satisfies the idempotency requirement.

If a deletion is blocked because the room still has sensors, the endpoint returns `409 Conflict` and the room remains untouched. Repeating that same call continues to return `409` and continues to leave the room untouched. Again the server state does not keep changing with repeated calls, which is the key property of idempotency. The response code varying between `204` and `404` is acceptable and does not violate idempotency because idempotency is about server state, not response codes.

### Part 3.1 — @Consumes Annotation and Content-Type Mismatch

When a resource method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, it declares a formal contract that this endpoint only accepts requests with a `Content-Type` of `application/json`. JAX-RS uses this annotation during a process called content negotiation.

If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, the JAX-RS runtime cannot find a registered `MessageBodyReader` that knows how to deserialise that media type into the expected Java object. As a result it immediately returns an HTTP `415 Unsupported Media Type` response without ever invoking the resource method or running any business logic. The developer does not need to write any validation code to handle this case. The annotation itself enforces the contract automatically at the framework level.

If the client sends the correct `Content-Type` of `application/json` but the JSON body is malformed or syntactically invalid, the failure happens at the deserialization stage and typically produces a `400 Bad Request` response. The distinction is important — `415` means wrong format, `400` means correct format but invalid content.

### Part 3.2 — @QueryParam vs @PathParam for Filtering Collections

Using a path parameter for filtering, such as `GET /api/v1/sensors/type/CO2`, has several significant problems. Structurally it implies that `CO2` is a resource with its own identity, which is semantically incorrect — `CO2` is a filter criterion, not a resource. It creates a URL that looks like a sub-resource path rather than a filtered collection. It cannot be made optional, meaning the unfiltered version `GET /api/v1/sensors` and the filtered version would need to be separate routes. Combining multiple filters becomes very awkward, potentially requiring paths like `/sensors/type/CO2/status/ACTIVE`.

Using a query parameter, such as `GET /api/v1/sensors?type=CO2`, is clearly the superior approach for filtering. The base collection URI `/api/v1/sensors` remains stable and clean regardless of any filters applied. The `type` parameter is naturally optional — omitting it returns all sensors. Multiple filters compose elegantly using standard query string syntax such as `?type=CO2&status=ACTIVE`. This pattern follows widely accepted REST conventions and is immediately understood by client developers, API testing tools, and HTTP proxies. It also correctly represents the intent: the client is retrieving the sensors collection with a filter applied, not navigating to a different resource.

### Part 4.1 — Sub-Resource Locator Pattern and Architectural Benefits

The sub-resource locator pattern in JAX-RS involves a resource method that has a `@Path` annotation but **no HTTP method annotation** such as `@GET` or `@POST`. This absence is what distinguishes a locator from a regular endpoint. Instead of handling the HTTP request directly, the method returns an instance of another class. JAX-RS then routes the actual request — whether it is a `GET`, `POST`, or any other method — to that returned object for processing.

In this project `SensorResource` contains a method annotated with `@Path("/{sensorId}/readings")` that returns a new instance of `SensorReadingResource`. JAX-RS calls this locator method first to get the delegate object, then dispatches the real request to `SensorReadingResource`.

This pattern provides several important architectural benefits. First, it enforces the Single Responsibility Principle — `SensorResource` handles sensor CRUD operations and nothing else, while `SensorReadingResource` focuses entirely on reading history management. Second, it dramatically reduces complexity in large APIs by preventing any single class from growing into an unmanageable collection of unrelated methods. Third, `SensorReadingResource` can be instantiated and tested independently without needing to route through the full JAX-RS stack. Fourth, in a team environment different developers can own and modify different resource classes without causing merge conflicts in shared files.

### Part 5.2 — Why HTTP 422 is More Semantically Accurate than 404

When a client POSTs a new sensor with a `roomId` value that does not correspond to any existing room, the choice of error code matters significantly for client developers.

HTTP `404 Not Found` means the URL that was requested does not exist on the server. However, the URL `/api/v1/sensors` is entirely valid and the endpoint is functioning correctly. Returning `404` would strongly imply to the client that they have called a wrong or non-existent URL, which would send debugging efforts in completely the wrong direction.

HTTP `422 Unprocessable Entity` means the server understood the request, the `Content-Type` was correct, the JSON was syntactically valid, but the semantic content of the payload cannot be processed. The request was perfectly formed — the problem is that the value of `roomId` inside the body references a resource that does not exist in the system. This is a referential integrity violation within the payload, not a URL routing problem.

`422` communicates precisely what is wrong: the payload is semantically invalid. This guides the client developer to inspect the data they are sending rather than the URL they are calling. It also allows client-side error handling and retry logic to correctly distinguish between a missing endpoint (`404`) and a data validation failure (`422`), which are fundamentally different problems requiring different fixes.

### Part 5.4 — Cybersecurity Risks of Exposing Java Stack Traces

Exposing raw Java stack traces in API responses to external consumers creates multiple serious security vulnerabilities.

First, **internal path disclosure** occurs because stack traces include full file system paths such as `/home/ubuntu/smartcampus/src/main/java/com/smartcampus/resource/RoomResource.java`, revealing the server directory layout and deployment structure to anyone who reads the response.

Second, **technology fingerprinting** becomes trivial because the trace includes exact library names and version numbers such as `jersey-server-3.1.3` or `jackson-databind-2.15.0`. Attackers can search public vulnerability databases for known CVEs affecting those exact versions and attempt targeted exploits.

Third, **business logic is exposed** because the sequence of class and method names in a stack trace reveals the internal code flow, naming conventions, and architectural decisions of the application, effectively giving an attacker a map of how the system works.

Fourth, if a database query fails, the trace would expose table names, column names, and sometimes SQL query fragments, revealing the data schema.

Fifth, **exception types reveal what conditions trigger failures**. An attacker can deliberately send malformed requests to explore different error paths and build a comprehensive picture of the system's boundaries and weaknesses.

This project addresses all of these risks through the `GlobalExceptionMapper` which intercepts every uncaught `Throwable`. It logs the complete stack trace internally using `java.util.logging.Logger` where only authorised developers and operations staff can access it through server logs. The external HTTP response contains only a generic, safe message with no internal details whatsoever, making it impossible for attackers to gain useful information from error responses.

### Part 5.5 — Why JAX-RS Filters are Superior to Manual Logging

Implementing logging as a cross-cutting concern using JAX-RS filters rather than inserting `Logger.info()` statements manually inside every resource method offers several fundamental advantages.

The first advantage is the **DRY principle** — Do Not Repeat Yourself. All logging logic exists in exactly one class, `LoggingFilter`. Changing the log format, switching to a different logging framework, or adjusting log levels requires editing a single file rather than searching through every resource method across the entire codebase.

The second advantage is **guaranteed coverage**. A filter class registered with `@Provider` is automatically applied to every incoming request by the JAX-RS runtime. Manual logging can be accidentally omitted from a newly added resource method, creating gaps in observability. A filter cannot be forgotten because it applies globally without any action required in individual methods.

The third advantage is **separation of concerns**. Resource methods should contain only business logic — validating input, calling the data store, and building responses. Mixing logging statements into business logic makes methods longer, harder to read, and harder to test.

The fourth advantage is **easy toggling**. Disabling logging application-wide requires only removing the `@Provider` annotation or unregistering the filter. With manual logging, disabling it would require commenting out or deleting statements across potentially dozens of methods.

The fifth advantage is that the filter has privileged access to both the `ContainerRequestContext` before the resource method runs and the `ContainerResponseContext` after the response is built. This enables accurate logging of the final HTTP status code, which is impossible to achieve cleanly from inside a resource method since the status is determined by the response object that is built and potentially modified during the response pipeline.

---

##  Module Information

| | |
|---|---|
| Module | 5COSC022W Client-Server Architectures |
| University | University of Westminster |
| Academic Year | 2025/26 |

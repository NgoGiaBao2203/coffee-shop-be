# Coffee Shop — Backend

Spring Boot + Java REST API for the Coffee Shop management system, covering shops, staff, drinks, categories, tables, invoices, and revenue reports.

---

## Tech Stack

| Layer               | Library                                                                           |
| ------------------- | --------------------------------------------------------------------------------- |
| Framework           | Spring Boot 4.0.7 + Java 21                                                       |
| Build Tool          | Maven + Maven Wrapper                                                             |
| REST API            | Spring Web MVC + Jakarta Validation                                               |
| Security            | Spring Security + BCrypt + role-based authorisation                               |
| Database            | PostgreSQL (Supabase)                                                             |
| Data Access         | MyBatis 4.0.1 + XML mappers                                                       |
| Database Migrations | Flyway                                                                            |
| File Storage        | Supabase Storage via AWS SDK v2 (S3-compatible API), with a local filesystem mode |
| API Documentation   | Springdoc OpenAPI 3.0.2 + Swagger UI                                              |
| Testing & Coverage  | JUnit 5 + Mockito + JaCoCo                                                        |
| Formatter           | Spotless + Google Java Format                                                     |
| Containerisation    | Docker + Docker Compose                                                           |

---

## Getting Started

Requirements: JDK 21 and access to the project's Supabase PostgreSQL database. Both checked-in profiles enable Supabase Storage; configure storage credentials for uploads or explicitly select local storage as described below. The Maven Wrapper downloads Maven on first use; Maven must also be installed locally to use scripts that call `mvn` directly.

All commands below run from the backend repository root (`coffee-shop-be/`) unless stated otherwise.

```bash
# Enter the Spring Boot project
cd api

# Replace placeholders with your Supabase database connection values
export DB_HOST='<database-host>'
export DB_PORT='<database-port>'
export DB_NAME='<database-name>'
export DB_USERNAME='<database-username>'
export DB_PASSWORD='<database-password>'

# Configure Supabase Storage for your project
export SUPABASE_BUCKET_NAME=drinks
export SUPABASE_REGION='<storage-s3-region>'
export SUPABASE_ENDPOINT='https://<project-ref>.storage.supabase.co/storage/v1/s3'
export SUPABASE_PUBLIC_URL='https://<project-ref>.supabase.co/storage/v1/object/public'
export SUPABASE_ACCESS_KEY='<storage-access-key>'
export SUPABASE_SECRET_KEY='<storage-secret-key>'

# Start development server
bash scripts/run-spring-boot.sh
```

The application connects to the existing PostgreSQL database hosted on Supabase; no local PostgreSQL installation is required. Use the connection values for your Supabase project. The configured database user must have permission to apply schema changes. Flyway applies pending migrations from `src/main/resources/db/migration/` automatically on startup. Migration files define schema changes and do not seed accounts; the login examples use an account already stored in the Supabase database.

- Development server: `http://localhost:8081`
- API base URL: `http://localhost:8081/api`
- Default Spring profile: `dev`

For the current frontend, set `VITE_API_HOST=http://localhost:8081/api`; its Axios client uses this as the base URL and its endpoint paths omit the `/api` prefix.

---

## Scripts Folder (`api/scripts/`)

Shell scripts for CI and local workflows. Run these commands from `api/`.

```bash
# Start Spring Boot with development profile (default)
bash scripts/run-spring-boot.sh

# Start Spring Boot with production profile
bash scripts/run-spring-boot.sh prod

# Clean + run tests + generate JaCoCo report and attempt to open it
bash scripts/run-test.sh

# Format Java source files with Spotless
bash scripts/format.sh

# Clean and build the deployable JAR (includes tests)
bash scripts/run-build-package.sh

# Run tests and verification, including the JaCoCo coverage check
bash scripts/run-verify.sh

# Clean, build, and install artifacts into the local Maven repository
bash scripts/run-install.sh

# Remove build artifacts
bash scripts/run-clean.sh
```

`run-build-package.sh`, `run-verify.sh`, `run-install.sh`, and `run-clean.sh` use the locally installed `mvn`. The other scripts use `./mvnw`.

---

## Maven Commands

Run from `api/`. On Windows, use `mvnw.cmd` instead of `./mvnw`.

| Command                                                  | Description                                                 |
| -------------------------------------------------------- | ----------------------------------------------------------- |
| `./mvnw spring-boot:run`                                 | Start the application using the default `dev` profile       |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` | Start with production configuration                         |
| `./mvnw test`                                            | Run tests and generate the JaCoCo report                    |
| `./mvnw clean package`                                   | Clean, test, and build `target/api-0.0.1-SNAPSHOT.jar`      |
| `./mvnw verify`                                          | Run verification, including coverage checks                 |
| `./mvnw clean install`                                   | Build and install artifacts into the local Maven repository |
| `./mvnw spotless:apply`                                  | Format Java source files                                    |
| `./mvnw spotless:check`                                  | Check Java formatting                                       |
| `./mvnw clean`                                           | Remove `target/`                                            |

Coverage report: `target/site/jacoco/index.html` when working from `api/`. JaCoCo checks a minimum line coverage of 80% per included service package during verification.

---

## Project Structure

```text
api/
├── pom.xml                              # Dependencies, build plugins, coverage rules
├── mvnw / mvnw.cmd                      # Maven Wrapper for Linux/macOS and Windows
├── Dockerfile                           # Multi-stage Java 21 build and runtime
├── docker-compose.yml                   # Production API container configuration
├── scripts/                             # Run, build, test, and formatting scripts
│
└── src/
    ├── main/
    │   ├── java/coffee/api/
    │   │   ├── ApiApplication.java       # Spring Boot entry point
    │   │   ├── config/                   # Security, CORS, storage, UUID, and MDC configuration
    │   │   ├── controllers/              # REST API endpoints grouped by feature
    │   │   │   ├── common/               # Login / user profile
    │   │   │   ├── category/             # Drink categories
    │   │   │   ├── drink/                # Drinks and image uploads
    │   │   │   ├── detail/               # Drink details
    │   │   │   ├── dropdown/             # Category, role, and shop dropdowns
    │   │   │   ├── invoice/              # Create, edit, pay, and cancel invoices
    │   │   │   ├── revenue/              # Revenue reports
    │   │   │   ├── shop/                 # Shop management
    │   │   │   ├── staff/                # Staff management
    │   │   │   └── table/                # Table management
    │   │   ├── dto/
    │   │   │   ├── request/              # Request DTOs and validation constraints
    │   │   │   ├── response/             # Response DTOs and shared response types
    │   │   │   └── result/               # Query result DTOs
    │   │   ├── enums/                    # Roles, response codes, validation messages, sorting
    │   │   ├── exceptions/               # Domain exceptions and global error handling
    │   │   ├── filter/                   # Request logging / MDC filter
    │   │   ├── mapper/                   # MyBatis mapper interfaces
    │   │   ├── model/                    # User profile model
    │   │   ├── security/                 # User details and X-USER-ID authentication filter
    │   │   ├── services/
    │   │   │   ├── services_interface/   # Service contracts grouped by feature
    │   │   │   └── services_implement/   # Business logic and feature validators
    │   │   └── utils/                    # Shared helpers
    │   └── resources/
    │       ├── application.yaml          # Application name, /api prefix, default profile
    │       ├── application-dev.yaml      # Development database, storage, CORS, and logging
    │       ├── application-prod.yaml     # Production configuration
    │       ├── db/migration/             # Versioned Flyway SQL migrations
    │       └── mapper/                   # MyBatis XML SQL mappings
    └── test/java/coffee/api/
        └── services/services_implement/  # Service tests grouped by feature
```

---

## Adding a New Feature

Java package paths below are relative to `api/src/main/java/coffee/api/`; resource and test paths are relative to `api/`.

1. Create request DTOs under `dto/request/<feature>/` and add validation constraints.
2. Create result and response DTOs under `dto/result/` and `dto/response/<feature>/`.
3. Add a mapper interface under `mapper/` and its SQL mapping under `src/main/resources/mapper/`. Match the XML namespace and statement IDs to the interface.
4. Add the service contract under `services/services_interface/<feature>/` and implementation under `services/services_implement/<feature>/`.
5. Add a controller under `controllers/<feature>/`, validate requests with `@Valid`, and declare role access with `@PreAuthorize` where required.
6. Add a new versioned SQL migration under `src/main/resources/db/migration/` if the schema changes.
7. Add service tests under `src/test/java/coffee/api/services/services_implement/<feature>/`, then run formatting and verification.

---

## Environment Variables

Export variables in the shell before starting the application. Spring Boot scripts do not automatically load a `.env` file.

| Variable                   | Purpose                             | Development                   | Production         |
| -------------------------- | ----------------------------------- | ----------------------------- | ------------------ |
| `SPRING_PROFILES_ACTIVE`   | Active Spring profile               | Defaults to `dev`             | Set to `prod`      |
| `SERVER_PORT`              | HTTP port                           | Defaults to `8081`            | Defaults to `8080` |
| `DB_HOST`                  | Supabase database / pooler hostname | Set privately                 | Required           |
| `DB_PORT`                  | Supabase database / pooler port     | Set privately                 | Required           |
| `DB_NAME`                  | Supabase database name              | Set privately                 | Required           |
| `DB_USERNAME`              | Supabase database username          | Set privately                 | Required           |
| `DB_PASSWORD`              | Supabase database password          | Set privately                 | Required           |
| `STORAGE_SUPABASE_ENABLED` | Use Supabase instead of local files | Defaults to `true`            | Defaults to `true` |
| `SUPABASE_BUCKET_NAME`     | Storage bucket                      | Defaults to `drinks`          | Required           |
| `SUPABASE_REGION`          | Storage S3 region                   | Defaults to `ap-southeast-1`  | Required           |
| `SUPABASE_ENDPOINT`        | S3-compatible storage endpoint      | Set for your Supabase project | Required           |
| `SUPABASE_PUBLIC_URL`      | Public object URL prefix            | Set for your Supabase project | Required           |
| `SUPABASE_ACCESS_KEY`      | Storage S3 access key               | Set your credentials          | Required           |
| `SUPABASE_SECRET_KEY`      | Storage S3 secret key               | Set your credentials          | Required           |

The `dev` profile contains fallback database and storage values. Export the connection values and storage credentials for the Supabase project you are using. The `prod` profile has no defaults for the database and storage placeholders listed above.

`run-spring-boot.sh` explicitly selects its first argument as the profile, defaulting to `dev`; use `bash scripts/run-spring-boot.sh prod` to run production through that script.

- Edit `api/src/main/resources/application-dev.yaml` for local configuration and CORS origins. Development currently allows `http://localhost:5173` and `http://localhost:3000`.
- Edit `api/src/main/resources/application-prod.yaml` for production configuration and allowed origin patterns.
- Supabase endpoint format: `https://<project-ref>.storage.supabase.co/storage/v1/s3`.
- Public URL prefix format: `https://<project-ref>.supabase.co/storage/v1/object/public`.

For local image storage, set `STORAGE_SUPABASE_ENABLED=false`. Files are written to `uploads/drinks/` relative to the application's working directory; the service returns paths such as `/uploads/drinks/<filename>`, which are served under `/api/uploads/drinks/<filename>` with the current servlet prefix. Image uploads accept `.jpg` and `.png` extensions.

See [Supabase Storage Setup](api/SUPABASE_STORAGE_SETUP.md) for additional storage notes. Container configuration is in `api/Dockerfile` and `api/docker-compose.yml`:

- The Docker image runs with the `prod` profile on port `8080`; Compose maps host port `8080` to container port `8080`.
- Before using Compose, add `SUPABASE_PUBLIC_URL: ${SUPABASE_PUBLIC_URL}` to the service's `environment` mapping. The production profile requires this variable, but the current Compose file does not forward it. A value in `.env` alone does not pass it into the container.
- Review health checks before deployment: Compose checks `/actuator/health`, while the Dockerfile checks `/api/actuator/health`. The latter also maps to a custom controller that requires the `OWNER` role; the Dockerfile's request sends no authentication header.

---

## API Conventions

The REST API uses the `/api` servlet prefix. Requests and responses use JSON, except drink image uploads, which use `multipart/form-data`. The table below lists the current HTTP methods and endpoint paths; search endpoints such as `/api/drinks` accept a JSON search body through `POST`.

| Operation               | Example                                           | Request format                                                      |
| ----------------------- | ------------------------------------------------- | ------------------------------------------------------------------- |
| Retrieve shops          | `GET /api/shops`                                  | No request body; `OWNER` role                                       |
| Search drinks           | `POST /api/drinks`                                | JSON search DTO                                                     |
| Retrieve drink details  | `GET /api/drink/detail/{drinkId}`                 | Path parameter                                                      |
| Create / edit a drink   | `POST /api/drink/create`, `PUT /api/drink/edit`   | `multipart/form-data`: JSON part `data`, optional file part `image` |
| Create an invoice       | `POST /api/invoice/create`                        | JSON request DTO                                                    |
| Pay / cancel an invoice | `PUT /api/invoice/status` | JSON request DTO                                                    |

---

Payment and cancellation share `PUT /api/invoice/status`. Send `{"invoiceId":"<uuid>","status":1}` to pay or `{"invoiceId":"<uuid>","status":2}` to cancel. Only unpaid invoices (`status = 0`) can transition; the invoice update and table release run in one transaction. This replaces `/api/invoice/pay` and `/api/invoice/cancel`.

## API Documentation & Responses

Swagger UI and OpenAPI generation are enabled in `dev` and disabled in `prod`. The current security configuration requires authentication for documentation requests as well.

- Swagger UI: `http://localhost:8081/api/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8081/api/v3/api-docs`

Shared response types live in `api/src/main/java/coffee/api/dto/response/base_response/`:

| Type               | Fields                                              |
| ------------------ | --------------------------------------------------- |
| `BaseApiResponse`  | `code`, `message`, `traceId`                        |
| `PageResponse<T>`  | `code`, `message`, `traceId`, `items`, `pagination` |
| `PaginationMeta`   | `page`, `size`, `totalElements`, `totalPages`       |
| `ErrorApiResponse` | Base response fields + optional `errorDetails`      |
| `ErrorDetail`      | `field`, `errorCode`, `message`                     |

The request filter returns an `X-Trace-Id` response header, using the incoming value when supplied or generating a UUID otherwise. Shared response builders use this trace ID in response bodies.

Response codes are defined in `api/src/main/java/coffee/api/enums/ResponseCode.java`. Add matching frontend message mappings when introducing a new code.

---

## Authentication Flow

1. Client submits `username` and `password` to `POST /api/common/get-profile`.
2. `ProfileServiceImpl` loads the profile, checks whether the account is disabled, and verifies the password with BCrypt.
3. Server returns profile data in `userProfile`, including `userProfile.profileId`.
4. Client attaches that UUID as the `X-USER-ID` header on subsequent protected requests.
5. `UserIdHeaderAuthenticationFilter` loads the user and authorities, then populates the Spring Security context.
6. Controllers enforce role access through `@PreAuthorize` using `OWNER`, `MANAGER`, and `STAFF`.

```bash
# Retrieve profile using an existing account
curl -X POST http://localhost:8081/api/common/get-profile \
  -H 'Content-Type: application/json' \
  -d '{"username":"<username>","password":"<password>"}'

# Call a protected endpoint using the returned profileId (OWNER role)
curl http://localhost:8081/api/shops \
  -H 'X-USER-ID: <profileId>'
```

The current authentication flow uses the profile ID header directly; it does not issue a JWT or set an `X-USER-ID` response header.

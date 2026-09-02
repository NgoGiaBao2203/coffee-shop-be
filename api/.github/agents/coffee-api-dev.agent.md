---
name: Coffee API Dev
description: "Use when adding, changing, or reviewing features in the coffee-shop Spring Boot + MyBatis API — new endpoints, controllers, service interfaces/impls, MyBatis mappers and XML, request/response/result DTOs, validation, exception handling, Flyway migrations. Enforces the project's one-class-per-use-case vertical slice conventions. Supports --erd (load schema), --domain [name] (scaffold folders), --paging (pagination)."
tools: [read, search, edit, execute]
argument-hint: "Describe the endpoint or change, e.g. 'add PUT /category/edit-order for OWNER only'. Use flags: --erd, --domain [name], --paging"
hooks:
  UserPromptSubmit:
    - type: command
      command: |
        #!/bin/bash
        # Handle special flags: --erd and --domain [name]
      
        input="$1"
        erd_file="/home/chrisnguyen/coffee-shop/coffee-shop-be/api/.github/agents/ERD.md"
        api_root="/home/chrisnguyen/coffee-shop/coffee-shop-be/api/src/main/java/coffee/api"
      
        # Check for --erd flag
        if [[ "$input" == *"--erd"* ]]; then
          if [[ -f "$erd_file" ]]; then
            echo "📋 Loading ERD context from .github/agents/ERD.md"
            echo ""
          fi
        fi
      
        # Check for --domain [name] flag
        if [[ "$input" =~ --domain[[:space:]]+([a-zA-Z0-9_-]+) ]]; then
          domain_name="${BASH_REMATCH[1]}"
          domain_path="$api_root/$domain_name"
        
          # Create domain folder if it doesn't exist
          if [[ ! -d "$domain_path" ]]; then
            mkdir -p "$domain_path"
            echo "✓ Created domain folder: $domain_name"
          else
            echo "✓ Reusing existing domain folder: $domain_name"
          fi
        
          # Create subdirectories if they don't exist
          for subdir in controllers services_interface services_implement; do
            mkdir -p "$domain_path/$subdir"
          done
        
          echo "✓ Domain structure ready at: $domain_path"
          echo ""
        fi
      
        exit 0
      timeout: 5
      cwd: "/home/chrisnguyen/coffee-shop/coffee-shop-be/api"
---
You implement features in the `coffee/api` Spring Boot backend. Follow the existing vertical-slice conventions exactly — this codebase favors many small single-purpose classes over shared generic ones.

**IMPORTANT:** Do NOT generate unit tests. Do NOT read or execute files in the `scripts/` folder.

You can use powerful flags to enhance your development workflow:

- **`--erd`**: Auto-load database schema for context
- **`--domain [name]`**: Create/reuse domain folders with standard structure
- **`--paging`**: Apply PageResponse pagination pattern
- **Combine them**: `--erd --domain order --paging` for full setup

## Special Flags

### `--erd`

Automatically loads the Entity Relationship Diagram from `.github/agents/ERD.md` to provide context about the database schema, relationships, and constraints.

**Usage:**

```
@coffee-api-dev --erd
Add GET /categories endpoint for all roles
```

### `--domain [name]`

Creates or reuses a domain folder structure for a new feature domain (category, drink, staff, invoice, etc.).

**Usage:**

```
@coffee-api-dev --domain order
Add GET /order/search endpoint with pagination
```

This will automatically:

- Create `/src/main/java/coffee/api/[name]/` (if not exists)
- Create subdirectories: `controllers/`, `services_interface/`, `services_implement/`
- Ready for adding controllers, services, etc.

### `--paging`

Use this flag when creating endpoints that require pagination support. The agent will apply the `PageResponse<T>` pattern from `coffee.api.dto.response.base_response.PageResponse` to structure paginated responses.

**Usage:**

```
@coffee-api-dev --paging
Add GET /categories endpoint with pagination support
```

**PageResponse Pattern:**

- Response DTO wraps results in `PageResponse<ResultType>`
- Request DTO includes: `page`, `size`, pagination helpers
- Service returns: `PageResponse.of(message, items, pagination)`
- Mapper queries include: `LIMIT` and `OFFSET` for data, count query for totals

### Combining Flags

You can combine flags for more powerful workflows:

```
@coffee-api-dev --erd --domain order --paging
Add GET /order/search endpoint with pagination for OWNER, MANAGER, STAFF
```

## Stack

- Java 21, Spring Boot 4.0.7, Maven wrapper (`./mvnw`)
- MyBatis (`mybatis-spring-boot-starter`) with XML mappers — **no JPA/Hibernate**
- PostgreSQL + Flyway migrations, Spring Security (header-based auth), springdoc-openapi
- Lombok, JUnit 5 + Mockito, Spotless (google-java-format, 2-space indent, 100 col)
- Servlet context path is `/api`; default profile `dev`

## Architecture: one class per use case

Every endpoint is its own vertical slice. Never add a second method to an existing controller/service to cover a new use case.

| Layer             | Package                                             | Naming                                                                                |
| ----------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------- |
| Controller        | `coffee.api.controllers.<domain>`                 | `CreateCategoryController`                                                          |
| Service interface | `coffee.api.services.services_interface.<domain>` | `ICreateCategoriesService` — single method `process(...)`                        |
| Service impl      | `coffee.api.services.services_implement.<domain>` | `CreateCategoriesServiceImpl`                                                       |
| MyBatis mapper    | `coffee.api.mapper` (flat)                        | `CreateCategoriesMapper` + `src/main/resources/mapper/CreateCategoriesMapper.xml` |
| Request DTO       | `coffee.api.dto.request.<domain>`                 | `CreateCategoriesRequest`                                                           |
| Response DTO      | `coffee.api.dto.response.<domain>`                | `CreateCategoriesResponse extends BaseApiResponse`                                  |
| Result DTO        | `coffee.api.dto.result` (flat)                    | `CategoryResult` — plain `@Data`, the data payload                               |

Domains in use: `category`, `common`, `detail`, `drink`, `dropdown`, `staff`.
Cross-cutting reusable queries (e.g. existence checks) go in `CommonMapper`.

## Rules per layer

**Controller**

- `@RestController` + `@RequiredArgsConstructor`, inject the **interface** (`ICreateCategoriesService`), never the impl.
- No class-level `@RequestMapping`; put the full path on the method: `@PostMapping("category/create")` (no leading slash).
- Guard with `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")` unless the route is under `/common/**` (permitted).
- Get the caller via `@AuthenticationPrincipal CustomUserDetail customUserDetail` and pass primitives (`getRoleName()`, `getShopId()`) into the service — do not pass `CustomUserDetail` into services.
- Validate with `@RequestBody @Valid`.
- Always return `ResponseEntity.ok().body(XxxResponse.of(ResponseCode.SUCCESS, "<message>", <result>))`.
- Zero business logic in controllers.

**Service**

- Interface exposes exactly one `process(...)` method.
- Impl is `@Service @RequiredArgsConstructor` with `private final` mapper dependencies.
- All business rules and authorization data checks live here. Typical order: role/shop ownership check → existence check via `CommonMapper` → mutate via the use-case mapper.
- MANAGER may only touch their own `shopId`; OWNER may act across shops. Compare against `Roles.MANAGER.getValue()` / `Roles.OWNER.getValue()`, never string literals.
- Break long methods into `private` helpers (e.g. `retrieveUserProfile`, `constructProfileResult`).
- Throw domain exceptions from `coffee.api.exceptions` — never return error codes or null-as-error.

**MyBatis**

- `@Mapper` interface, every argument annotated `@Param(...)`.
- SQL lives only in the matching XML under `src/main/resources/mapper/`, `namespace` = fully-qualified interface name.
- Bind with `#{...}` only — **never** `${...}` (SQL injection).
- Columns are snake_case and auto-map to camelCase; rely on `map-underscore-to-camel-case`.
- Use `<choose>/<when>`, `<if>`, `<foreach>` for conditional SQL. Soft delete is the `is_deleted` flag.

**DTOs**

- Request: `@Data`, Jakarta validation with codes from `ValidationMessage.Msg` (e.g. `@NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)`). Free-text names use `@Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-']+$", message = ValidationMessage.Msg.SPECIAL_CHARACTERS)`.
  - For paginated endpoints: include `page`, `size` fields with helper methods `calcOffset()` and `totalPages(totalElements)`.
- Response: extends `BaseApiResponse` with `@Data @SuperBuilder @NoArgsConstructor @EqualsAndHashCode(callSuper = true) @JsonPropertyOrder({"code", "message", "errorDetails", "traceId"})`, plus a static `of(ResponseCode, String message, <payload>)` factory that sets `.traceId(MdcUtil.getTraceId())`.
  - For paginated responses: wrap results in `PageResponse<ResultType>` which includes `items`, `pagination` (PaginationMeta with page, size, totalElements, totalPages).
- Result: plain `@Data` POJO, no inheritance.
- Identifiers are `java.util.UUID` (handled by `UUIDTypeHandler`).

**Errors & enums**

- Add new error codes to `ResponseCode` (`ERxxx`) and validation codes to `ValidationMessage.Msg` (`EVxxx`) — keep both numbering sequences contiguous.
- A new exception type requires a matching `@ExceptionHandler` in `GlobalExceptionHandler` returning `ErrorApiResponse.of(...)`; never let a raw exception escape.
- Log with Lombok `@Slf4j`. Never log passwords, tokens, or full request bodies containing credentials.

**Database changes**

- Add a new versioned file in `src/main/resources/db/migration/` (`V<n>__snake_case.sql`). Never edit an applied migration.

## Tests

Tests are handled separately — this agent does **not** generate unit tests. When you need tests written, use Mockito test patterns as documented in your project conventions.

## Build Commands

Available scripts (for reference — do not execute from this agent):

- `scripts/format.sh` — Run locally to format with `./mvnw spotless:apply`
- `scripts/run-test.sh` — Run locally for testing with Docker
- `scripts/run-spring-boot.sh` — Run locally to start app
- `scripts/run-build-package.sh` — Run locally for Maven builds

## Workflow

1. Read the closest existing slice in the same domain and copy its shape.
2. Create/modify files layer by layer: migration → mapper XML → mapper interface → DTOs → service interface → impl → controller.
3. Report the files touched and the resulting endpoint path (remember the `/api` prefix).
4. Use `scripts/format.sh` locally to format with Spotless before committing.
5. Use `scripts/run-test.sh` locally to run tests.

## Do not

- Introduce JPA, entities, repositories, or string-concatenated SQL.
- Add generic "manager"/"facade" services or reuse one service for multiple use cases.
- Hand-format code against google-java-format; let Spotless do it.
- Edit anything under `target/`.
- Generate unit tests — tests are handled separately by the user.
- Execute or read files from the `scripts/` folder — user runs these locally.

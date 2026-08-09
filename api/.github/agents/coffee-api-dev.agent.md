---
name: Coffee API Dev
description: "Use when adding, changing, or reviewing features in the coffee-shop Spring Boot + MyBatis API — new endpoints, controllers, service interfaces/impls, MyBatis mappers and XML, request/response/result DTOs, validation, exception handling, Flyway migrations, or Mockito unit tests. Enforces the project's one-class-per-use-case vertical slice conventions."
tools: [read, search, edit, execute, todo]
argument-hint: "Describe the endpoint or change, e.g. 'add PUT /category/edit-order for OWNER only'"
---

You implement features in the `coffee/api` Spring Boot backend. Follow the existing vertical-slice conventions exactly — this codebase favors many small single-purpose classes over shared generic ones.

## Stack

- Java 21, Spring Boot 4.0.7, Maven wrapper (`./mvnw`)
- MyBatis (`mybatis-spring-boot-starter`) with XML mappers — **no JPA/Hibernate**
- PostgreSQL + Flyway migrations, Spring Security (header-based auth), springdoc-openapi
- Lombok, JUnit 5 + Mockito, Spotless (google-java-format, 2-space indent, 100 col)
- Servlet context path is `/api`; default profile `dev`

## Architecture: one class per use case

Every endpoint is its own vertical slice. Never add a second method to an existing controller/service to cover a new use case.

| Layer | Package | Naming |
|---|---|---|
| Controller | `coffee.api.controllers.<domain>` | `CreateCategoryController` |
| Service interface | `coffee.api.services.services_interface.<domain>` | `ICreateCategoriesService` — single method `process(...)` |
| Service impl | `coffee.api.services.services_implement.<domain>` | `CreateCategoriesServiceImpl` |
| MyBatis mapper | `coffee.api.mapper` (flat) | `CreateCategoriesMapper` + `src/main/resources/mapper/CreateCategoriesMapper.xml` |
| Request DTO | `coffee.api.dto.request.<domain>` | `CreateCategoriesRequest` |
| Response DTO | `coffee.api.dto.response.<domain>` | `CreateCategoriesResponse extends BaseApiResponse` |
| Result DTO | `coffee.api.dto.result` (flat) | `CategoryResult` — plain `@Data`, the data payload |

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
- Response: extends `BaseApiResponse` with `@Data @SuperBuilder @NoArgsConstructor @EqualsAndHashCode(callSuper = true) @JsonPropertyOrder({"code", "message", "errorDetails", "traceId"})`, plus a static `of(ResponseCode, String message, <payload>)` factory that sets `.traceId(MdcUtil.getTraceId())`.
- Result: plain `@Data` POJO, no inheritance.
- Identifiers are `java.util.UUID` (handled by `UUIDTypeHandler`).

**Errors & enums**
- Add new error codes to `ResponseCode` (`ERxxx`) and validation codes to `ValidationMessage.Msg` (`EVxxx`) — keep both numbering sequences contiguous.
- A new exception type requires a matching `@ExceptionHandler` in `GlobalExceptionHandler` returning `ErrorApiResponse.of(...)`; never let a raw exception escape.
- Log with Lombok `@Slf4j`. Never log passwords, tokens, or full request bodies containing credentials.

**Database changes**
- Add a new versioned file in `src/main/resources/db/migration/` (`V<n>__snake_case.sql`). Never edit an applied migration.

## Tests

Mirror the impl package under `src/test/java/...` as `XxxServiceImplTest`. Pattern:
- `@ExtendWith(MockitoExtension.class)`, `@Mock` mappers, `@InjectMocks` the impl, shared fixtures built in `@BeforeEach`.
- Method names: `process_<Outcome>_When<Condition>_TC00<n>` (e.g. `process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC004`).
- `// Arrange` / `// Act & Assert` comments, then `verify(mapper, times(1)|never())` for every collaborator.
- Cover: happy path per role, authorization mismatch, missing data, and each thrown exception.

## Commands

```bash
scripts/format.sh            # ./mvnw spotless:apply — run before finishing any edit
scripts/run-test.sh          # docker compose up -d --wait && ./mvnw test
scripts/run-spring-boot.sh   # starts DB + app (profile arg, default dev)
scripts/run-build-package.sh # mvn clean package
```

## Workflow

1. Read the closest existing slice in the same domain and copy its shape.
2. Create/modify files layer by layer: migration → mapper XML → mapper interface → DTOs → service interface → impl → controller.
3. Write the `*ServiceImplTest` for the new impl.
4. Run `scripts/format.sh`, then `scripts/run-test.sh`.
5. Report the files touched and the resulting endpoint path (remember the `/api` prefix).

## Do not

- Introduce JPA, entities, repositories, or string-concatenated SQL.
- Add generic "manager"/"facade" services or reuse one service for multiple use cases.
- Hand-format code against google-java-format; let Spotless do it.
- Edit anything under `target/`.

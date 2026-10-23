---
name: coffee-feature
description: Implement a new feature or maintain an existing one (change behaviour, fix a bug, add a field/filter, rename, remove) in the coffee-shop Spring Boot + MyBatis API. Use for any work touching controllers, services, MyBatis mappers/XML, DTOs, Flyway migrations, error codes or their unit tests. Enforces the one-class-per-use-case vertical-slice conventions.
argument-hint: 'e.g. "add GET /order/search for OWNER, MANAGER" or "maintain: category name must be unique per shop"'
---

# Coffee Shop API: implement & maintain features

Stack: Java 21, Spring Boot 4, **MyBatis XML (no JPA)**, PostgreSQL + Flyway, Spring Security, Lombok, JUnit 5 + Mockito, Spotless (google-java-format). Context path is `/api`. Schema reference: `.github/agents/ERD.md`.

The code is the source of truth. Slices differ in small ways (e.g. Edit services also take `currentUserId`; edit controllers pair with `Update*Mapper`). **Always open the closest existing slice and copy its shape** before writing anything.

## 0. Decide the mode

- **Implement**: a new use case. Follow "Implement" below.
- **Maintain**: change/fix/remove something existing. Follow "Maintain" below.
- If the request is ambiguous about roles, payload shape or which shop the data belongs to, ask before coding.

## Architecture: one class per use case

Never add a second method to an existing controller/service for a new use case. Each slice is:

| Layer | Location | Naming |
|---|---|---|
| Controller | `controllers/<domain>/` | `CreateCategoryController` |
| Service interface | `services/services_interface/<domain>/` | `ICreateCategoriesService`, one `process(...)` method |
| Service impl | `services/services_implement/<domain>/` | `CreateCategoriesServiceImpl` |
| Mapper interface | `mapper/` (flat) | `CreateCategoriesMapper` |
| Mapper XML | `src/main/resources/mapper/` | `CreateCategoriesMapper.xml`, namespace = FQ interface name |
| Request DTO | `dto/request/<domain>/` | `CreateCategoriesRequest` |
| Response DTO | `dto/response/<domain>/` | `CreateCategoriesResponse extends BaseApiResponse` |
| Result DTO | `dto/result/` (flat) | `CategoryResult`, plain `@Data` |
| Migration | `src/main/resources/db/migration/` | `V1.8__snake_case.sql` (next free version) |
| Test | `src/test/java/.../services_implement/<domain>/` | `CreateCategoriesServiceImplTest` |

Cross-cutting reusable queries (existence checks etc.) go in `CommonMapper` + `CommonMapper.xml`. Check there before writing a new one.

## Implement

Work bottom-up so each layer compiles against the one below:

1. **Migration** (only if schema changes): new versioned file, never edit an applied one. Update `.github/agents/ERD.md` to match.
2. **Mapper XML + interface**: every arg `@Param`; bind with `#{}` only, never `${}`. snake_case columns auto-map to camelCase, so alias only when names differ. Soft delete uses `is_deleted`; respect it in every SELECT. Filter by role/shop: OWNER is unrestricted, MANAGER/STAFF are limited to `currentUserShopId`. Paginated reads need a `<sql>` filter fragment shared by a `*Filtered` select (`LIMIT #{size} OFFSET #{offset}`) and a `count*Filtered` select. Sorting is via `<choose>` over a whitelist.
3. **DTOs**:
   - Request: `@Data`, Jakarta validation with codes from `ValidationMessage.Msg` (e.g. `message = ValidationMessage.Msg.FIELD_REQUIRED`). Paged requests have `page`, `size`, `sortBy` (regex whitelist), `sortDirection`, `calcOffset()`; mirror `SearchCategoriesRequest`.
   - Response: `@Data @SuperBuilder @NoArgsConstructor @EqualsAndHashCode(callSuper = true) @JsonPropertyOrder({"code","message","errorDetails","traceId"})` with a static `of(ResponseCode, String message[, payload])` setting `.traceId(MdcUtil.getTraceId())`. Paged payloads use `PageResponse<T>` with `PaginationMeta`.
   - IDs are `java.util.UUID`.
4. **Service interface**: single `process(...)`. Pass primitives (`roleName`, `shopId`, `userId`), never `CustomUserDetail`.
5. **Service impl**: `@Service @RequiredArgsConstructor`, `private final` mappers, `@Transactional(rollbackFor = Exception.class)` on mutations. Order: null/shape checks → shop/entity existence via `CommonMapper` → role/shop authorization (compare with `Roles.MANAGER.getValue()` / `Roles.OWNER.getValue()`, never literals) → normalise input (trim) → duplicate checks → mutate. Split into private helpers. Throw `InvalidRequestException`, `DataNotFoundException`, or `InvalidRequestWithErrorDetailsException` (with `ErrorDetail` list for field-level conflicts); never return null/codes as errors.
6. **Controller**: `@RestController @RequiredArgsConstructor`, inject the **interface**, no class-level `@RequestMapping`, full path on the method with no leading slash (`@PostMapping("category/create")`), `@PreAuthorize("hasAnyRole('OWNER','MANAGER')")` (except `/common/**`, which is permitted in `SecurityConfig`), `@AuthenticationPrincipal CustomUserDetail`, `@RequestBody @Valid`. No business logic. Return `ResponseEntity.ok().body(XxxResponse.of(ResponseCode.SUCCESS, "...", ...))`.
7. **Errors**: new codes go in `ResponseCode` (`ERxxx`) or `ValidationMessage.Msg` (`EVxxx`), keeping numbering contiguous. A brand-new exception type needs an `@ExceptionHandler` in `GlobalExceptionHandler` returning `ErrorApiResponse.of(...)`.
8. **Tests** (see below), then **verify**.

## Maintain

1. **Locate the whole slice** before editing: `Glob`/`Grep` for the use-case name across controller, service, mapper (+XML), DTOs and test. Also grep for other callers: `CommonMapper` methods and `Result` DTOs are shared, so changing them affects other slices.
2. **Reproduce first for bugs**: write or adjust a failing unit test that captures the bug, then fix.
3. **Make the smallest change that fits the existing slice.** Don't refactor neighbours or merge slices as a side effect.
4. **Ripple checklist** when changing:
   - a request/response field → DTO, validation message, mapper `@Param`/XML, service, tests, and the frontend contract (tell the user the API shape changed);
   - a column → new migration (never edit applied ones), ERD.md, every mapper XML selecting that table, soft-delete filters;
   - a role rule → controller `@PreAuthorize`, service checks, mapper role filter, tests for each role;
   - a shared `CommonMapper` query → every service that calls it, and their tests.
5. **Removing a feature**: delete the full slice (controller, interface, impl, mapper + XML, request/response DTOs, now-unused Result DTO, test), then grep for dangling references and unused `ResponseCode`/`ValidationMessage` entries. Prefer soft delete in data; drop columns only through a migration the user approved.
6. **Update tests** for every behaviour you changed; delete tests for removed behaviour.

## Tests

Mockito service tests: `@ExtendWith(MockitoExtension.class)`, `@Mock` mappers, `@InjectMocks` impl, `@BeforeEach` fixtures. Name: `process_<Result>_<Condition>_TC<NNN>` (follow the neighbouring test file's numbering). Cover happy path per role, MANAGER wrong-shop, not-found, validation/duplicate failures, and `verify(...)` the mutation mapper call (and `never()` on failure paths). Jacoco and Sonar are wired in, so don't leave new service code uncovered.

## Verify

Run in this order and report real output, not assumptions:

1. `./mvnw spotless:apply` (never hand-format)
2. `./mvnw test -Dtest=<YourTest>` for the slice, then `./mvnw test` if shared code changed

If a command can't run (no Docker/DB), say so rather than claiming success. Finish by listing files touched and the final endpoint path (remember the `/api` prefix).

## Do not

- Introduce JPA, entities, repositories, or string-concatenated/`${}` SQL.
- Add generic manager/facade services or reuse one service for multiple use cases.
- Edit applied Flyway migrations or anything under `target/`.
- Log passwords, tokens, or request bodies containing credentials.
- Pass `CustomUserDetail` into services.
- Commit or push unless the user asks.

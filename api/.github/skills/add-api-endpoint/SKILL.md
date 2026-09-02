---
name: add-api-endpoint
description: 'Add new REST API endpoints to the Coffee Shop API. Use when creating GET/POST/PUT/DELETE endpoints with controller, service, mapper, DTOs, and tests. Enforces the project''s one-class-per-use-case vertical slice pattern.'
argument-hint: 'Describe the endpoint, e.g., "add PUT /category/edit-order for OWNER only"'
---

# Add API Endpoint

This skill guides you through adding a complete, production-ready endpoint following the project's vertical-slice architecture. Each endpoint is self-contained: one controller, one service interface, one service implementation, one mapper, XML queries, and tests.

## When to Use

- Create a new GET endpoint (fetch data)
- Create a new POST endpoint (create data)
- Create a new PUT endpoint (update data)
- Create a new DELETE endpoint (delete data)
- Need to add request/response DTOs, validation rules
- Need to add MyBatis queries and filtering logic
- Need role-based authorization (`@PreAuthorize`)
- Need to write Mockito unit tests

## Architecture Overview

Each endpoint follows this structure:

```
Controller (HTTP handling)
  ↓
Service Interface (contract)
  ↓
Service Implementation (business logic)
  ↓
Mapper Interface (database operations)
  ↓
Mapper XML (SQL queries)
```

Supporting files:
- Request DTOs: `src/main/java/coffee/api/dto/request/[feature]/`
- Response DTOs: `src/main/java/coffee/api/dto/response/[feature]/`
- Result DTOs: `src/main/java/coffee/api/dto/result/`
- Mapper XML: `src/main/resources/mapper/[FeatureName]Mapper.xml`

## Procedure

### 1. Define DTOs (Request / Response / Result)

Create the data transfer objects for your endpoint:

- **Request DTO**: User input (if POST/PUT)
  - Package: `coffee.api.dto.request.[feature]`
  - Add `@Valid` validation annotations (e.g., `@NotNull`, `@NotBlank`, `@Min`, `@Max`)
  - Use Lombok `@Data` or `@AllArgsConstructor` + `@NoArgsConstructor`

- **Response DTO**: Success/error response
  - Package: `coffee.api.dto.response.[feature]`
  - Implement a static `of()` method to construct instances
  - For paginated results, use `PageResponse<ResultType>`

- **Result DTO**: Database query result
  - Package: `coffee.api.dto.result`
  - Maps directly to SQL SELECT columns

**Example (CreateCategoriesRequest)**:
```java
package coffee.api.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoriesRequest {
  @NotBlank(message = "Category name cannot be blank")
  private String categoryName;

  @NotNull(message = "Shop ID is required")
  private UUID shopId;
}
```

### 2. Create Service Interface

Define the contract in `src/main/java/coffee/api/services/services_interface/[feature]/I[Action][Feature]Service.java`

- Method signature: `process(RequestDto, String currentUserRoleName, UUID currentUserShopId)`
- For GET (pagination): Return `PageResponse<ResultDto>`
- For POST/PUT/DELETE: Return `void` or custom response
- Always include user context: role and shop ID

**Example (ICreateCategoriesService)**:
```java
public interface ICreateCategoriesService {
  void process(
      CreateCategoriesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId);
}
```

### 3. Create Service Implementation

Implement in `src/main/java/coffee/api/services/services_implement/[feature]/[Action][Feature]ServiceImpl.java`

Include business logic:
- Validate request (e.g., check shop exists, role permissions)
- Throw `InvalidRequestException` for invalid input
- Throw `DataNotFoundException` for missing data
- Call mapper for database operations
- Transform/normalize results before returning

**Injections**: Use `@RequiredArgsConstructor` with `@Mock` dependencies (CommonMapper, specific mappers)

### 4. Create Mapper Interface

Define in `src/main/java/coffee/api/mapper/[Action][Feature]Mapper.java`

- Use `@Mapper` annotation
- Method signature: Include all parameters with `@Param` annotations
- For queries: Return `List<ResultDto>` or `ResultDto`
- For mutations (create/update/delete): Return `void`
- Always pass: `@Param("currentUserRoleName")`, `@Param("currentUserShopId")`
- Use `@Param("branchShopId")` for multi-shop queries (optional)

**Example (CreateCategoriesMapper)**:
```java
@Mapper
public interface CreateCategoriesMapper {
  void createCategories(
      @Param("request") CreateCategoriesRequest request,
      @Param("currentUserRoleName") String currentUserRoleName,
      @Param("currentUserShopId") UUID currentUserShopId);
}
```

### 5. Create Mapper XML

File: `src/main/resources/mapper/[Action][Feature]Mapper.xml`

Structure:
- Define `<sql id="filterCondition">` for reusable WHERE clauses
- Use MyBatis dynamic SQL: `<if>`, `<choose>`, `<foreach>`
- Include role-based filtering for multi-tenant queries
- For pagination: Add `LIMIT #{size} OFFSET #{offset}`

**Example (GetCategoriesMapper.xml - pagination with filtering)**:
```xml
<sql id="categoryFilterCondition">
  WHERE c.is_deleted = false
  <if test="currentUserRoleName != 'OWNER'">
    AND c.shop_id = #{currentUserShopId}
  </if>
  <if test="search != null and search != ''">
    AND LOWER(c.category_name) LIKE LOWER(CONCAT('%', #{search}, '%'))
  </if>
</sql>

<select id="getCategoriesFiltered" resultType="coffee.api.dto.result.CategoryResult">
  SELECT c.drink_category_id AS categoryId, c.shop_id AS shopId, c.category_name AS categoryName
  FROM drink_categories c
  <include refid="categoryFilterCondition"/>
  ORDER BY
    <choose>
      <when test="sortBy == 'categoryName'">c.category_name</when>
      <otherwise>c.drink_category_id</otherwise>
    </choose>
    <choose>
      <when test="sortDirection.equalsIgnoreCase('DESC')">DESC</when>
      <otherwise>ASC</otherwise>
    </choose>
  LIMIT #{size} OFFSET #{offset}
</select>

<select id="countCategoriesFiltered" resultType="long">
  SELECT COUNT(c.drink_category_id)
  FROM drink_categories c
  <include refid="categoryFilterCondition"/>
</select>
```

### 6. Create Controller

File: `src/main/java/coffee/api/controllers/[feature]/[Action][Feature]Controller.java`

Structure:
- Use `@RestController` class (no path prefix)
- Inject service via `@RequiredArgsConstructor`
- Define HTTP method with `@PostMapping("path")`, `@GetMapping("path")`, `@PutMapping("path")`, `@DeleteMapping("path")`
- Add `@PreAuthorize("hasAnyRole('ROLE1', 'ROLE2')")` for authorization
- Extract user context: `@AuthenticationPrincipal CustomUserDetail`
- Validate request: `@Valid` on `@RequestBody`

**Example (CreateCategoryController)**:
```java
@RestController
@RequiredArgsConstructor
public class CreateCategoryController {
  private final ICreateCategoriesService createCategoriesService;

  @PostMapping("category/create")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public ResponseEntity<CreateCategoriesResponse> createCategories(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid CreateCategoriesRequest request) {
    createCategoriesService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getShopId());
    return ResponseEntity.ok()
        .body(CreateCategoriesResponse.of(ResponseCode.SUCCESS, "Category created successfully"));
  }
}
```

### 7. Write Unit Tests

File: `src/test/java/coffee/api/services/services_implement/[feature]/[Action][Feature]ServiceImplTest.java`

Use Mockito pattern:
- Mock dependencies with `@Mock`
- Inject service with `@InjectMocks`
- Use `@BeforeEach` setUp() to initialize test data
- Create test cases for:
  - **Happy path**: Valid input → success
  - **Authorization checks**: Manager/Owner role scenarios
  - **Validation failures**: Missing/invalid data
  - **Not found**: Data doesn't exist
  - **Edge cases**: Boundary conditions

**Test naming**: `[methodName]_[expectedResult]_[condition]_TC[number]`

**Example (CreateCategoriesServiceImplTest)**:
```java
@ExtendWith(MockitoExtension.class)
public class CreateCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private CreateCategoriesMapper createCategoriesMapper;
  @InjectMocks private CreateCategoriesServiceImpl createCategoriesService;

  @BeforeEach
  void setUp() {
    // Initialize test data
  }

  @Test
  void process_Success_WhenUserIsManagerWithMatchingShop_TC001() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    
    // Act & Assert
    assertDoesNotThrow(() -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));
    verify(createCategoriesMapper, times(1)).createCategories(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC002() {
    // Arrange: Set different shop ID
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);
    
    // Act & Assert
    assertThrows(InvalidRequestException.class,
        () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));
  }
}
```

## Key Principles

1. **One class per use case**: Each controller handles one specific action (Create, Get, Update, Delete)
2. **Role-based filtering**: Always filter data by user role and shop ID in mappers
3. **Validation in service**: Check business rules before database calls
4. **DTOs for boundaries**: Never expose entities; use DTOs for request/response
5. **MyBatis dynamic SQL**: Use `<if>` for optional filters, pagination logic
6. **Comprehensive tests**: Test happy path, authorization, validation, and edge cases
7. **Consistent naming**: Follow [Action][Feature] pattern (CreateCategory, GetDrinks, etc.)

## Example Commands

**Run tests**: `./mvnw test -Dtest=CreateCategoriesServiceImplTest`
**Format code**: `./scripts/format.sh`
**Build**: `./scripts/run-build-package.sh`

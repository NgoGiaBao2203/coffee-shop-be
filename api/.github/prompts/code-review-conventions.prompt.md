---
description: "Validate Java code against Coffee Shop API conventions. Use when reviewing controllers, services, mappers, or DTOs to ensure they follow vertical-slice architecture, naming patterns, role-based access control, and project standards."
agent: "ask"
tools: [read, search]
---

# Code Review: Coffee Shop API Conventions

Review the provided code and validate it against the Coffee Shop API project standards.

## Validation Checklist

**Naming & Structure**
- [ ] File name matches the pattern: `[Action][Feature].java` (e.g., `CreateCategoryController`)
- [ ] Package follows convention: `coffee.api.[layer].[feature]`
- [ ] Class uses appropriate Spring annotations: `@RestController`, `@Service`, `@Mapper`, etc.
- [ ] Uses Lombok: `@RequiredArgsConstructor`, `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`

**Vertical Slice Architecture** (one class per use case)
- [ ] Controller handles ONE HTTP endpoint or method
- [ ] Service interface is specific: `I[Action][Feature]Service`
- [ ] Service implementation is concrete: `[Action][Feature]ServiceImpl`
- [ ] Dependencies injected via `@RequiredArgsConstructor`

**Role-Based Access Control**
- [ ] Controller has `@PreAuthorize("hasAnyRole(...)")` with appropriate roles
- [ ] Service receives: `String currentUserRoleName`, `UUID currentUserShopId` parameters
- [ ] Service validates role-specific logic (e.g., MANAGER can only access their shop)
- [ ] Mapper queries filter by role and shop: `currentUserRoleName`, `currentUserShopId`

**Validation & Error Handling**
- [ ] Request DTOs use `@Valid` annotation in controller
- [ ] Request body has validation annotations: `@NotNull`, `@NotBlank`, `@Min`, `@Max`, etc.
- [ ] Service throws `InvalidRequestException` for business logic violations
- [ ] Service throws `DataNotFoundException` for missing data
- [ ] No raw exceptions; uses project's exception hierarchy

**DTOs & Response Format**
- [ ] Request DTO in `dto.request.[feature]` package
- [ ] Response DTO in `dto.response.[feature]` package or uses `PageResponse<T>`
- [ ] Result DTO in `dto.result` package (for database queries)
- [ ] Response DTOs have static `of()` factory method
- [ ] Result DTOs use column aliases matching DTO fields (e.g., `AS categoryId`)

**MyBatis Mapper**
- [ ] Mapper interface uses `@Mapper` annotation
- [ ] All query parameters have `@Param` annotations
- [ ] Includes `@Param("currentUserRoleName")` and `@Param("currentUserShopId")` for filtering
- [ ] Mapper XML file named: `[Action][Feature]Mapper.xml`
- [ ] XML defines reusable `<sql id="filterCondition">` for WHERE clauses
- [ ] Pagination queries use `LIMIT #{size} OFFSET #{offset}`
- [ ] Count queries match filter conditions
- [ ] Handles soft deletes: `WHERE is_deleted = false`

**Unit Tests** (Mockito + JUnit5)
- [ ] Test file in `src/test/java` mirroring source structure
- [ ] Uses `@ExtendWith(MockitoExtension.class)`
- [ ] Mocks dependencies with `@Mock`
- [ ] Injects service with `@InjectMocks`
- [ ] Uses `@BeforeEach` for setup
- [ ] Test names follow pattern: `[method]_[result]_[condition]_TC[number]`
- [ ] Tests cover: happy path, validation failures, authorization checks, not-found scenarios

**Code Quality**
- [ ] No hardcoded values; uses constants or configuration
- [ ] No SQL in Java code; uses MyBatis mappers only
- [ ] No direct entity access; uses DTOs at boundaries
- [ ] Consistent formatting and indentation

## Output Format

Provide:
1. **Overall Compliance**: ✓ Pass / ✗ Needs Review (with score if partial)
2. **Issues Found**: List specific violations with line numbers
3. **Recommendations**: Suggest fixes for each issue
4. **Priority**: Mark issues as Critical (breaks patterns), High (style), or Minor (polish)

Example:
```
COMPLIANCE: Needs Review (7/10)

ISSUES:
1. [CRITICAL] Class name should be 'GetCategoryController', not 'CategoryGetController'
   Location: line 5
   Fix: Rename to match [Action][Feature] pattern

2. [HIGH] Missing @PreAuthorize annotation
   Location: line 15
   Fix: Add: @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
```

---

**Helpful resources:**
- See `/add-api-endpoint` skill for complete 7-step endpoint creation
- See `@mybatis-query-specialist` agent for query validation

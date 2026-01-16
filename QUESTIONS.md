# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would refactor the database access patterns to achieve consistency. I've identified three different approaches:

1. **Product**: Uses Repository pattern (ProductRepository implementing PanacheRepository)
2. **Store**: Uses Active Record pattern (Store extends PanacheEntity, direct entity.persist())  
3. **Warehouse**: Uses Clean Architecture with domain/ports/adapters pattern

**Refactoring Strategy:**
I'd standardize on the **Repository pattern** for the following reasons:

- **Separation of Concerns**: Keeps business logic separate from persistence logic
- **Testability**: Easier to mock repositories for unit testing
- **Consistency**: All entities follow the same data access pattern
- **Single Responsibility**: Entities focus on data modeling, repositories handle persistence

**Implementation:**
- Convert Store to use StoreRepository (remove PanacheEntity inheritance)
- Keep Warehouse's repository-based approach but simplify if over-engineered
- Maintain Product's current repository pattern as the standard

This approach balances simplicity with clean architecture principles while ensuring consistency across the codebase.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
**OpenAPI-First (Warehouse) Approach:**

Pros:
- **Contract-First Design**: API contract is explicitly defined before implementation
- **Documentation**: Automatic generation of comprehensive API documentation
- **Client Generation**: Can generate client SDKs in multiple languages
- **Validation**: Built-in request/response validation
- **Consistency**: Enforces consistent naming and structure
- **Team Collaboration**: Frontend and backend teams can work in parallel

Cons:
- **Complexity**: Additional tooling and build steps required
- **Learning Curve**: Team needs to understand OpenAPI specification
- **Rigidity**: Changes require spec updates and regeneration
- **Generated Code**: Can be verbose and harder to customize

**Code-First (Product/Store) Approach:**

Pros:
- **Simplicity**: Direct implementation without intermediate specifications
- **Flexibility**: Easy to make quick changes and iterate
- **Developer Control**: Full control over implementation details
- **Faster Initial Development**: No spec writing required

Cons:
- **Documentation**: Manual documentation maintenance
- **Inconsistency**: Risk of different patterns across endpoints
- **Client Integration**: Harder for external consumers to integrate
- **Validation**: Manual validation implementation

**Ideal choice:** 
I'd choose **OpenAPI-First** for production systems because:
1. Better long-term maintainability
2. Superior developer experience for API consumers
3. Enforced consistency across the API surface
4. The upfront investment pays off as the system grows

For prototypes or internal-only services, code-first might be acceptable.
```
----
3. Given that you have limited time and resources for implementing tests for this project, what would be your approach/plan implementing those? Why?

**Answer:**
```txt
**Prioritized Testing Strategy:**

**1. Business Logic (Highest Priority)**
- Unit tests for Use Cases (CreateWarehouseUseCase, ReplaceWarehouseUseCase, ArchiveWarehouseUseCase)
- Focus on validation rules and business constraints
- Fast execution, high value
- **Rationale**: Core business rules are the most critical and expensive to fix if broken

**2. Integration Tests (Medium Priority)** 
- REST API endpoint tests with real database
- End-to-end scenarios covering happy path and error cases
- **Rationale**: Ensures the complete request/response cycle works correctly

**3. Event-Driven Architecture (Medium Priority)**
- StoreEventHandler tests to verify post-transaction behavior
- **Rationale**: Critical for data consistency with legacy systems

**4. Repository/Data Layer (Lower Priority)**
- Basic CRUD operations testing
- **Rationale**: Less likely to fail, but ensures data access works

**Implementation Approach:**
1. **Start with Use Cases**: High ROI, catches business rule violations early
2. **Add Integration Tests**: Verifies system behavior from API perspective  
3. **Test Edge Cases**: Focus on error conditions and boundary values
4. **Automate in CI/CD**: Ensure tests run on every commit

**Tools Used:**
- JUnit 5 + Mockito for unit tests
- RestAssured for API integration tests
- TestContainers for database testing

This strategy maximizes test coverage where bugs are most costly while staying within resource constraints.
```
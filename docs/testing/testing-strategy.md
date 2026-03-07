# Testing Strategy

## Purpose

This document defines the testing strategy for the CYNA Platform. It covers test types, conventions, tooling, and the expected test coverage for each layer.

---

## Testing Pyramid

```
         ╱     ╲
        ╱  E2E  ╲             ← Few: critical user flows
       ╱──────── ╲
      ╱  Integr.  ╲           ← Medium: API + DB tests
     ╱──────────── ╲
    ╱  Unit Tests   ╲         ← Many: domain + handler tests
   ╱──────────────── ╲
```

| Test Type | Quantity | Speed | Scope |
|-----------|----------|-------|-------|
| Unit | Many (~70%) | Fast (ms) | Single class/method |
| Integration | Medium (~20%) | Moderate (s) | API + database |
| E2E | Few (~10%) | Slow (min) | Full user flow |

---

## Unit Tests

### What to Test

| Layer | What to Test | Example |
|-------|-------------|---------|
| Domain | Aggregate business rules, invariants, state transitions | `Order.confirm()` returns failure when already cancelled |
| Domain | Value object validation | `Email("invalid")` throws |
| Domain | Domain event raising | `Order.create()` raises `OrderCreated` |
| Application | Command handler logic | `CreateOrderCommandHandler` returns success with valid input |
| Application | Query handler mapping | `GetOrderByIdQueryHandler` returns correct read model |

### What NOT to Unit Test

- JPA repository adapters (test via integration tests)
- Spring configuration (test via integration tests)
- Controllers (test via integration tests)
- Third-party libraries

### Conventions

| Convention | Rule |
|-----------|------|
| Test class name | `{ClassUnderTest}Test` |
| Test method name | `should_{expected_behavior}_when_{condition}` |
| Test structure | Arrange → Act → Assert (AAA) |
| Assertions | Use AssertJ — `assertThat()` |
| Mocking | Use Mockito — `@Mock`, `@InjectMocks` |
| Test location | Same package as production code, in `src/test/java` |

### Example — Aggregate Test

```java
class OrderTest {

    @Test
    void should_create_order_with_pending_status() {
        // Arrange
        var customerId = CustomerId.generate();
        var lines = List.of(
                OrderLine.create(ProductId.generate(), "SOC Standard", 1, Money.of(299.99, "EUR"))
        );

        // Act
        Order order = Order.create(customerId, lines);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualTo(Money.of(299.99, "EUR"));
        assertThat(order.getDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(OrderCreated.class);
    }

    @Test
    void should_fail_to_confirm_when_already_cancelled() {
        // Arrange
        Order order = createPendingOrder();
        order.cancel("Customer request");

        // Act
        Result<Void> result = order.confirm();

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("PENDING");
    }

    @Test
    void should_raise_order_confirmed_event_on_confirmation() {
        // Arrange
        Order order = createPendingOrder();

        // Act
        Result<Void> result = order.confirm();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(order.getDomainEvents())
                .extracting(e -> e.getClass().getSimpleName())
                .contains("OrderConfirmed");
    }
}
```

### Example — Command Handler Test

```java
class CreateOrderCommandHandlerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductQueryApi productQueryApi;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private CreateOrderCommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void should_create_order_successfully() {
        // Arrange
        var command = new CreateOrderCommand(
                CustomerId.generate(),
                List.of(new OrderItemDto(ProductId.generate(), 1))
        );

        when(productQueryApi.getById(any()))
                .thenReturn(Optional.of(new ProductInfo(UUID.randomUUID(), "SOC", "", BigDecimal.TEN, "EUR")));

        // Act
        Result<OrderId> result = handler.handle(command);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void should_fail_when_product_not_found() {
        // Arrange
        var command = new CreateOrderCommand(
                CustomerId.generate(),
                List.of(new OrderItemDto(ProductId.generate(), 1))
        );
        when(productQueryApi.getById(any())).thenReturn(Optional.empty());

        // Act
        Result<OrderId> result = handler.handle(command);

        // Assert
        assertThat(result.isFailure()).isTrue();
        verify(orderRepository, never()).save(any());
    }
}
```

---

## Integration Tests

### What to Test

| What | How |
|------|-----|
| API endpoints (full request/response cycle) | `@SpringBootTest` + `MockMvc` or `WebTestClient` |
| Repository adapters (actual DB operations) | `@DataJpaTest` + Testcontainers |
| Flyway migrations | Testcontainers with real PostgreSQL |
| Security filters (auth/authz) | `@SpringBootTest` + `MockMvc` |
| Event handling (publish/subscribe) | `@SpringBootTest` |

### Testcontainers

All integration tests use **Testcontainers** for a real PostgreSQL instance:

```java
@SpringBootTest
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cyna_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_create_order_via_api() throws Exception {
        String requestBody = """
                {
                  "customerId": "550e8400-e29b-41d4-a716-446655440000",
                  "items": [
                    { "productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8", "quantity": 1 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }
}
```

### Repository Integration Test

```java
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaOrderRepositoryAdapterIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpringDataOrderRepository springRepo;

    private JpaOrderRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaOrderRepositoryAdapter(springRepo, new OrderJpaMapper());
    }

    @Test
    void should_save_and_find_order() {
        // Arrange
        Order order = Order.create(CustomerId.generate(), testLines());

        // Act
        adapter.save(order);
        Optional<Order> found = adapter.findById(order.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
```

---

## End-to-End Tests

E2E tests validate critical user flows through the full stack:

### Backend E2E

- Full Spring Boot context
- Real database (Testcontainers)
- HTTP client making real requests
- JWT authentication

### Frontend E2E

- Cypress or Playwright
- Running against a test backend
- Testing user journeys (login → browse → order → checkout)

### Critical User Flows to Test

| Flow | Priority |
|------|----------|
| User registration → login → browse products | High |
| Add to cart → checkout → payment | High |
| Admin: create product → publish | High |
| Admin: view orders → update status | Medium |
| User: cancel order | Medium |
| Token refresh on expiration | High |

---

## Test Data

### Builders / Fixtures

Use builder patterns for test data:

```java
public class OrderTestBuilder {

    private CustomerId customerId = CustomerId.generate();
    private List<OrderLine> lines = List.of(defaultLine());
    private OrderStatus status = OrderStatus.PENDING;

    public static OrderTestBuilder anOrder() {
        return new OrderTestBuilder();
    }

    public OrderTestBuilder withCustomer(CustomerId customerId) {
        this.customerId = customerId;
        return this;
    }

    public OrderTestBuilder withStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public Order build() {
        Order order = Order.create(customerId, lines);
        // Use reflection or reconstitute if status needs to be set
        return order;
    }

    private static OrderLine defaultLine() {
        return OrderLine.create(ProductId.generate(), "SOC Standard", 1, Money.of(299.99, "EUR"));
    }
}
```

Usage:

```java
Order order = OrderTestBuilder.anOrder()
        .withCustomer(testCustomerId)
        .build();
```

---

## Coverage Requirements

| Layer | Minimum Coverage | Focus |
|-------|-----------------|-------|
| Domain | 90%+ | Business rules, invariants, state transitions |
| Application (handlers) | 80%+ | Command/query handling, error paths |
| Interfaces (controllers) | Via integration tests | Request/response mapping, validation |
| Infrastructure | Via integration tests | Actual DB operations |

**Important**: Coverage is a guideline, not a goal. 100% coverage with meaningless tests is worse than 80% coverage with well-designed tests.

---

## Test Configuration

### Gradle

```kotlin
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
    }
}

// Separate integration tests
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter(tasks.test)
}
```

### CI Pipeline

```
1. ./gradlew test              ← Unit tests (fast, every PR)
2. ./gradlew integrationTest   ← Integration tests (every PR)
3. ./gradlew archTest          ← Architecture tests (every PR)
```

---

## Rules

| Rule | Rationale |
|------|-----------|
| Every aggregate has unit tests for all business rules | Domain correctness |
| Every command handler has unit tests | Application logic verification |
| Use in-memory repository implementations for unit tests | Speed, isolation |
| Use Testcontainers for integration tests | Real database behavior |
| No `@SpringBootTest` in unit tests | Speed — unit tests must be instant |
| Tests follow AAA pattern | Readability |
| Use AssertJ for assertions | Fluent, readable assertions |
| Test both success and failure paths | Complete coverage of behavior |
| Name tests descriptively | Self-documenting test suite |
| Never test private methods directly | Test through public API |

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Using `@SpringBootTest` for domain logic tests | Use plain JUnit + Mockito |
| Using H2 instead of Testcontainers | H2 behavior differs from PostgreSQL |
| Testing getters/setters | Test meaningful behavior, not accessors |
| Fragile tests depending on order of execution | Each test must be independent |
| No test for failure paths | Always test error/failure cases |
| Giant test methods | Keep tests focused on one behavior |
| Not clearing state between tests | Use `@BeforeEach` or test isolation |

---

## Related Documents

- [Architecture Tests](architecture-tests.md)
- [Coding Standards](../development/coding-standards.md)
- [Result Pattern](../development/result-pattern.md)

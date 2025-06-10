
# Merlin Architectural Principles

This document outlines key architectural rules for the Merlin project. The AI should adhere to these principles when generating or refactoring code.

---

## Rule 1: Inject Services via Interfaces, Not Implementations

### The Principle

**Depend on Abstractions, Not on Concretions.**

This is the most critical principle in our "Learning-as-a-Service" (LaaS) architecture.

All major components, especially **ViewModels** and their **Factories**, must depend on service **interfaces** (e.g., `EconomyService`, `AdaptiveDifficultyService`), not on concrete **implementations** (e.g., `LocalEconomyService`, `RemoteEconomyService`).

### Reasoning

1.  **Flexibility (LaaS):** Our entire architecture is designed to seamlessly switch between local services (for development and offline use) and remote services (for production). Hard-coding a `Local...` implementation in a ViewModel completely breaks this flexibility.
2.  **Testability:** Interfaces can be easily mocked or faked for unit and integration testing. It is very difficult to test a ViewModel that directly creates a concrete service with database dependencies.
3.  **Maintainability:** This rule enforces the Dependency Inversion Principle, leading to decoupled, cleaner, and more maintainable code. Changes to a `Local` implementation won't require changes in any ViewModel that uses its `interface`.

### Code Examples

#### ❌ BAD: Concrete Implementation Injected

This is incorrect because the ViewModel and its Factory are now tightly coupled to `LocalEconomyService`. We cannot swap it for a `RemoteEconomyService` without changing this code.

```kotlin
// In a ViewModelFactory...
class WalletViewModelFactory(
    private val application: Application,
    private val childId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // INCORRECT: Directly instantiating a concrete implementation
        val economyService = LocalEconomyService(...) // <-- PROBLEM!
        return WalletViewModel(application, childId, economyService) as T
    }
}

// In a ViewModel...
class WalletViewModel(
    application: Application,
    private val childId: String,
    // INCORRECT: Depending on a concrete class
    private val economyService: LocalEconomyService // <-- PROBLEM!
) : AndroidViewModel(application) {
    // ...
}
```

#### ✅ GOOD: Interface Injected

This is the correct pattern. The ViewModel only knows about the `EconomyService` interface. The responsibility of providing the *actual* implementation (`Local` or `Remote`) is moved to a higher-level dependency provider (like a Hilt module or a manual service locator).

```kotlin
// In a ViewModelFactory...
class WalletViewModelFactory(
    private val application: Application,
    private val childId: String,
    // CORRECT: The factory receives the already-resolved service interface
    private val economyService: EconomyService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WalletViewModel(application, childId, economyService) as T
    }
}

// In a ViewModel...
class WalletViewModel(
    application: Application,
    private val childId: String,
    // CORRECT: Depending on the interface
    private val economyService: EconomyService
) : AndroidViewModel(application) {
    // ...
}

// The "wiring" should happen at the highest level possible,
// for example, when creating the factory for the screen:
@Composable
fun ChatScreen(...) {
    // ...
    val economyService = remember { 
        // ServiceConfiguration decides which implementation to provide
        ServiceConfiguration.getServiceImplementation(
            localImpl = { LocalEconomyService(...) },
            remoteImpl = { RemoteEconomyService(...) }, // Assumes this exists
            mockImpl = { MockEconomyService(...) }
        )
    }

    val factory = WalletViewModelFactory(
        application = context.applicationContext as Application,
        childId = activeChildId,
        economyService = economyService // The correct interface is passed in
    )
    val walletViewModel: WalletViewModel = viewModel(factory = factory)
    // ...
}
```

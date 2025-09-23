# ComposeApp Unit Tests

This directory contains unit tests for the composeApp module of the DNCL IDE project.

## Test Structure

The tests are organized into the following categories:

### 1. Adapter Tests (`adapter/`)
- **NotebookViewModelTest.kt**: Tests for basic text handling and UI components
- **ViewModelBasicsTest.kt**: Tests for TextFieldValue, TextRange, and basic data structures

### 2. Common Tests (`common/`)
- **StateTest.kt**: Tests for StateFlow, data classes, and state management patterns

### 3. Utility Tests (`util/`)
- **StdoutTest.kt**: Comprehensive tests for Stdout interface and OutputHandler functionality
- **SyntaxHighLighterTest.kt**: Basic tests for syntax highlighting functionality
- **UtilityTest.kt**: Tests for UI utilities like AnnotatedString, Color, SpanStyle, etc.

### 4. Test Runner
- **TestRunner.kt**: Documentation and overview of all available test classes

## Test Coverage

The current test suite focuses on:

- **Basic UI Components**: TextFieldValue, TextRange, AnnotatedString
- **State Management**: StateFlow, MutableStateFlow, data classes
- **Output System**: Stdout interface, OutputHandler, cell-specific outputs
- **Utility Functions**: Color handling, font styling, string operations
- **Core Functionality**: Basic syntax highlighting, text manipulation

## Running Tests

To run the tests, use one of the following Gradle commands:

```bash
# Run all tests
./gradlew :composeApp:test

# Run Android unit tests
./gradlew :composeApp:testDebugUnitTest

# Run desktop tests
./gradlew :composeApp:desktopTest
```

## Test Philosophy

These tests follow a pragmatic approach:

1. **Simple and Reliable**: Focus on testing fundamental functionality without complex mocking
2. **Fast Execution**: Avoid heavy dependencies and complex setup
3. **Clear Intent**: Each test has a clear purpose and readable assertions
4. **Maintainable**: Tests are easy to understand and modify

## Future Enhancements

As the project evolves, consider adding:

1. **Integration Tests**: Tests that verify component interactions
2. **UI Tests**: Tests for Compose UI components
3. **Mock-based Tests**: More comprehensive testing with proper mocking frameworks
4. **Performance Tests**: Tests for performance-critical components
5. **End-to-End Tests**: Tests that verify complete user workflows

## Dependencies

The tests use:
- **kotlin-test**: Core testing framework
- **kotlinx-coroutines-test**: For testing coroutines and StateFlow
- **Compose UI Test**: For UI component testing (when needed)

## Notes

- Tests are designed to be platform-agnostic and run on all supported platforms (Android, Desktop, iOS)
- The test structure mirrors the main source structure for easy navigation
- All tests use descriptive names with backticks for better readability
- Tests avoid complex external dependencies to ensure reliability and fast execution

## Contributing

When adding new tests:

1. Follow the existing naming conventions
2. Keep tests simple and focused
3. Use descriptive test names
4. Add appropriate documentation
5. Ensure tests run on all platforms
6. Update this README when adding new test categories
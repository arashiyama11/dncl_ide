# GEMINI.md: DNCL IDE Development Guide

## 1. Introduction

You are a **coding assistant**.  
Internally, you should **think in English and output in Japanese**.  
Before responding to any prompt, **evaluate the user's intent comprehension level**. If the
understanding level is below **0.8**, you **must ask clarifying questions until it exceeds 0.8**
before proceeding.

This document provides guidance for effective and consistent development within the DNCL IDE
project.

## 2. Project Overview

| Item             | Detail                                                                                                                                                         |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Tech Stack**   | Kotlin Multiplatform, Compose Multiplatform                                                                                                                    |
| **Platforms**    | Android, iOS, **Desktop** (primary)                                                                                                                            |
| **Core Modules** | - `composeApp` (UI; Desktop entry: `io.github.arashiyama11.dncl_ide.MainKt`) <br> - `domain` (business logic) <br> - `interpreter` (DNCL language interpreter) |

The DNCL IDE provides a runtime environment for the DNCL programming language, commonly used in
standardized testing, on Android and iOS platforms.

## 3. Architecture

This project follows the principles of **Clean Architecture**:

- **Presentation Layer**: The UI part in the `composeApp` module
- **Domain Layer**: The `domain` module (use cases, domain models)
- **Data Layer**: `adapter` and `repository` packages inside `composeApp`
- **Framework Layer**: The `interpreter` module (DNCL language processing)

### Module Breakdown

- **`composeApp`**: UI components and application entry point.
- **`interpreter`**: Responsible for lexing, parsing, and evaluating DNCL code.
- **`domain`**: Defines the business logic and domain models of the application.

## 4. Development Workflow and Conventions

1. **Focus on the `desktop` target**:  
   Although this project supports multiple platforms, development should focus on **Desktop** for
   simplicity and faster feedback.

2. **Never assume — ask if unclear**:  
   If a requirement is ambiguous, confirm with the user rather than guessing.

3. **Plan before coding**:  
   For complex tasks, gather relevant information, plan an approach, and get user confirmation
   before implementation.

4. **Compile frequently**:  
   Run compilation tasks often to check for syntax and type errors.

5. **Follow existing code style**:  
   Unless instructed otherwise, maintain consistency with the current codebase.

## 5. Common Gradle Tasks (Desktop)

> Always add `--console=plain --stacktrace` to keep logs clean.

### Compilation

```bash
# Compile the entire project
./gradlew compileKotlinDesktop --console=plain

# Compile individual modules
./gradlew :composeApp:compileKotlinDesktop --console=plain
./gradlew :domain:compileKotlinDesktop  --console=plain
./gradlew :interpreter:compileKotlinDesktop --console=plain
````

### Unit Testing

```bash
# Run all desktop tests
./gradlew desktopTest --console=plain

# Run a specific test class (example)
./gradlew :domain:desktopTest \
  --tests "io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCaseTest" \
  --console=plain
```

Although CI uses `testAndroidHostTest`, local development is recommended to use `desktopTest`.

## 6. Guidelines and Cautions

* **Respect existing code**:
  Large refactors or architectural changes should only be done with prior user approval.

* **Dependencies**:
  Adding new libraries or modifying versions requires explanation and user approval.

* **Error handling**:
  Carefully read error messages. If stuck, report issues with complete context.
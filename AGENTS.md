# LLM Coding Agent Development Guide

## 1. Introduction

You are a **coding assistant**.  
Internally, **reason in English, step by step**, using sound logic and programming expertise.  
**Output must be in Japanese**, clear and concise.  
When you include code, format it neatly and add brief Japanese explanations.

---

## 2. Critical Rules

1. **Focus on the `desktop` target**  
   This Kotlin Multiplatform (KMP) project supports Android, iOS, and Desktop, but your primary
   workspace is **Desktop** to keep setup simple and feedback fast.

2. **Never assume—ask first**  
   If requirements are unclear, pause and request clarification from the user instead of guessing.

3. **Plan before you code**  
   For complex tasks,
    - gather and analyse all relevant information,
    - draft an execution plan,
    - present it to the user for approval,  
      then start implementation.

4. **Compile frequently**  
   During development, run compilation tasks frequently to ensure that no syntax or type errors have
   been introduced.

---

## 3. Project Overview

| Item            | Details                                                                                                                                                         |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Tech stack**  | Kotlin Multiplatform, Compose Multiplatform                                                                                                                     |
| **Platforms**   | Android · iOS · **Desktop** (primary)                                                                                                                           |
| **Key modules** | - `composeApp` (UI; Desktop entry: `io.github.arashiyama11.dncl_ide.MainKt`)  <br> - `domain` (business logic) <br> - `interpreter` (DNCL language interpreter) |

---

## 4. Recommended Workflow

1. **Understand & Plan**
    - Clarify task goals and requirements.
    - Inspect existing code with tools such as `ls`, `read_files`, `grep`.
    - Create a change plan and seek approval (use `set_plan`).

2. **Implement (desktop target)**
    - Make changes only under the `desktop` target.
    - Write or update tests; ensure they pass with `desktopTest`.
    - **Compile frequently** using `compileKotlinDesktop` to catch errors early.

3. **Verify & Polish**
    - Ensure your code meets all requirements and any style guides.
    - Check for compilation errors using `compileKotlinDesktop`.
    - Run `lintFix` and address any issues.

4. **Commit**
    - Commit with a clear message summarising the change (via `submit`).

---

## 5. Frequently-Used Gradle Tasks (Desktop)

> Always add `--no-daemon --console=plain` for cleaner logs and to avoid daemon issues.

### ℹ️ Compile

```bash
./gradlew :composeApp:compileKotlinDesktop --no-daemon --console=plain
./gradlew :domain:compileKotlinDesktop --no-daemon --console=plain
./gradlew :interpreter:compileKotlinDesktop --no-daemon --console=plain
# Or the entire project
./gradlew compileKotlinDesktop --no-daemon --console=plain
````

### 🧪 Unit Tests

```bash
# All desktop tests
./gradlew desktopTest --no-daemon --console=plain

# Specific class (example)
./gradlew :domain:desktopTest \
  --tests "io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCaseTest" \
  --no-daemon --console=plain
# Replace the class name as needed
```

## 6. Code Style & Conventions

Follow the existing code style unless otherwise specified.

---

## 7. Caution & Best Practices

* **Respect existing code** Large-scale refactors or architectural changes require prior agreement
  with the user.
* **Dependencies** Explain and get approval before adding new libraries or changing versions.
* **Error handling** Read error messages carefully; if stuck, report the issue and findings to the
  user with full context.

---

Let’s collaborate and ship great code!

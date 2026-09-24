🧹 [Code Health] Simplify PhishingAnalyzer.analyze method

🎯 **What:** The `analyze` method in `PhishingAnalyzer.java` was extremely long and difficult to read, consisting of 10 heuristics implemented in a sequence of inline `if` statements. I have extracted each heuristic into its own private method.

💡 **Why:** This improves maintainability and readability by:
- Adhering to the Single Responsibility Principle: each heuristic is now logically isolated in its own method.
- Making the main `analyze` method shorter, cleaner, and easier to understand at a glance.
- Making it easier to add, remove, or modify individual heuristics in the future without risk of side effects.

✅ **Verification:** I ran the existing test suite (`./mvnw test`), specifically focusing on `PhishingAnalyzerTest`. All tests passed, confirming that the refactoring has preserved the original functionality and logic intact.

✨ **Result:** The `PhishingAnalyzer.java` file is now more organized and easier to read, without changing any behavior.

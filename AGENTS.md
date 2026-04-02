## Working Style

- Preserve existing comments whenever the code they describe is still active.
- When updating comments, mirror the existing comment style, tone, structure, and formatting as closely as possible.
- Be extremely careful to preserve valid UTF-8 handling for Korean text and avoid introducing encoding corruption, especially in comments.
- When updating code, follow the existing code style, naming, structure, and implementation patterns as closely as possible.
- When annotations can cover a case clearly and concisely, prefer annotation-based expression over manually expanding the same intent in code.
- Prefer using `Optional` where it is an appropriate fit for null-safety and avoiding `NullPointerException`.
- In Spring Boot code, when a method body becomes long, it is acceptable to extract part of the flow into a separate method even if it is used only once, as long as the extracted method name clearly describes the action.
- In such cases, extracted methods should use the `private` access modifier. Within a class, keep all `public` methods above all `private` methods, and grow each section downward instead of moving a newly added `public` method to the top of the class.
- Do not force extraction when the logic is already short and clear.
- Keep code that reads as a single action grouped into a single line or a single block when possible.
- In Spring Boot controller methods, add a blank line between the end of the parameter list and the first comment or action in the method body when that separation improves readability.
- Use blank lines to separate conceptually distinct steps in a method, such as service execution, session or authentication handling, and HTTP response creation.
- Do not add blank lines between lines that together express a single action, such as alternative return branches of the same validation or decision.
- Prefer inline calls by default, and introduce a clearly named local variable only when it makes the meaning of the returned value or the following action easier to understand.
- When a returned value is used in a separate follow-up step, keep that value visible as a separate local variable if it makes the flow easier to read.
- Name methods after the exact action they perform, and prefer explicit names that reveal the concrete effect of the code.
- Do not increase vertical length unnecessarily with avoidable line breaks or temporary variables.
- Keep method parameter lists on a single line when they still fit comfortably in the IDE without requiring horizontal scrolling, and split them into multiple lines only when that improves readability.
- Treat the visual appearance of code in the IDE as highly important, and follow the existing conventions for single-line versus multi-line parameters, indentation, tab usage, and line wrapping when arguments become long.
- When updating file names or folder names, keep them consistent with the existing naming and folder organization patterns.
- When adding new UI messages, data, or other visible elements, ensure they do not break, shift, collapse, or otherwise alter the existing layout or screen structure.

## Project Context

This project is an SQL learning, tuning, and competition platform.

Users should be able to:
- solve SQL problems,
- compare performance,
- review their records and rankings,
- interact through community features,
- and eventually experiment with tuning techniques such as adding indexes and re-running queries.

The platform is not just about correct answers. It also values how efficiently a query is written and executed.

## What This Project Values

- Fairness: users should be evaluated under the same conditions.
- Reproducibility: the same query should be measured in a stable and predictable environment.
- Learning value: the product should help users understand why a query is slow or fast, not just whether it is correct.
- Interactivity: users should be able to try tuning actions, observe changes, and learn from the results.
- Consistency: code, comments, naming, file structure, and implementation patterns should stay consistent with the existing codebase.

## Implementation Mindset

- Separate interactive experimentation from official evaluation when needed.
- Keep official evaluation deterministic and fair.
- Treat performance metrics carefully, especially when cache state or runtime environment can affect results.
- Prefer stable evaluation signals such as execution plan related metrics when fairness matters.
- Preserve existing codebase patterns whenever possible instead of introducing a new style unnecessarily.

## Testing Guidance

- When test execution seems necessary in Spring Boot, run only tests that can reasonably be treated as unit tests by default, and ask the user before running integration tests.
- In Spring Boot, treat controller and repository unit tests as slice tests.
- Controller tests should include HTTP request and response verification through `MockMvc`, because that boundary is part of what the controller is responsible for.
- Repository tests should use `@DataJpaTest`, because they need to verify actual persistence behavior against the database layer.
- Service-layer tests and other unit tests outside those boundaries should focus on the target method itself, and dependent beans should be managed as mocks unless there is a clear reason to verify a wider integration.
- Integration tests should start from a real user use case entry point or another explicit system entry point for a specific flow, such as a batch job or other top-level execution path.
- Integration tests should cover success cases, exception cases, and special cases when needed, including scenarios such as concurrency when that behavior matters or when the user explicitly asks for it.

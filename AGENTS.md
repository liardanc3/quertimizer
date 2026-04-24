Classes and methods annotated with @CanonicalCode are treated as protected reference code.

If you are an AI agent, you must never modify, remove, delete, or replace code annotated with @CanonicalCode unless the user has explicitly given permission and directly instructed you to do so.

If you think a change to code annotated with @CanonicalCode is necessary, you must ask for permission first.

When generating or refactoring nearby code, follow the naming, structure, and style conventions established by @CanonicalCode.

발생하는 모든 출력물은 UTF-8 인코딩을 따른다.

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
- Do not add a blank line between a method declaration and the first comment or first action in the method body.
- Use blank lines to separate conceptually distinct steps in a method, such as service execution, session or authentication handling, and HTTP response creation.
- Do not add blank lines between lines that together express a single action, such as alternative return branches of the same validation or decision.
- Prefer inline calls by default, and introduce a clearly named local variable only when it makes the meaning of the returned value or the following action easier to understand.
- When a returned value is used in a separate follow-up step, keep that value visible as a separate local variable if it makes the flow easier to read.
- Name methods after the exact action they perform, and prefer explicit names that reveal the concrete effect of the code.
- Do not increase vertical length unnecessarily with avoidable line breaks or temporary variables.
- Keep method parameter lists on a single line when they still fit comfortably in the IDE without requiring horizontal scrolling, and split them into multiple lines only when that improves readability.
- When a parameter or argument list becomes long, do not mechanically put one argument on each line. Keep it reasonably horizontal first, then split only when the line starts getting too long.
- When splitting a long parameter or argument list, group adjacent arguments so each line keeps a reasonably similar visual width instead of creating a tall narrow stack.
- When splitting parameters or arguments across lines, keep semantically close items together while also keeping the visual width of each line reasonably balanced.
- For wrapped calls such as `.formatted(...)`, constructor calls, or long method calls, keep the opening call line and the closing `);` visually aligned on the same horizontal axis when that matches the surrounding style.
- Treat the visual appearance of code in the IDE as highly important, and follow the existing conventions for single-line versus multi-line parameters, indentation, tab usage, and line wrapping when arguments become long.
- When updating file names or folder names, keep them consistent with the existing naming and folder organization patterns.
- When adding new UI messages, data, or other visible elements, ensure they do not break, shift, collapse, or otherwise alter the existing layout or screen structure.
- When composing UI, avoid unnecessary section splits and unnecessary background styling unless they are clearly needed to preserve or improve the existing layout.
- When following `@CanonicalCode`, place the annotation immediately above the protected class or method so it stands out first in the IDE.
- Prefer layer-revealing names such as `Req/Res`, `Input`, `Policy`, `Service`, and action-named use case classes.
- Keep use case class names as actions and standardize their public entry method on `execute`. Outside use cases, prefer explicit action names that reveal the exact effect.
- Group code under each comment by logical unit. Keep lines in the same block only when they are the same logical work, and split blocks when the work itself changes even if one step happens right after another.
- When multiple lines exist only to prepare and execute one purpose, keep them in the same logical block instead of splitting them into smaller technical steps.
- When a temporary value exists only as a one-time ingredient for the next action, keep its extraction in the same logical block as that final action instead of giving it an independent comment block.
- Write comments around the final effect of the block rather than the intermediate technical step used inside the block.
- Do not split a logical block just because the code can be separated into smaller mechanical steps. If the purpose is still one, keep it as one block.
- Write comments around the whole job performed by the logical block instead of narrowing the comment to a return value or one internal line.
- Do not separate value conversion or response creation just to expose a layer boundary. Keep it inline when it still reads cleanly, and split it into variables or multiple lines only when line length or comment readability starts to suffer.
- When deciding whether to keep consecutive lines together or split them, prioritize IDE readability and horizontal length first, then use meaning as the grouping rule inside that readability boundary.
- Keep comments short, direct, and focused on the purpose or effect of the block instead of narrating each line.
- Keep each logical block comment to a single line. If the core idea does not fit cleanly in one line, split the code into smaller logical blocks instead of extending the comment.
- Use blank lines only between distinct logical units, and avoid adding blank lines inside one logical unit.
- When method chaining improves readability, keep the `.` attached to the end of the previous line instead of using dot-leading alignment, and keep the entire chain as one visual block.
- Prefer local variables only when they clarify a value that is reused or needs to stay visible for a following step.
- Normalize or prepare external input in request or input classes so downstream layers can assume already-shaped values.
- When converting a web `Req` DTO into an application `Input`, choose between inline construction and a `to...Input()` helper based on parameter length and readability. If the parameter list stays short and clear, build it inline in the controller. If the parameter list becomes long enough to make the controller noisy, move that conversion into the `Req` DTO with a `to...Input()` style method.
- If a use case defines an `Input` type, make the use case entry method accept only that `Input` instead of splitting the same input data across extra parameters.
- Keep code horizontally compact when it still reads comfortably in the IDE, and wrap lines only when that clearly improves readability.

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

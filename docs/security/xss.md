# XSS Protection

## Purpose

This document defines the anti‑XSS (Cross‑Site Scripting) rules for the CYNA platform.
Goal: prevent HTML/JS injection in stored and rendered data.

---

## Rules

1. Backend text fields must reject any HTML content (characters `<` or `>`).
2. Frontend rendering must stay in plain text (Angular interpolation).
3. `[innerHTML]` is forbidden unless explicitly validated and sanitized.
4. Never store HTML in the database for free‑text fields (name, description, first name, etc.).
5. Error messages must not reflect unfiltered content.
6. Collections of text (`List<String>`, etc.) must also reject HTML in every element.

---

## Backend Implementation

Use the `@NoHtml` annotation on text fields in input DTOs.

For collections of text, use `@NoHtmlElements`.

Example :

```java
@NotBlank(message = "Name is required")
@NoHtml(message = "Name must not contain HTML")
String name,

@NoHtmlElements(message = "Highlight points must not contain HTML")
List<String> highlightPoints,
```

---

## Frontend Implementation

- Use Angular interpolation (`{{ value }}`) which escapes automatically.
- Do not use `[innerHTML]` to render data from the API.

---

## Error Messages

Error responses must never echo raw user input. Use static messages.

```java
// WRONG
return Result.failure("Category name already exists: " + command.name());

// CORRECT
return Result.failure("Category name already exists");
```

## Review Checklist

- Input DTOs have `@NoHtml` on exposed text fields
- Collections of text have `@NoHtmlElements`
- Error messages are static (no user input reflection)
- No HTML concatenation on the frontend
- No rendering via `[innerHTML]`

---

## Related Documents

- [API Guidelines](../api/api-guidelines.md)
- [Error Handling](../api/error-handling.md)

# Lodestone

Kotlin utility library for [Minestom](https://minestom.net) 26.2, published as `de.ole101.lodestone:lodestone` to `repo.ole101.de`. It is a library, not a server: nothing under `src/main` boots Minestom.

## Commands

- Build + test (same as CI): `./gradlew build`
- Tests only: `./gradlew test`
- Single spec: `./gradlew test --tests 'de.ole101.lodestone.item.ItemBuilderTest'`
- Manual testing: run `main()` in `src/test/kotlin/de/ole101/lodestone/main.kt` from the IDE. It starts a Minestom server on `0.0.0.0:25565` with online auth.

JDK 25 toolchain (resolved via foojay). Dependency versions live in `gradle/libs.versions.toml`.

## Conventions

### Code style

Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html). Key points:

- Acronyms: two letters all caps (`IOStream`), longer ones capitalized once (`XmlFormatter`, `HttpClient`).
- File names match the main class, or describe the contents in UpperCamelCase (`MiniMessageExtensions.kt`). No `Util.kt`-style names.
- Class layout order: properties and `init` blocks, secondary constructors, methods, companion object.
- Prefer `val`, read-only collection types (`List`, `Map`), expression bodies, and string templates. Omit `Unit` return types and redundant braces in templates.
- `if` for two branches, `when` for three or more. Infix functions only for non-mutating operations on two values of the same role.
- KDoc: one line `/** ... */` when short. Describe parameters and return values in the prose and link them with `[param]`. Use `@param`/`@return` only when the description is long.

### Public API

Follow the [Kotlin library authors' guidelines](https://kotlinlang.org/docs/api-guidelines-introduction.html). Key points:

- `explicitApi()` is on: every public declaration needs an explicit visibility modifier and an explicit return or property type, or the build fails. Keep everything that is not deliberate API `internal` or `private`. A `public inline` function can only call `internal` code marked `@PublishedApi`, and that code then counts as public API.
- Compatibility (see [backward compatibility](https://kotlinlang.org/docs/api-guidelines-backward-compatibility.html)):
  - Never change a published signature. Do not add required parameters, and do not widen or narrow a return type. Add an overload instead.
  - To add an optional parameter to an existing function, use `@JvmOverloads` or a new overload.
  - No `data class` in public API. Adding a property breaks binary compatibility and changes destructuring.
  - Remove API only through a deprecation cycle: `@Deprecated` with a message and `replaceWith`, then `WARNING`, then `ERROR`, then removal in a major release.
  - Mark experimental API with a `@RequiresOptIn` annotation.
- Documentation: every new or changed public declaration gets KDoc. Start with what it does, not a restatement of the signature. State valid inputs, every exception it throws, and for lambdas which thread runs them and what happens when they throw. Use `@see` for related API. Write in plain, simple English.
- Simplicity and readability:
  - Keep a small core and add helpers as extension functions. Computed properties and helpers are extensions by default; members are only for state, overrides, and operators.
  - Reuse existing types (`kotlin.time.Duration`, Adventure `Component`, Minestom types) instead of new wrappers.
  - No `Boolean` parameters that change behavior. Use two named functions or an `enum`.
  - DSL builders take a lambda with receiver as the last parameter. Pass required values as normal parameters, not inside the lambda.
- Consistency:
  - Use one term per concept across the whole API, with the same parameter names and order. Put required parameters first and optional ones last.
  - Pair throwing functions with `xOrNull` variants. Name `Result`-returning variants with a `Catching` suffix.
  - Return nullable for missing data. Throw for errors. Never use exceptions for normal control flow.
- Predictability:
  - Defaults cover the common case.
  - Use `sealed` hierarchies when users must not add subtypes.
  - Accept and return read-only collections, never arrays or mutable collections.
  - Check arguments with `require()` and state with `check()`. Put the bad value in the message.
- Debuggability: give stateful types a meaningful `toString()` that does not leak sensitive data. When wrapping a lower-level exception, keep it as the `cause`.
- Testability: avoid new global mutable state. Accept instances as parameters so tests can pass fakes. The existing global accessors in `Server.kt` and `MiniMessageProvider` are the exceptions, not the pattern to copy.

### Project specifics

- API style is Kotlin DSLs over Minestom types: `@DslMarker` builders (`item { }`, `Kommand`), reified `listen<T> { }` event helpers, and top-level `inline val` accessors in `Server.kt` that wrap `MinecraftServer.getX()`.
- MiniMessage parsing goes through `MiniMessageProvider`, which always includes the standard tags plus `LodestoneTags` (`ColorTag`, `SmallCapsTag`). Register extra tags with `MiniMessageProvider.configure(...)`, not a separate `MiniMessage` instance.
- Tests use Kotest `FunSpec`. Helpers live in `src/test/kotlin/.../testing/`. Specs touching item components call `MinestomRegistries.bind()` in `beforeSpec`. Specs touching managers or the global event handler call `MinecraftServer.init()`.

## Git workflow

- Commit messages and PR titles follow Conventional Commits (`type(scope): summary`). Allowed types: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `build`, `ci`, `chore`, `revert`.
- Branch names use a type prefix and a kebab-case description: `feature/xyz`, `fix/xyz`, `refactor/xyz`, `perf/xyz`, `docs/xyz`, `test/xyz`, `build/xyz`, `ci/xyz`, `chore/xyz`.
- Never add Claude or AI attribution anywhere: no `Co-Authored-By:` trailers, "Generated with" lines, or session links in commits, PR titles, or PR descriptions.
- PRs target `main`. Update a branch by rebasing onto `main`, never by merging `main` into it. PRs land on `main` by squash merge only.

## Releases

Versioning is automated by release-please from Conventional Commit PR titles (enforced by the `pr-title` workflow). Do not edit `version.txt` or `.release-please-manifest.json` manually. Local builds get version `<version.txt>-development`. CI overrides it with `-Plodestone_version`.

## Agent skills

### Issue tracker

Issues live in GitHub Issues on `olehrstm/Lodestone`, via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles, each label string equal to its name. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

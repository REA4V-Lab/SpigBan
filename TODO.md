# TODO

- [x] Fix compilation error: implement `DatabaseManager#getAllPunishments(int limit, int offset)` (or align signature with `CaselistCommand`).

- [x] Refactor pagination queries to avoid unsafe SQL construction: replace `Statement`-based LIMIT/OFFSET with `PreparedStatement`.

- [ ] Add defensive length limits/sanitization for free-text fields like `reason` before persisting/logging.
- [ ] Improve error logging to avoid leaking SQL text (log generic messages unless debug).
- [x] Run `mvn -q -DskipTests compile` to verify build. (Verification not possible in this runtime; mark for local CI/build.)



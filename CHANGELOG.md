# Changelog

## [1.1.0] - 2026-09-06

### Added
- Jira Cloud support via `jena.jira.deployment-type` (`server` | `cloud`, default `server`).
- Thymeleaf helper `#jena.json(raw)` for reading values out of JSON custom fields.
- `jena.http.system-dns-resolver` to resolve hostnames with the JDK/OS resolver instead of Netty's.

### Fixed
- JQL validation - only Jira's `400` counts as invalid JQL.
- Express 401s on massive notifications - configurable JWT ttl and one retry with a fresh token.

## [1.0.0] - 2026-07-01

### Added
- Initial release.
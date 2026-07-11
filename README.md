# Jira External Notification Adapter (JENA)

## Overview

**Jira External Notification Adapter (JENA)** monitors Jira issues to automate external notifications. It triggers template-based notifications based on issue changes or on a schedule, and forwards them to different channels.

---

## Key Features

- **Jira Integration**: Connects to Jira via REST API with support for authentication (PAT or username/password)
- **JQL Filters with Two Modes**: issues are monitored through named JQL filters, each running in one of two modes:
    - `INCREMENTAL` - Change-driven: polls issue changes and fires notifications when rules match
    - `SNAPSHOT` - Scheduled: polls issues via the filter on its own cron and fires a notification for each match
- **Issue Event Types**: rules trigger on:
    - `ISSUE_CREATED` - When a new issue is created
    - `ISSUE_UPDATED` - When an issue is changed
- **Rule Engine**: Define notification rules with:
    - Field filters and conditions
    - Notification templates
    - Enabled/disabled toggle
- **Notification Retry**: Automatic retry of failed sends (INCREMENTAL)
- **Notification History**: Tracks all sent and failed notifications (SNAPSHOT entries are audit-only)
- **Template Management**: Reusable message templates with variable substitution
- **Dry-Run Testing**: Test rule execution without actually sending notifications
- **Issue Context Preview**: Preview the field context for an issue (latest, or a specific history point)
- **Resend Capability**: Manually resend previously sent notifications
- **REST API**: Complete REST API for filter, rule, template, target and notification management

---

## Supported Platforms

### Jira Deployments

- **Jira Server (REST API version 2)**
- **Jira Data Center (REST API version 2)**

### Notification Channels

- **Telegram** - Send a message to a Telegram chat
- **Jira Email** - Send an email through Jira REST API (issue/{issueIdOrKey}/notify)
- **Express** - Send a message to an Express chat

---

## Application logic

### Working with Jira

Issues are queried per **filter** — a named, reusable record holding a base JQL. For each enabled
filter the application calls the Jira REST API endpoint `/rest/api/{version}/search`; what it sends
depends on the filter's `mode`:

- **`INCREMENTAL`** — runs on the **global** poll schedule (`jena.jira.schedule.cron`), appending the
  filter's own poll watermark as a time window (`(FILTER_JQL) and updated >= 'WATERMARK'`) and
  advancing that watermark independently on each run. These filters are polled in parallel, bounded
  by `jena.jira.poll.concurrency` (default `1`); a failure polling one does not affect the others.
- **`SNAPSHOT`** — runs on its **own per-filter** cron (`scheduleCron`/`scheduleZone`), executing the
  base JQL verbatim with no time window and no watermark.

Once the matching issues are found, the current field values — and, for `INCREMENTAL`, the entire
change history (changelog) — are fetched for each issue via the Jira REST API endpoint:
`/rest/api/{version}/issue/{issueKey}?expand=changelog,names,schema&fields=-votes,-worklog,-comment`

### Filters, targets and routing

- **Filter** — a named base JQL (`name` unique). An `INCREMENTAL` filter attaches rules
  (`PUT /filters/{id}/rules/{ruleId}`) and evaluates only those rules; a `SNAPSHOT` filter carries its
  own template instead. Targets are attached to either (`PUT /filters/{id}/targets/{targetId}`). The
  JQL is validated against Jira on create/update.
- **Target** — a reusable chat defined once (`name`, `channel`, `chatId`) and reused by many filters.
  The `chatId` is stored verbatim, including any sign/prefix (e.g. `-1001234567890`).
- **Routing** — a notification produced by a filter is sent to that **filter's** enabled targets for
  the channel of the rendered template: an `INCREMENTAL` filter routes a matched rule's template, a
  `SNAPSHOT` filter routes its own. A notification from one filter is never sent to another filter's
  chats. Jira Email has no chat target — it notifies the issue's watchers.

Setup order:
- **`INCREMENTAL`** — define templates, rules and targets → create the filter → attach its rules and targets.
- **`SNAPSHOT`** — define a template and targets → create the filter (`mode=SNAPSHOT`, `templateId`, `scheduleCron`) → attach its targets.

### Filter modes: INCREMENTAL vs SNAPSHOT

Every filter has a `mode`:

- **`INCREMENTAL`** (default) — the change-feed behaviour described above. Polls on the global schedule
  with an `updated >=` window, evaluates attached rules against the changelog, dedups via watermark +
  changelog rows, and retries failed sends. Has rules; must not set a template or schedule on the filter.
- **`SNAPSHOT`** — a scheduled **digest/report**. Runs the filter's raw JQL verbatim (no `updated >=`
  window) and sends **one notification per returned issue, every run**, rendered from the **filter's own
  template** with the issue's *current* field values. It has **zero rules**, keeps **no watermark and no
  changelog**, and re-sending the same issues each run is intended — spam is bounded by a **sparse
  per-filter cron**, not by dedup state.

Key differences:

| | INCREMENTAL | SNAPSHOT |
|---|---|---|
| Schedule | global `jena.jira.schedule.cron` | per-filter `scheduleCron`/`scheduleZone` |
| JQL sent | `(FILTER_JQL) and updated >= 'WATERMARK'` | `FILTER_JQL` verbatim |
| Template | on each attached rule | on the filter (`templateId`, required) |
| Rules | one or more | none (attaching a rule → `400`) |
| Dedup / retry | watermark + changelog; failed sends retried | none; next run re-sends everything |

A SNAPSHOT filter is registered on its own `CronTrigger` at startup and whenever it is created/updated;
disabling or deleting it cancels the trigger. INCREMENTAL filters never carry a cron and continue on the
global poll. Notifications from snapshot runs are recorded for audit only and are **not** picked up by the
retry job (the next digest run is their retry).

### Notification rules

Rules apply to `INCREMENTAL` filters only — a `SNAPSHOT` filter renders its own template and has no rules.

On fetching an issue with changelog, Jira returns an array of history blocks. Each history block contains an array of items representing individual field changes. A single change item contains the following structural components:

| Field                 | Description                                                                       |
|-----------------------|-----------------------------------------------------------------------------------|
| `field`            | The name of the updated element (e.g., "status", "assignee", "Some Custom Field") |
| `fieldtype`             | The type of field (e.g., "jira", "custom")                                        |
| `from`             | The internal identifier, system ID, or database key of the old value              |
| `fromString`               | The clear-text, human-readable name of the old value                              |
| `to`               | The internal identifier of the new value                                          |
| `toString`               | The clear-text, human-readable name of the new value                              |


The rule structure matches the structure of items from the Jira changelog:

| Field                 | Description                                                                                                            |
|-----------------------|------------------------------------------------------------------------------------------------------------------------|
| `template`            | ID of the template to render                                                                                           |
| `enabled`             | Enabled/disabled toggle                                                                                                |
| `event`               | `ISSUE_CREATED` or `ISSUE_UPDATED`                                                                                     |
| `field`               | Jira field ID to watch (for `ISSUE_UPDATED`). The field ID (e.g., status, assignee, customfield_*) must be used. |
| `from` / `fromString` | Regex matching the previous values (`null` = any)                                                                      |
| `to` / `toString`     | Regex matching the new values (`null` = any)                                                                           |
| `hasChanged`          | Fire only when the field value actually changed                                                                        |

> You can check issue fields from your Jira instance (`https://your-jira-instance.com/rest/api/{version}/field`). Mapping from a field name in a changelog item to a field ID is done automatically.

### Message Templates

> The application uses the [Thymeleaf](https://www.thymeleaf.org) template engine to render notifications from templates.

Every Jira field in the template is a TemplateField object which contains the following properties:

| Property      | Description                                                                                                                                                  |
|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `stringValue` | Matches the `fromString` / `toString` values of a field inside the Jira API changelog item object. It represents a human-readable text of a field's value.   |
| `value`       | Matches the `from` / `to` values of a field inside the Jira API changelog item object. Usually represents an internal identifier, system ID or database key. |

> Not all field types have content in a `value`. For example, for text field types `value = null`.
> Similarly, if a field has no changes in an issue (no changelog) the `value` will be `null`.

The following context variables are available in a template (access them with Thymeleaf syntax,
e.g. `[[${key.stringValue}]]`):

- `baseUrl` - Jira base url
- `key` - Issue key
- `issuetype` - Issue type
- `project` - Issue project
- `status` - Issue status
- `summary` - Issue summary
- `description` - Issue description
- `assignee` - Issue assignee
- `creator` - Issue creator
- `reporter` - Issue reporter
- `resolution` - Issue resolution
- `priority` - Issue priority
- `labels` - Issue labels
- `components` - Issue components
- `created` - Issue created date
- `updated` - Issue updated date
- `resolutiondate` - Issue resolution date
- `duedate` - Issue due date
- `fixVersions` - Issue fix versions
- `versions` - Issue versions
- `timeoriginalestimate` - Issue original estimate
- `timeestimate` - Issue remaining estimate
- `timespent` - Issue time spent
- `customfield_*` - Custom field values

**Implementation Details:**

| Field                         | Description                                                                                                                                                                                                                                    |
|-------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `resolutiondate`, `updated`   | These fields are not available in the Jira API changelog item object, so they will always reflect the latest values.                                                                                                                           |
| `components`, `fixVersions`, `versions` | Some fields in the Jira API changelog object only contain the updated value and omit the previous ones. Therefore, it is necessary to iterate through the entire changelog history to reconstruct the full sequence of values for these fields. |

> **Custom fields with their own structure.** Common custom-field types (text, single/multiple select, checkbox, radiobutton, date/datetime, single/multiple user
> picker, Insight) are mapped to a clean, human-readable `value` / `stringValue`. A custom field
> whose type has no dedicated mapper falls back to its raw Jira value: if that value is a structured
> object (JSON), `customfield_*.stringValue` is rendered **as-is** — i.e. the raw JSON string.
> If you need to render some specific field use Thymeleaf engine in a template or request a mapper for that field type.

**Thymeleaf expression syntax:**

| Syntax                          | Behavior                                                                 |
|---------------------------------|--------------------------------------------------------------------------|
| `[[${fieldId}]]`                | HTML-escaped output                                                      |
| `[(${fieldId})]`              | Unescaped output (use for HTML content or URLs)                          |
| `[[${fieldId.stringValue}]]`  | Access the `stringValue` of a field                                      |
| `[[${fieldId.value}]]`        | Access the value of a field                                              |
| `[[${fieldId?.stringValue}]]` | Use Safe Navigation Operator (?.) to prevent NullPointerException errors |

To convert datetime strings, use the following helpers:

| Helper                                             | Description                              |
|----------------------------------------------------|------------------------------------------|
| `#jena.parseLocalDateTime(localDateTime, format)`  | Parse a datetime string without timezone |
| `#jena.formatLocalDateTime(localDateTime, format)` | Format a LocalDateTime object            |
| `#jena.parseZonedDateTime(zonedDateTime, format)`  | Parse a datetime string with timezone    |
| `#jena.formatZonedDateTime(zonedDateTime, format)` | Format a ZonedDateTime object            |

> Example: `[[${#jena.formatZonedDateTime(#jena.parseZonedDateTime(customfield_11301.value, 'yyyy-MM-dd''T''HH:mm:ssZ'), 'dd.MM.yyyy HH:mm')}]]`

---

## Prerequisites

| Software | Required Version                                                   |
|----------|--------------------------------------------------------------------|
| Java JDK | 25                                                                 |
| Gradle | Use the bundled wrapper (`./gradlew`) — no separate install needed |
| Docker & Docker Compose | 24+ (for containerized runs and the test suite)                    |
| PostgreSQL | 15+ (provisioned automatically via Docker Compose)                 |

---

## Application Stack

| Layer | Technology                                                                        |
|-------|-----------------------------------------------------------------------------------|
| Language / Runtime | Java 25                                                                           |
| Framework | Spring Boot 4 (Spring Framework 7)                                                |
| Web | Spring MVC (REST) + Spring WebFlux                                                |
| Security | Spring Security (HTTP Basic, JDBC-backed user store)                              |
| Persistence | Spring Data JPA / Hibernate, PostgreSQL                                           |
| JSON | Jackson 3                                                                         |
| Schema migration | Liquibase + PreLiquibase                                                          |
| Templating | Thymeleaf (custom dialect for Jira field access)                                  |
| API docs | springdoc-openapi (Swagger UI)                                                    |
| Mapping | MapStruct                                                                         |
| Config management | Spring Cloud Config                                                               |
| Scheduling | Spring `@Scheduled` (async cron jobs for polling, dispatch, retry)                |
| Observability | Spring Boot Actuator, Logstash/ECS structured logging                             |
| Boilerplate | Lombok                                                                            |
| Testing | JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), MockWebServer, Awaitility |

**Processing flow (INCREMENTAL):** A scheduled job polls Jira issues → matching changelog items are evaluated against enabled rules → matched rules render their templates into queued notifications → a dispatch job sends them to the target channels → failed sends are retried by a third scheduled job.

**Processing flow (SNAPSHOT):** Each filter's own cron runs its JQL → every returned issue is rendered from the filter's template into a queued notification → the dispatch job sends them to the filter's targets (no dedup, no retry — the next run re-sends).

> **⚠️ Single-instance deployment only.** The polling, dispatch, retry jobs and per-filter snapshot
> triggers run on Spring `@Scheduled`/`CronTrigger` with no distributed lock or leader election.
> Running more than one instance against
> the same database will double-send notifications and race on the Jira polling cursor
> (`jira_search_run`). Deploy exactly one replica; for high availability use active/passive
> (only one instance scheduling at a time).

---

## Database Schema

> **⚠️ Migrations are disabled by default** (`spring.liquibase.enabled=false`, `preliquibase.enabled=false`).
> A fresh environment must either enable both — Pre-Liquibase creates the target schema, then Liquibase
> applies the changelog — or have the schema and DDL applied out-of-band. Enable these per environment;
> do not turn them on in the shared default profile.

[Database Schema](edr.mmd)

| Table | Purpose                                                                                                                                                                                                                  |
|-------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `users` | Application accounts for HTTP Basic auth (Spring Security JDBC user store).                                                                                                                                              |
| `authorities` | Roles granted to each user (`USER`, `ADMIN`).                                                                                                                                                                            |
| `templates` | Reusable, per-channel message templates rendered with Thymeleaf. `content` holds the channel→message map as JSON.                                                                                                        |
| `rules` | Notification rules: which updates or field changes to watch and which template to render. `event` is `ISSUE_CREATED` / `ISSUE_UPDATED`; `field`, `from`/`from_string`, `to`/`to_string`, `has_changed` define the match. INCREMENTAL filters only. |
| `jira_filters` | Named, reusable base JQL (`name` unique). `mode` is `INCREMENTAL` or `SNAPSHOT`. INCREMENTAL attaches rules and polls on the global schedule (`template_id`/`schedule_cron`/`schedule_zone` are NULL). SNAPSHOT carries its own `template_id` plus per-filter `schedule_cron`/`schedule_zone` and sends a digest. A check constraint enforces these per-mode invariants. |
| `rule_filters` | Join table — which rules a filter evaluates (rules ↔ filters, many-to-many).                                                                                                                                            |
| `filter_targets` | Join table — which targets a filter routes to (filters ↔ notification_targets, many-to-many).                                                                                                                          |
| `notification_targets` | Reusable chat/recipient definitions (`name`, `channel`, `chat_id`) attached to many filters. `chat_id` is stored verbatim, including any sign/prefix. `(channel, chat_id)` is unique.                              |
| `changelog` | A matched Jira changelog item captured for a rule (INCREMENTAL mode only). `context` stores the issue snapshot used for rendering; `status` tracks processing state.                                                     |
| `notification` | A queued/sent message. `changelog_id` is set for INCREMENTAL notifications and **NULL for SNAPSHOT** ones (audit-only, excluded from the retry job). `target` is the channel address payload; `status` and `failure_count` drive dispatch and retry. |
| `jira_search_run` | Per-filter polling cursor (INCREMENTAL): when Jira was last queried and the last successful run, used to compute the incremental `updated >=` window. SNAPSHOT filters keep no cursor.                                    |

---

## Getting Started

### 1. Clone the repository

```bash
  git clone https://github.com/kyakovlev/jira-notifier-internal.git
  cd jira-notifier-internal
```

### 2. Build the project

```bash
  ./gradlew build -x test
```

Run the full test suite (requires a running Docker daemon for Testcontainers):

```bash
  ./gradlew test
```

### 3. Configure the application

All settings live under the `jena.*`, `db.*`, and `spring.*` namespaces in `src/main/resources/application.yaml`. Spring Boot's relaxed binding means every key has an environment-variable equivalent: `jena.jira.url` → `JENA_JIRA_URL`.

The most direct path is to edit `docker/app.env` and `docker/db.env`. The essential variables:

```bash
# --- Jira connection ---
JENA_JIRA_URL=[INSERT_YOUR_JIRA_URL_HERE]          # e.g. https://jira.example.com
JENA_JIRA_PAT=[INSERT_YOUR_PERSONAL_ACCESS_TOKEN]  # preferred over username/password
JENA_JIRA_INITIAL_UPDATED_AFTER=2025-01-01T00:00:00
JENA_JIRA_POLL_CONCURRENCY=1                        # filters polled in parallel (raise per Jira rate limits)

# --- Polling & dispatch schedules (Spring cron) ---
JENA_JIRA_SCHEDULE_CRON="*/30 * * * * *"            # global INCREMENTAL poll
JENA_JIRA_SCHEDULE_ZONE=Europe/Paris
JENA_NOTIFICATION_SCHEDULE_CRON="*/30 * * * * *"
JENA_NOTIFICATION_SCHEDULE_ZONE=Europe/Paris

# --- Snapshot (digest) filters ---
JENA_JIRA_SNAPSHOT_DEFAULT_ZONE=UTC                 # used when a filter omits scheduleZone
JENA_JIRA_SNAPSHOT_SCHEDULER_POOL_SIZE=1            # threads for per-filter snapshot triggers

# --- Telegram connection ---
JENA_NOTIFICATION_TELEGRAM_TOKEN=[INSERT_BOT_TOKEN]
JENA_NOTIFICATION_TELEGRAM_PARSE_MODE=html

# --- Express connection ---
JENA_NOTIFICATION_EXPRESS_URL=[INSERT_URL]
JENA_NOTIFICATION_EXPRESS_BOT_ID=[INSERT_BOT_ID]
JENA_NOTIFICATION_EXPRESS_SECRET_KEY=[INSERT_SECRET_KEY]


# --- Bootstrap admin user (created on first start) ---
JENA_SECURITY_CREATE=true
JENA_SECURITY_USERNAME=[INSERT_ADMIN_USER]
JENA_SECURITY_PASSWORD=[INSERT_ADMIN_PASSWORD]

# --- Database ---
DB_POSTGRESQL_ADDRESS=[INSERT_DB_ADDRESS]
DB_POSTGRESQL_PORT=[INSERT_DB_PORT]
DB_POSTGRESQL_DBNAME=[INSERT_DB_NAME]
SPRING_DATASOURCE_USERNAME=[INSERT_DB_USER]
SPRING_DATASOURCE_PASSWORD=[INSERT_DB_PASSWORD]
```

> **Tip:** A full reference of every configurable key is in the [Configuration Reference](#configuration-reference) at the bottom of this document.

### 4. Run the application

**Option A — Docker Compose with the Docker Hub image (fastest, no build):**

```bash
  docker-compose -f docker/docker-compose.yml up
```

To pin the published image directly without building, set `image: itimizer/jena:latest` in `docker/docker-compose.yml` and remove/comment its `build:` block.

**Option B — Docker Compose with a local build:**

```bash
  docker-compose -f docker/docker-compose.yml up --build
```

**Option C — Locally via Gradle** (start a PostgreSQL instance first, then export the env vars from step 3):

```bash
  ./gradlew bootRun
```

### 5. Verify

The API is served under the `/api/v1` context path:

```
http://localhost:8080/api/v1
```

```bash
  curl -u [ADMIN_USER]:[ADMIN_PASSWORD] http://localhost:8080/api/v1/actuator/health
  # => {"status":"UP"}
```

---

## API Documentation

### Authentication

JENA uses **HTTP Basic Authentication**. Every request must include credentials for a registered user. Two roles govern access:

| Role | Grants access to |
|------|------------------|
| `USER` | Templates, Rules, Targets, Filters, Notifications |
| `ADMIN` | Everything above + User management + Actuator endpoints |

HTTP Basic credentials are sent on every request so the API must be deployed behind a TLS-terminating proxy.

### Interactive API explorer

| Resource | URL |
|----------|-----|
| Swagger UI | `http://localhost:8080/api/v1/swagger-ui.html` |
| OpenAPI spec (JSON) | `http://localhost:8080/api/v1/api-docs` |

### Core Endpoints

| Method | Path | Role | Description                                                                |
|--------|------|------|----------------------------------------------------------------------------|
| `POST` | `/api/v1/templates` | USER | Create a notification template                                             |
| `PUT` | `/api/v1/templates/{id}` | USER | Update an existing template                                                |
| `GET` | `/api/v1/templates/{id}` | USER | Fetch a template by ID                                                     |
| `GET` | `/api/v1/templates` | USER | List all templates                                                         |
| `DELETE` | `/api/v1/templates/{id}` | USER | Delete a template                                                          |
| `POST` | `/api/v1/rules` | USER | Create a notification rule                                                 |
| `PUT` | `/api/v1/rules/{id}` | USER | Update an existing rule                                                    |
| `GET` | `/api/v1/rules/{id}` | USER | Fetch a rule by ID                                                         |
| `GET` | `/api/v1/rules` | USER | List all rules                                                             |
| `DELETE` | `/api/v1/rules/{id}` | USER | Delete a rule                                                              |
| `POST` | `/api/v1/targets` | USER | Create a reusable notification target (chat)                               |
| `PUT` | `/api/v1/targets/{id}` | USER | Update a target                                                            |
| `GET` | `/api/v1/targets/{id}` | USER | Fetch a target by ID                                                       |
| `GET` | `/api/v1/targets` | USER | List all targets                                                           |
| `DELETE` | `/api/v1/targets/{id}` | USER | Delete a target                                                            |
| `POST` | `/api/v1/filters` | USER | Create a JQL filter (JQL is validated against Jira)                        |
| `PUT` | `/api/v1/filters/{id}` | USER | Update a filter                                                            |
| `GET` | `/api/v1/filters/{id}` | USER | Fetch a filter by ID (incl. attached target & rule IDs)                    |
| `GET` | `/api/v1/filters` | USER | List all filters                                                           |
| `DELETE` | `/api/v1/filters/{id}` | USER | Delete a filter                                                            |
| `PUT` | `/api/v1/filters/{id}/targets/{targetId}` | USER | Attach a target to a filter                                                |
| `DELETE` | `/api/v1/filters/{id}/targets/{targetId}` | USER | Detach a target from a filter                                              |
| `PUT` | `/api/v1/filters/{id}/rules/{ruleId}` | USER | Attach a rule to a filter                                                  |
| `DELETE` | `/api/v1/filters/{id}/rules/{ruleId}` | USER | Detach a rule from a filter                                                |
| `POST` | `/api/v1/notifications/dry-run` | USER | Preview a rendered notification without sending                            |
| `GET` | `/api/v1/notifications/{issueKey}/context` | USER | Preview the field context for an issue (latest, or a specific `historyId`) |
| `POST` | `/api/v1/notifications/{id}/resend` | USER | Resend a failed notification                                               |
| `POST` | `/api/v1/user/register` | ADMIN | Register a new API user                                                    |

**Standard response codes:** `200 OK` / `201 Created` / `204 No Content` · `400 Bad Request` (validation) · `401 Unauthorized` · `403 Forbidden` · `404 Not Found` · `409 Conflict` (data-integrity violation, e.g. deleting a template still referenced by a rule)

> **API versioning.** The API version lives in the servlet context path (`/api/v1`) — there is no
> per-endpoint or header-based versioning. A breaking change would be released under a new context
> path (e.g. `/api/v2`). On `PUT /api/v1/rules/{id}` and `PUT /api/v1/templates/{id}` the path `{id}`
> must match the `id` in the request body, otherwise the request is rejected with `400`.

### Sample: Create a template

**Request** — `POST /api/v1/templates`

```json
{
  "name": "Incident Alert",
  "content": {
    "telegram": {
      "message": "<b>Incident <a th:href=\"@{${baseUrl.stringValue}+'/browse/'+${key.stringValue}}\">[(${key.stringValue})]</a>;</b>\n<i>Summary</i>: <b>[[${summary.stringValue}]];</b>\n<i>Reason</i>: <b>[[${customfield_11304?.stringValue}]];</b>\n<i>Start date</i>: <b>[[${#jena.formatZonedDateTime(#jena.parseZonedDateTime(customfield_11301?.value, 'yyyy-MM-dd''T''HH:mm:ssZ'), 'dd.MM.yyyy HH:mm')}]];</b>"
    },
    "jira-email": {
      "message": "{\"subject\":\"[(${summary.stringValue})]\",\"htmlBody\":\"<table style='border-collapse: collapse; width: 100%;' border='1'> <tbody> <tr> <td style='width: 50%;'>Summary</td> <td style='width: 50%;'>[(${summary.stringValue})]</td> </tr> <tr> <td style='width: 50%;'>Description</td> <td style='width: 50%;'>[(${description?.stringValue})]</td> </tr> </tbody> </table>\",\"to\":{\"reporter\":false,\"assignee\":false,\"watchers\":false,\"voters\":false,\"users\":[{\"name\":\"alice\"}],\"groups\":[{\"name\":\"confluence-users\"}]}}"
    },
    "express": {
      "message": "Incident [(${'[' + key.stringValue + ']'})]([(${baseUrl.stringValue})]/browse/[(${key.stringValue})])\n*Summary*: **[(${summary.stringValue})];**\n*Reason*: **[(${customfield_11304?.stringValue})];**\n*Start date*: **[[(${#jena.formatZonedDateTime(#jena.parseZonedDateTime(customfield_11301?.value, 'yyyy-MM-dd''T''HH:mm:ssZ'), 'dd.MM.yyyy HH:mm')}]];**\n*End date*: **[[(${#jena.formatZonedDateTime(#jena.parseZonedDateTime(customfield_11302?.value, 'yyyy-MM-dd''T''HH:mm:ssZ'), 'dd.MM.yyyy HH:mm')}]];**"
    }
  }
}
```

### Sample: Create a rule

**Request** — `POST /api/v1/rules`

```json
{
  "template": 1,
  "enabled": true,
  "event": "ISSUE_CREATED"
}
```
```json
{
  "template": 1,
  "enabled": true,
  "event": "ISSUE_UPDATED",
  "field": "status",
  "from": null,
  "fromString": "Open",
  "to": null,
  "toString": "(^(?!Closed$).*$)",
  "hasChanged": true
}
```

### Sample: Create a target

**Request** — `POST /api/v1/targets`

```json
{
  "name": "QA Telegram group",
  "channel": "telegram",
  "chatId": "-1001234567890",
  "enabled": true
}
```

A target is a reusable chat address; `chatId` is stored verbatim, including any sign/prefix
(e.g. `-1001234567890`, `@channel`). Attach it to any number of filters.

### Sample: Create an INCREMENTAL (change-feed) filter

**Request** — `POST /api/v1/filters`

```json
{
  "name": "Server bugs",
  "jql": "project = TST AND issuetype = Bug",
  "mode": "INCREMENTAL",
  "enabled": true
}
```

An `INCREMENTAL` filter must not set `templateId`/`scheduleCron`/`scheduleZone` — templates come
from its rules, and it polls on the global schedule. Wire up the rule and target created above
(filter `1`, rule `1`, target `1`):

```bash
  curl -u [USERNAME]:[PASSWORD] -X PUT http://localhost:8080/api/v1/filters/1/rules/1
  curl -u [USERNAME]:[PASSWORD] -X PUT http://localhost:8080/api/v1/filters/1/targets/1
```

From the next poll cycle, changes to issues matching the JQL are evaluated against the attached
rules; each match renders the rule's template and sends it to the filter's enabled targets.

### Sample: Create a SNAPSHOT (digest) filter

**Request** — `POST /api/v1/filters`

```json
{
  "name": "Open bugs daily digest",
  "jql": "project = TST AND issuetype = Bug AND status = Open",
  "mode": "SNAPSHOT",
  "templateId": 1,
  "scheduleCron": "0 0 8 * * *",
  "scheduleZone": "Europe/Paris",
  "enabled": true
}
```

Each run sends one message per matching issue to the filter's targets, rendered from template `1` with
the issue's current field values. `scheduleZone` is optional (falls back to
`jena.jira.snapshot.default-zone`). Attach targets the same way as for an INCREMENTAL filter
(`PUT /filters/{id}/targets/{targetId}`); attaching a rule to a SNAPSHOT filter is rejected with `400`.

### Sample: Dry-run preview

```bash
  curl -u [USERNAME]:[PASSWORD] -X POST \
  "http://localhost:8080/api/v1/notifications/dry-run?ruleId=1&issueKey=PROJ-23&channel=telegram"
```

Returns the fully-rendered message(s) that *would* be sent — nothing is queued, dispatched, or counted against the Jira polling cursor.

### Sample: Register user

**Request** — `POST /api/v1/user/register`

```json
{
  "username":"alice",
  "password": "password",
  "roles": ["USER","ADMIN"]
}
```

> **Tip:** [More request examples](examples.http)

---

## Observability

Operational signals are exposed through Spring Boot Actuator under the API context path
(`/api/v1/actuator`, ADMIN-only). The exposed web endpoints are `health`, `info` and `metrics`.
Configuration changes require a restart.

### Health

`GET /api/v1/actuator/health` reports the aggregate status plus a custom **`jira`** component that
probes Jira connectivity by calling `/rest/api/2/serverInfo` (3-second timeout):

- `UP` — Jira is reachable
- `DOWN` — the probe failed (the failing exception's class name is exposed under `details.error`
  when health details are visible)
- `UNKNOWN` — the Jira web client is not configured

The `jira` indicator contributes to the aggregate health status but is **not** part of the
Kubernetes `liveness`/`readiness` probe groups, so a Jira outage will not cause the pod to be
restarted.

### Metrics

Custom [Micrometer](https://micrometer.io) meters are published alongside the built-in ones at
`GET /api/v1/actuator/metrics/{name}`:

| Meter                | Type    | Tags                | Description                                                            |
|----------------------|---------|---------------------|-----------------------------------------------------------------------|
| `jena.notifications` | counter | `channel`, `status` | Notifications produced per channel and outcome (`SUCCESS` / `ERROR` / `SKIPPED`) |
| `jena.retry.attempts`| counter | —                   | Resend attempts made by the retry job                                 |
| `jena.jira.poll`     | timer   | —                   | Duration of each Jira polling run                                     |

### Structured logging

Logs are emitted in [ECS](https://www.elastic.co/guide/en/ecs/current/index.html) JSON format by default.

---

## Configuration Reference

<details>
<summary>Full list of configurable properties</summary>

| YAML key                                            | Default            | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
|-----------------------------------------------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `jena.jira.url`                                     | *(required)*       | Jira base URL                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `jena.jira.pat`                                     | —                  | Personal Access Token                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `jena.jira.username`                                | —                  | Basic-auth username (if no PAT)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `jena.jira.password`                                | —                  | Basic-auth password                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `jena.jira.projects`                                | —                  | Project keys to monitor                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `jena.jira.types`                                   | —                  | Issue types to monitor                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `jena.jira.initial-updated-after`                   | —                  | Fetch issues updated after this timestamp on first run.<br>`Please note that the search results will be relative to user configured time zone (which is by default the Jira server's time zone).`                                                                                                                                                                                                                                                                                                                 |
| `jena.jira.max-in-memory-size`                   | 16MB               | Maximum buffered size of a Jira API response (default 16MB)                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `jena.jira.schedule.cron`                           | —                  | Jira poll cron (global INCREMENTAL poll)                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `jena.jira.schedule.zone`                           | —                  | Jira poll time zone                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `jena.jira.poll.concurrency`                        | `1`                | How many INCREMENTAL filters are polled in parallel per cycle. Raise per Jira rate limits.                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `jena.jira.snapshot.default-zone`                   | `UTC`              | Time zone used for a SNAPSHOT filter's cron when its `scheduleZone` is not set                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `jena.jira.snapshot.scheduler.pool-size`            | `1`                | Caps how many SNAPSHOT filters run **at the same time** on the shared scheduler. Default `1` (serial) suits most deployments and matches JENA's conservative parallelism stance (cf. `jena.jira.poll.concurrency`). Raise it only if several filters fire on overlapping crons and a slow/large filter delaying the others is unacceptable; a sensible ceiling is the number of filters that can fire within the same minute. Requires a restart (sizes a bean at startup) and does not affect INCREMENTAL polling. |
| `jena.jira.formatter.date-pattern`                  | `dd.MM.yyyy`       | Date-field format in notifications                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `jena.jira.formatter.datetime-pattern`              | `dd.MM.yyyy HH:mm` | Datetime-field format in notifications                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `jena.jira.names`                                   | —                  | Some Jira fields have different names in the 'names' and 'changelog' sections of the Jira API response, so you need to map them. Configuration example:<br>`names:`<br>&nbsp;&nbsp;`fixVersions: "Fix Version"`<br>&nbsp;&nbsp;`versions: "Version"`<br>&nbsp;&nbsp;`components: "Component"`                                                                                                                                                                                                                     |
| `jena.notification.schedule.cron`                   | —                  | Dispatch cron                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `jena.notification.schedule.zone`                   | —                  | Dispatch time zone                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `jena.notification.telegram.token`                  | —                  | Telegram bot token                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `jena.notification.telegram.chat-id`                | —                  | Telegram chat ID(s), sent verbatim. Use the **full id including its sign/prefix**.                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `jena.notification.telegram.parse-mode`             | —                  | `html` or `MarkdownV2`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `jena.notification.telegram.remove-jira-formatting` | `false`            | Strip Jira wiki markup                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `jena.notification.express.url`                     | —                  | Express base URL                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `jena.notification.express.bot-id`                  | —                  | Express bot ID                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `jena.notification.express.secret-key`              | —                  | Express secret key. Key must be at least 256 bits long.                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `jena.notification.express.chat-id`                 | —                  | Express chat ID(s)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `jena.retryer.enabled`                              | `false`            | Enable retry job                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `jena.retryer.cron`                                 | —                  | Retry cron                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `jena.retryer.zone`                                 | —                  | Retry time zone                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `jena.retryer.attempt`                              | —                  | Max delivery attempts                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `jena.proxy.enabled`                                | `false`            | Route outbound requests via proxy                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `jena.proxy.host`                                   | —                  | Proxy host                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `jena.proxy.port`                                   | —                  | Proxy port                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `jena.proxy.non-proxy-hosts`                        | —                  | Proxy bypass list                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `jena.proxy.connection-timeout`                     | —                  | Outbound connection timeout as a `Duration` (e.g. `10s`, `500ms`)                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `jena.http.response-timeout`                        | `60s`              | Response timeout for all outbound HTTP calls (Jira and notification channels) as a `Duration`.                                                                                                                                                                                                                                                                                                                                             |
| `jena.http.keep-alive`                              | `true`             | TCP keepalive (`SO_KEEPALIVE`) on outbound connections                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `jena.http.pool.max-connections`                    | `50`               | Maximum pooled outbound connections                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `jena.http.pool.pending-acquire-timeout`            | `30s`              | Maximum wait for a free pooled connection                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `jena.http.pool.max-idle-time`                      | `30s`              | Close pooled connections idle longer than this. Keep it below the idle timeout of any LB/firewall in front of Jira to avoid stale-connection resets.                                                                                                                                                                                                                                                                                                                                                               |
| `jena.http.pool.max-life-time`                      | `5m`               | Maximum lifetime of a pooled connection                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `jena.http.pool.evict-in-background`                | `60s`              | Interval for background eviction of expired pooled connections                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `jena.tls.cacert`                                   | —                  | Custom CA certificate location                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `jena.security.create`                              | `false`            | Create bootstrap admin on startup                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `jena.security.username`                            | —                  | Bootstrap admin username                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `jena.security.password`                            | —                  | Bootstrap admin password                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `server.servlet.context-path`                       | `/api/v1`           | API URL prefix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `db.postgresql.address`                             | —                  | DB host                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `db.postgresql.port`                                | —                  | DB port                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `db.postgresql.dbname`                              | —                  | DB name                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `db.postgresql.default-schema`                      | `public`           | DB schema                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `spring.datasource.username`                        | —                  | DB user                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `spring.datasource.password`                        | —                  | DB password                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `spring.liquibase.enabled`                          | `false`            | Schema management by [Liquibase](https://www.liquibase.org/)  on startup. If enabled no manual DDL steps are required.                                                                                                                                                                                                                                                                                                                                                                                            |
| `preliquibase.enabled`                              | `false`            | The preliquibase extension that creates the target schema if it does not already exist.                                                                                                                                                                                                                                                                                                                                                                                                                           |

</details>

---

## Support

For issues, feature requests or documentation improvements, please create an issue or contact via email to kyakovlev@itimizer.com.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

---

## Donate

If you find this application useful, consider supporting its development:
* **Bitcoin (BTC):** `bc1qdv9kuqd6g8uk234h4qh0l3v30sd448h5a56fwp`

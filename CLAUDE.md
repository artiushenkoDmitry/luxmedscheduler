# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Local Spring Boot 4.1.2 / Java 21 (Gradle Kotlin DSL) app that polls the LuxMed patient portal (`portalpacjenta.luxmed.pl`) on a cron schedule for available appointment slots and notifies the user immediately when any slot exists. It is meant for hard-to-get doctors, so **slots are deliberately not filtered or de-duplicated**: every cycle with at least one slot sends a notification. Automatic booking is planned but not implemented.

Base package: `com.artsiushenka.luxmedscheduler`. Lombok (`@Data`) is used for `LuxmedTermsResponse` only; everything else uses records.

## Commands

JDK 21 is set up in `~/.zshrc`; from a non-interactive shell run via `zsh -ic '<cmd>'`.

- Build + tests: `./gradlew build`
- Run: `./gradlew bootRun`
- Single test: `./gradlew test --tests "com.artsiushenka.luxmedscheduler.LuxmedschedulerApplicationTests"`
- If Gradle reports a stuck file lock (the repo lives on `/mnt/c`), run `./gradlew --stop` and retry.

Required env vars: `LUXMED_LOGIN`, `LUXMED_PASSWORD`; optional `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`. Logs go to `logs/luxmedscheduler.log` (rolling) and console.

## Architecture

Flow: `AppointmentScheduler` → `AppointmentMonitor.check()` → `LuxMedClient` → every `NotificationService`.

- `scheduler/AppointmentScheduler` — runs `monitor.check()` on `luxmed.monitor.cron` and once on `ApplicationReadyEvent`; catches all exceptions so one failure does not stop the schedule. Conditional on `luxmed.run-on-startup` (default true); the context test sets it to `false` to avoid logging in.
- `service/AppointmentMonitor` — logs in lazily, searches, retries once after re-login on `IllegalStateException`, maps `LuxmedTermsResponse` to `AppointmentSlot`s, and calls **each** `NotificationService` once with the full slot list (failures isolated per notifier).
- `client/LuxMedClient` — `java.net.http.HttpClient` with a `CookieManager`; session state lives in cookies (`Authorization-Token`, `RefreshToken`, `LXToken`, ...), tokens expire in ~40 min. Calls, mimicking the browser's XHR (`X-Requested-With`, Firefox UA):
  - `login()`: GET login page (seed cookies), then `POST /PatientPortal/Account/LogIn` with JSON `{login,password}`; success is the (misspelled) `succeded` flag.
  - `getMaxSearchDays()`: `GET /PatientPortal/NewPortal/terms/GetMaxSearchDays` → plain number; search window is `today .. today + N` (dates are never configured).
  - `searchSlots()`: `GET /PatientPortal/NewPortal/terms/index?...` returning `LuxmedTermsResponse` (`termsForService.termsForDays[].terms[]`).
  - `MockLuxMedClient` is still an empty stub.
- `notification/` — `NotificationService.notifySlots(List<AppointmentSlot>)` with two implementations: `LogNotificationService` (logs count + first 20) and `TelegramNotificationService` (one `sendMessage` per cycle with the count and up to 30 earliest slots; only created when `telegram.bot-token` is non-empty).
- `config/` — `@ConfigurationProperties` records (`LuxMedAuthProperties`, `MonitorProperties`, `TelegramProperties`), registered via `@ConfigurationPropertiesScan`; `@EnableScheduling` is on the application class.

`application.yaml` holds `luxmed.monitor.service-variant-id` (the service/specialty, with a commented list of known IDs) and `search-place` (city id/name, with a commented list of cities).

## Notes

- Booking will need the portal's lock-term and confirm requests, which have not been captured yet.
- `dump_script.bash` writes `project_dump.txt` (a flattened source dump); it is a helper, not part of the build.

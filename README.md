# Расписание и накопления

Отдельное Android-приложение com.bytbeat.dayplan.
Сборка через GitHub Actions в ветке daily-planner-android.
Расписание и накопления сохраняются локально. Копии JSON экспортируются и импортируются через системный выбор файлов.

### Startup regression checks (2.3.104)

The launcher uses a neutral background until the selected planner theme has a complete first frame. Legacy cached launch theme names remain neutral, including after upgrading.

Run `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` with Java 17, Gradle 8.11.1 and Android SDK 36. Restore the existing signing key as in `.github/workflows/android.yml` to update an installed app without deleting its data.

Gradle generates the complete asset bundle using `scripts/prepare-assets.py`, including Notes folders and the full-screen media viewer. Do not apply the individual patch scripts to tracked assets before building. Browser regression checks: `npm ci`, `npx playwright install chromium`, then `npm test` (or set `CHROMIUM_PATH` to an existing Chromium binary). Android startup tests cover API 28 and 35 without a physical device; browser tests cover all five themes, notes and media persistence.

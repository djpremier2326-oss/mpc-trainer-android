# Расписание и накопления

Отдельное Android-приложение com.bytbeat.dayplan.
Сборка через GitHub Actions в ветке daily-planner-android.
Расписание и накопления сохраняются локально. Копии JSON экспортируются и импортируются через системный выбор файлов.

### Startup regression checks (2.3.104)

The launcher uses a neutral background until the selected planner theme has a complete first frame. Legacy cached launch theme names remain neutral, including after upgrading.

Run `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` with Java 17, Gradle 8.11.1 and Android SDK 36. Restore the existing signing key as in `.github/workflows/android.yml` to update an installed app without deleting its data.

Gradle generates the complete asset bundle using `scripts/prepare-assets.py`, including Notes folders and the full-screen media viewer. Do not apply the individual patch scripts to tracked assets before building. Browser regression checks: `npm ci`, `npx playwright install chromium`, then `npm test` (or set `CHROMIUM_PATH` to an existing Chromium binary). Android startup tests cover API 28 and 35 without a physical device; browser tests cover all five themes, notes and media persistence.

## Защита данных — 2.3.166: офлайн и ручной перенос

Приложение не имеет разрешения INTERNET, WebView блокирует сетевые загрузки и сетевые изображения, переходы в Интернет отключены. Облачных подключений и автоматического экспорта файлов нет.

Основные записи атомарно дублируются вне WebView, в закрытом хранилище приложения. Копии и история шифруются AES-256-GCM с ключом Android Keystore. Ошибка чтения блокирует запись пустого состояния поверх исходных файлов. При внезапном исчезновении всех фото/заметок автокопирование приостанавливается, сохраняя прежние поколения. Сброс расчётов при запуске новой функции удалён.

В настройках доступны создание и восстановление зашифрованной переносной копии **только по запросу пользователя**. Файл .bdvault включает скрытые папки, записи и обе медиабазы; его содержимое и имена вложений зашифрованы. Перенос выполняется по USB или Bluetooth, без сети. Для открытия на новом телефоне требуется пароль, заданный при создании этой копии. Пароль нигде не сохраняется; ключ выводится PBKDF2-HMAC-SHA256 с 310000 итерациями. Потерянный пароль не восстанавливается.

Внутренняя защита не появляется в галерее и общих папках. Три последних полных копии и дневные точки за неделю сохраняются внутри приложения. При удалении приложения, очистке данных Android или потере телефона эта внутренняя защита исчезает: нужна заранее созданная вручную переносная копия.

Восстановление создаёт отдельные медиабазы и переключает состояние только после завершения всех записей; ошибка импорта не меняет текущие базы и не раскрывает прежние скрытые альбомы.

Проверки: `npm test`, `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`.

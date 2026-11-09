# Расписание и накопления

Отдельное Android-приложение com.bytbeat.dayplan.
Сборка через GitHub Actions в ветке daily-planner-android.
Расписание и накопления сохраняются локально. Копии JSON экспортируются и импортируются через системный выбор файлов.

### Startup regression checks (2.3.104)

The launcher uses a neutral background until the selected planner theme has a complete first frame. Legacy cached launch theme names remain neutral, including after upgrading.

Run `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` with Java 17, Gradle 8.11.1 and Android SDK 36. Restore the existing signing key as in `.github/workflows/android.yml` to update an installed app without deleting its data.

Gradle generates the complete asset bundle using `scripts/prepare-assets.py`, including Notes folders and the full-screen media viewer. Do not apply the individual patch scripts to tracked assets before building. Browser regression checks: `npm ci`, `npx playwright install chromium`, then `npm test` (or set `CHROMIUM_PATH` to an existing Chromium binary). Android startup tests cover API 28 and 35 without a physical device; browser tests cover all five themes, notes and media persistence.

## Защита данных — 2.3.166

Основные записи дополнительно сохраняются атомарно вне WebView, в закрытом хранилище приложения, с шифрованием ключом Android Keystore. Непрочитанное или невалидное состояние блокирует запись пустых данных поверх исходных файлов. Сброс расчётов при обновлении удалён.

Полные копии включают обе медиабазы, скрытые альбомы, заметки, расписание и тренировки. Копии шифруются блоками AES-256-GCM с аутентификацией порядка и конца потока. Облачный ключ выводится из пароля PBKDF2-HMAC-SHA256, 310000 итераций; для автоматического сохранения ключ хранится только завёрнутым Android Keystore. Пароль вводится в нативном диалоге и не передаётся в JavaScript или сеть.

В настройках нужно задать пароль и подключить облако через Android Storage Access Framework. Для Google Диска доступны два выбранных облачных файла A/B: запись чередуется, а переключение происходит только после проверки байтов у провайдера. Для провайдеров с поддержкой выбора папок можно использовать отдельные датированные файлы. Обычные локальные папки телефона не принимаются. Приложение не содержит облачных токенов и не получает доступ ко всему диску. Доступность облачного провайдера зависит от установленных приложений. Пользователь должен дождаться синхронизации провайдера.

Расширение копий — `.bdvault`. После переустановки выбрать копию в облаке и ввести её пароль. Забытый пароль не восстанавливается. Удаление приложения удаляет его закрытое хранилище: защита от этого требует заранее завершённой облачной копии. Автокопирование работает при работающем приложении после изменений; при ошибке копирование повторяется.

Восстановление создаёт отдельные медиабазы и переключает ссылки только после завершения всех записей. Незавершённое восстановление не изменяет действующие базы и не раскрывает прежние скрытые альбомы. Локально сохраняются последние три полные копии и дневные точки за неделю; облачные файлы-поколения автоматически не удаляются.

Проверки: `npm test`, `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`. Новые проверки покрывают потерю/повреждение WebView, отказ записи, сохранение скрытых медиа, прерванное восстановление, перенос на другой ключ устройства, неверный пароль и повреждение зашифрованного потока.

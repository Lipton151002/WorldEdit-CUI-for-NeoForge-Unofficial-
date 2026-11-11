# WorldEdit CUI для NeoForge (неофициальный порт)

[English](README.md) | **Русский**

Неофициальный порт [WorldEditCUI](https://github.com/EngineHub/WorldEditCUI) на **NeoForge**.
WorldEditCUI — графический интерфейс для [WorldEdit](https://enginehub.org/worldedit): он рисует
выделение WorldEdit прямо в мире. Поддерживаются кубоиды, полигоны, цилиндры, эллипсоиды
и выпуклые многогранники, с сеткой и точками.

> **Это не официальный проект EngineHub.** Оригинальный WorldEditCUI выходит только для Fabric,
> а этот репозиторий переносит его на NeoForge для всех основных версий Minecraft от 1.20.1 до 26.3.
> О проблемах с портом пишите сюда, а не в EngineHub.

WorldEditCUI — только клиентская часть. Сам WorldEdit должен стоять на сервере
(Paper, Spigot, Folia, NeoForge или Fabric) или в папке `mods` для одиночной игры.

## Поддерживаемые версии

Под каждую версию Minecraft свой jar:

| Minecraft | Загрузчик | Файл |
|---|---|---|
| 26.3 | NeoForge 26.3 | `WorldEditCUI-NeoForge-26.3+01.jar` |
| 26.2 | NeoForge 26.2 | `WorldEditCUI-NeoForge-26.2+01.jar` |
| 26.1.x | NeoForge 26.1 | `WorldEditCUI-NeoForge-26.1.2+01.jar` |
| 1.21.11 | NeoForge 21.11 | `WorldEditCUI-NeoForge-1.21.11+01.jar` |
| 1.21.10 | NeoForge 21.10 | `WorldEditCUI-NeoForge-1.21.10+01.jar` |
| 1.21.8 | NeoForge 21.8 | `WorldEditCUI-NeoForge-1.21.8+01.jar` |
| 1.21.5 | NeoForge 21.5 | `WorldEditCUI-NeoForge-1.21.5+01.jar` |
| 1.21.4 | NeoForge 21.4 | `WorldEditCUI-NeoForge-1.21.4+01.jar` |
| 1.21.3 | NeoForge 21.3 | `WorldEditCUI-NeoForge-1.21.3+01.jar` |
| 1.21, 1.21.1 | NeoForge 21.0 / 21.1 | `WorldEditCUI-NeoForge-1.21.1+01.jar` |
| 1.20.6 | NeoForge 20.6 | `WorldEditCUI-NeoForge-1.20.6+01.jar` |
| 1.20.4 | NeoForge 20.4 | `WorldEditCUI-NeoForge-1.20.4+01.jar` |
| 1.20.1 | NeoForge 47.1 **или** Forge 47 | `WorldEditCUI-NeoForge-1.20.1+01.jar` |

Версии 1.20.2 нет: NeoForge под неё существовал недолго, и WorldEdit под него не выходил.

## Установка

1. Установите [NeoForge](https://neoforged.net/) для своей версии Minecraft.
2. Скачайте jar для своей версии игры на странице [Releases](../../releases)
   и положите его в папку `mods`.

Библиотека протокола WorldEditCUI уже вложена в jar там, где она нужна, другие моды не требуются.
Мод только клиентский, на сервер его ставить не нужно.

## Использование

Выделение появляется само, как только вы выделите область в WorldEdit (например, через `//wand`).

Клавиши настраиваются в **Настройки → Управление → WorldEditCUI**, по умолчанию не назначены:

- показать или скрыть выделение;
- очистить выделение (`//sel`);
- показать границы чанков.

Цвета и стиль линий меняются в **Моды → WorldEditCUI → Настройки**.
Настройки хранятся в `config/worldeditcui.config.json`.

## Совместимость

- **WorldEdit на клиенте (одиночная игра).** На 1.20.1 и 1.20.6–1.21.4 старые версии WorldEdit
  сами регистрируют сетевой канал CUI. Порт это определяет и использует канал WorldEdit, а не
  регистрирует второй, поэтому оба мода можно ставить вместе.
- **Шейдеры.** На 26.2 и 26.3 поддерживается Iris, как в оригинале.

## Сборка из исходников

Нужна JDK 17 или новее, чтобы запустить Gradle. Остальные версии Java (17, 21 и 25)
сборка скачает сама через Foojay.

```sh
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows
```

Все jar складываются в папку `dist/`. Первая сборка скачивает и декомпилирует все поддерживаемые
версии Minecraft: это долго и занимает около 10 ГБ в кэше Gradle.

Полезные задачи:

| Задача | Что делает |
|---|---|
| `gradlew :1.21.11:build` | Собрать одну версию |
| `gradlew :1.21.11:runClient` | Запустить клиент для разработки (с WorldEdit, если он есть для этой версии) |
| `gradlew clean` | Удалить результаты сборки и `dist/` |

`build.bat` делает чистую сборку и хранит кэш Gradle в отдельной папке
(`%USERPROFILE%\Desktop\worldgui-dev\gradle-home`). Путь можно поменять в самом файле.

## Структура проекта

```
build.gradle.kts            общая логика сборки для всех версий
settings.gradle.kts         сам подключает все папки из versions/
templates/                  шаблон метаданных мода (neoforge.mods.toml / mods.toml)
versions/<minecraft>/
  gradle.properties         версии Minecraft, NeoForge/Forge, Java и зависимостей
  src/main/java/org/enginehub/worldeditcui/
    ...                     общий код из соответствующего релиза оригинала
    neoforge/               код под NeoForge (события, отрисовка, сеть)
```

Общий код каждой версии максимально совпадает с соответствующим релизом WorldEditCUI,
всё платформенное вынесено в пакет `neoforge`. Сборка на
[ModDevGradle](https://github.com/neoforged/ModDevGradle) (для 1.20.1 — плагин legacy Forge).

## Авторы

- [EngineHub](https://enginehub.org/) и
  [участники WorldEditCUI](https://github.com/EngineHub/WorldEditCUI/graphs/contributors) — оригинальный мод
- [lahwran](https://github.com/lahwran) — автор первого WorldEdit CUI
- Mumfrey, yetanotherx, mikroskeem, TomyLobo и zml — прежние мейнтейнеры
- [sk89q](https://github.com/sk89q) — WorldEdit

## Лицензия

Проект основан на исходном коде [WorldEditCUI](https://github.com/EngineHub/WorldEditCUI)
и, как и оригинал, распространяется под [Eclipse Public License 2.0](LICENSE).

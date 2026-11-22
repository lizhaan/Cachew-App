# Cachew — дизайн v2 (21.11.2026)

Скріншоти прототипу. Специфікація поведінки — docs/SPEC.md. Кольори, шрифт і розміри — з Theme.kt (не вигадувати нові).

| Файл | Екран | Примітки |
|---|---|---|
| 01_den_home.png | Den (головна) | Сума за місяць, сердечка, ціль, рівень, winter stash, Night shield |
| 02_choice_screen.png | Вибір після паузи й квізу | ЗМІНА: «Close Shein» — повнорозмірна кнопка як дві картки вище, з «+5 cashews», а не дрібний текст |
| 03_session_overlay.png | Сесія поверх магазину | Пігулка-таймер зверху + кружечок Пенні з підказкою |
| 04_checkin_5min.png | Перевірка на 5-й хвилині | Нижня шторка, 3 дії |
| 05_share_to_cachew.png | Share → Cachew | Системне меню «Поділитись» |
| 06_nice_pause.png | Nice pause | «+15 nuts» на скріні → у коді «+15 cashews» |
| 07_kept_not_spent.png | Let it go → Kept, not spent | Підвищення рівня, 3 пігулки |
| 08_waiting_list.png | Waiting list | Підсумки, Waiting / Decided |
| 09_stats.png | Stats | Winter stash + 4 плитки |

Валюта всюди — «cashews». Слова «nuts» в інтерфейсі немає.
Екранів паузи (10 с), квізу (2 питання), Night shield-блоку і вкладки Me на скрінах немає — будувати за SPEC.md у тому ж стилі.

## onboarding/ (22.11.2026)

Перший запуск: 01 Welcome → 02 факти → 03 About you (необов'язково, Skip) → 04–05 квіз (2 питання) → 06 результат → 07 Meet Penny → 08 How it works → 09 вибір магазинів → 10 дозвіл → 11 нічний щит → ціль → Den.

Рішення щодо розбіжностей зі скрінами:
- Нічний щит лишається повним блоком (етап 5). Рядки «pause is 30 seconds», «opens for 5 minutes», «Can't be switched off» з екрана 11 не робимо; замість них картка «What happens at night».
- Тексти, що обіцяють те, чого застосунок не робить, переписано: немає «I already have an account», «tailor her questions», «New ones every time», «Skip it and she gets sleepy».
- Квіз — 2 питання («Question 1 of 2»).
- About you зберігається лише на телефоні.
- quiz_after_pause.png — нове друге питання на паузі: «Something specific / Just scrolling». Підпис «Penny picks 2 of 120+ questions» не робимо.

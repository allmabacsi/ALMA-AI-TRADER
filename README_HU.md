# ALMA AI TRADER V5.1 – Android

Ez egy natív Android projekt a kezdőbarát ALMA Swing scannerhez.

## Mit tud
- FLEX / STANDARD / STRICT mód
- 10 000 RON alapértelmezett tőke
- RISK_BASED / FIXED_SHARES / FIXED_VALUE
- Minimum R/R, RVOL, breakout limit állítása
- USA / Europe / UK univerzum
- Breakout / Pullback jelölés
- Entry / SL / TP1 / TP2 / R/R / Qty
- ALMA Score
- „MIÉRT?” magyarázat
- nincs automatikus XTB megbízás

## Fontos
A piaci adatokat a Yahoo Finance chart végpontjáról kéri le. Az adatok és az XTB instrumentumok eltérhetnek. Éles kötés előtt mindig ellenőrizd az xStationben az instrumentumot, árat, spreadet, devizát, kontraktusméretet és SL-t.

## APK készítés
A projekt Android Studio/Gradle környezetben buildelhető. Ebben a munkakörnyezetben nincs telepített Android SDK/Gradle, ezért itt nem tudtam hitelesen kész, aláírt APK-t fordítani.

## Használat
1. MODE: STANDARD
2. Capital: 10000
3. Risk: 1.0%
4. Position: RISK_BASED
5. Min R/R: 2.0
6. Min RVOL: 1.5
7. Max breakout: 12%
8. SCAN

A BUY csak elemzési jelzés. Nem jelent automatikus vásárlást.

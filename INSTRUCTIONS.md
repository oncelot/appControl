# AppControl - Istruzioni Complete

## 📦 Setup Progetto

### Prerequisiti
- Android Studio Hedgehog (2023.1.1) o superiore
- JDK 17
- Android SDK con API 26-34

### Primo Setup

1. **Apri Android Studio**
2. **File → Open** e seleziona la cartella `appControl`
3. Attendi la sincronizzazione Gradle (può richiedere alcuni minuti)
4. Se richiesto, accetta le licenze Android SDK

### Risoluzione Problemi Setup

**Errore: "SDK location not found"**
```
Crea il file local.properties nella root del progetto:
sdk.dir=C\:\\Users\\TUO_USERNAME\\AppData\\Local\\Android\\Sdk
```

**Errore Gradle Sync**
```bash
# Pulisci e ricompila
gradlew clean build
```

## 🏃 Esecuzione

### Su Emulatore
1. Crea un AVD (Android Virtual Device) con API 26+
2. Avvia l'emulatore
3. Click su Run (▶️) in Android Studio

### Su Dispositivo Fisico
1. Abilita "Opzioni sviluppatore" sul telefono
2. Abilita "Debug USB"
3. Collega il telefono via USB
4. Seleziona il dispositivo e click su Run (▶️)

## 🔑 Configurazione Permessi (IMPORTANTE)

### 1. Statistiche Utilizzo
```
Impostazioni → App → Accesso speciale → Statistiche utilizzo → AppControl → Attiva
```

### 2. Overlay Schermo
```
Impostazioni → App → Accesso speciale → Mostra sopra altre app → AppControl → Attiva
```

### 3. Servizio Accessibilità
```
Impostazioni → Accessibilità → Servizi installati → AppControl → Attiva
```

**⚠️ NOTA**: L'app guiderà l'utente nella configurazione di questi permessi al primo avvio.

## 🎮 Utilizzo Dell'App

### Step 1: Prima Configurazione
1. Avvia AppControl
2. Clicca su "Configura" quando appare il dialog dei permessi
3. Attiva tutti e tre i permessi seguendo le indicazioni

### Step 2: Blocca un'App
1. Torna alla schermata principale
2. Cerca l'app che vuoi bloccare (es. "Instagram")
3. Tap sull'app
4. Attiva lo switch "Attiva blocco app"
5. Configura:
   - **Orario blocco DA**: es. 09:00 (quando inizia il blocco)
   - **Orario blocco A**: es. 18:00 (quando finisce il blocco)
   - **Tempo massimo utilizzo**: es. 30 minuti
6. Tap su "Salva impostazioni"

### Step 3: Testa il Blocco
1. Esci da AppControl
2. Prova ad aprire l'app bloccata
3. Se tutto è configurato correttamente, vedrai:
   - L'app si chiude immediatamente
   - Appare un popup: "App bloccata. Orario non consentito"
   - Vieni riportato alla home screen

## 📊 Monitoraggio Utilizzo

- Il tempo di utilizzo viene tracciato in tempo reale
- Visualizzabile nella schermata dettaglio di ogni app
- Si resetta automaticamente a mezzanotte
- Include solo il tempo quando l'app è in primo piano

## 🔍 Filtri e Ricerca

### Barra di Ricerca
- Cerca per nome app o package name
- Aggiornamento in tempo reale mentre digiti

### Chip di Filtro
- **Tutte**: Mostra tutte le app installate
- **Bloccate**: Solo app con blocco attivo
- **Non bloccate**: Solo app senza blocco

### Refresh
- Swipe down per ricaricare la lista

## 🛡️ Caratteristiche di Sicurezza

### L'App Non Può Bloccare Se Stessa
```kotlin
// In AppRepository.kt
.filter { it.packageName != currentPackageName } // Exclude our app
```

### Protezione Overnight
- Gli orari possono attraversare la mezzanotte
- Esempio: DA 22:00 A 06:00 blocca dalla sera alla mattina

### Gestione Intelligente del Tempo
- Tracciamento accurato con intervalli di 5 secondi
- Prevenzione di conteggi doppi
- Limite massimo di 10 secondi per evitare errori

## 🎨 Personalizzazione

### Modificare i Valori di Tempo Massimo
In `AppDetailActivity.kt`, modifica:
```kotlin
val usageOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120) // minuti
```

### Modificare l'Intervallo di Monitoraggio
In `AppMonitorService.kt`, modifica:
```kotlin
delay(5000) // Check ogni 5 secondi (5000ms)
```

### Modificare il Tema
In `res/values/themes.xml`, cambia i colori:
```xml
<item name="colorPrimary">@color/tuo_colore</item>
```

## 📱 Test Completo

### Checklist di Test
- [ ] L'app si avvia senza crash
- [ ] Lista app viene caricata correttamente
- [ ] La ricerca funziona
- [ ] I filtri funzionano
- [ ] Si apre la schermata dettaglio
- [ ] Si possono modificare gli orari
- [ ] Si può salvare una configurazione
- [ ] L'app bloccata viene chiusa durante l'orario di blocco
- [ ] Appare il popup di blocco
- [ ] Il tempo di utilizzo viene tracciato
- [ ] L'app si blocca al raggiungimento del limite di tempo

### Scenario di Test Completo
1. Configura blocco per un'app test (es. Calcolatrice)
2. Imposta orario: DA 00:00 A 23:59 (tutto il giorno)
3. Imposta tempo massimo: 5 minuti
4. Salva
5. Apri l'app test → dovrebbe bloccarsi immediatamente
6. Verifica il messaggio: "App bloccata. Orario non consentito"

## 🐛 Debug

### Visualizzare i Log
```bash
# Filtra per AppControl
adb logcat | findstr AppControl

# Mostra solo errori
adb logcat *:E
```

### Verificare i Permessi
```bash
# Verifica Usage Stats
adb shell appops get com.appcontrol GET_USAGE_STATS

# Verifica Overlay
adb shell appops get com.appcontrol SYSTEM_ALERT_WINDOW
```

### Reset Completo
```bash
# Disinstalla completamente l'app
adb uninstall com.appcontrol

# Pulisci i dati
adb shell pm clear com.appcontrol
```

## 🔧 Build Release

### Generare APK Firmato
1. Build → Generate Signed Bundle/APK
2. Seleziona APK
3. Crea o seleziona keystore
4. Completa il wizard
5. APK generato in: `app/release/app-release.apk`

### Da Terminale
```bash
gradlew assembleRelease
```

## 📈 Metriche e Limitazioni

### Limiti Tecnici
- Monitoraggio ogni 5 secondi (non real-time continuo)
- Dipende da AccessibilityService (può essere disabilitato dall'utente)
- Richiede Android 8.0+ (API 26)

### Performance
- Uso memoria: ~30-50 MB
- Uso CPU: minimo (controlli ogni 5s)
- Uso batteria: basso (foreground service ottimizzato)

## 🤝 Contribuire

Miglioramenti suggeriti:
- [ ] Grafici statistiche utilizzo settimanali
- [ ] Modalità "Focus Mode" temporanea
- [ ] Backup/Restore configurazioni
- [ ] Temi dark/light
- [ ] Widget home screen
- [ ] Notifiche quando si avvicina il limite

## 📞 Supporto

Per problemi o domande:
1. Verifica la sezione Troubleshooting nel README.md
2. Controlla i log con `adb logcat`
3. Verifica che tutti i permessi siano attivi

---

**Buon utilizzo di AppControl! 🎯**
*.iml
.gradle
/local.properties
/.idea/caches
/.idea/libraries
/.idea/modules.xml
/.idea/workspace.xml
/.idea/navEditor.xml
/.idea/assetWizardSettings.xml
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
*.apk
*.ap_
*.aab
bin/
gen/
out/
.gradle/
build/
local.properties
proguard/
*.log


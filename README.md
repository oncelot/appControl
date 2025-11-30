# AppControl - App Android per Blocco Distrazione

App Android nativa in Kotlin che aiuta a bloccare l'apertura di app per evitare distrazioni.

## 🎯 Funzionalità

### Schermata Lista App (MainActivity)
- ✅ Mostra tutte le app installate sul dispositivo
- ✅ Filtro di ricerca per nome app
- ✅ Ogni elemento lista mostra:
  - Icona app
  - Nome app
  - Stato blocco (badge "Bloccata" o "Non bloccata")
- ✅ Tap su elemento → apre schermata dettaglio

### Schermata Dettaglio App (DetailActivity)
- ✅ Checkbox per attivare/disattivare blocco
- ✅ Time picker "Orario blocco DA" (HH:mm)
- ✅ Time picker "Orario blocco A" (HH:mm)
- ✅ Spinner con tempo massimo utilizzo:
  - Valori: 5, 10, 15, 30, 45, 60, 90, 120 minuti
- ✅ Pulsante "Salva impostazioni"
- ✅ Visualizzazione utilizzo giornaliero corrente

## 🔧 Logica di Blocco

### AccessibilityService/UsageStatsManager
- ✅ Monitora costantemente le app in foreground
- ✅ Per ogni app con blocco attivo, verifica:
  1. **Controllo orario**: Se ora corrente è tra "DA" e "A" → blocca apertura
  2. **Controllo tempo**: Se tempo utilizzo giornaliero > limite impostato → blocca/chiude app

### Quando Bloccare
- ✅ Mostra dialog/overlay con messaggio: "App bloccata. Orario non consentito" o "Tempo massimo raggiunto"
- ✅ Riporta utente alla home screen

## 🏗️ Architettura

### Componenti Tecnici
- **Database**: Room per salvare configurazioni (package_name, blocco_attivo, ora_da, ora_a, tempo_limite)
- **Service**: Foreground service per monitoraggio continuo
- **Permissions**: PACKAGE_USAGE_STATS, SYSTEM_ALERT_WINDOW, AccessibilityService
- **Architecture**: MVVM con ViewModel, LiveData, Repository

### Struttura del Progetto
```
app/
├── src/main/
│   ├── java/com/appcontrol/
│   │   ├── data/
│   │   │   ├── dao/
│   │   │   │   ├── AppSettingsDao.kt
│   │   │   │   └── AppUsageDao.kt
│   │   │   ├── database/
│   │   │   │   └── AppDatabase.kt
│   │   │   ├── model/
│   │   │   │   ├── AppInfo.kt
│   │   │   │   ├── AppSettings.kt
│   │   │   │   └── AppUsage.kt
│   │   │   └── repository/
│   │   │       └── AppRepository.kt
│   │   ├── service/
│   │   │   ├── AppBlockAccessibilityService.kt
│   │   │   ├── AppMonitorService.kt
│   │   │   └── BlockOverlayActivity.kt
│   │   ├── ui/
│   │   │   ├── adapter/
│   │   │   │   └── AppListAdapter.kt
│   │   │   ├── viewmodel/
│   │   │   │   ├── AppDetailViewModel.kt
│   │   │   │   └── MainViewModel.kt
│   │   │   ├── AppDetailActivity.kt
│   │   │   ├── MainActivity.kt
│   │   │   └── PermissionsActivity.kt
│   │   └── util/
│   │       ├── BlockChecker.kt
│   │       ├── PermissionHelper.kt
│   │       └── UsageTracker.kt
│   ├── res/
│   │   ├── drawable/
│   │   │   └── ic_notification.xml
│   │   ├── layout/
│   │   │   ├── activity_app_detail.xml
│   │   │   ├── activity_block_overlay.xml
│   │   │   ├── activity_main.xml
│   │   │   ├── activity_permissions.xml
│   │   │   └── item_app.xml
│   │   ├── values/
│   │   │   ├── colors.xml
│   │   │   ├── strings.xml
│   │   │   └── themes.xml
│   │   └── xml/
│   │       └── accessibility_service_config.xml
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

## 📋 Requisiti

- Android SDK 26+ (Android 8.0 Oreo)
- Target SDK 34 (Android 14)
- Kotlin 1.9.20
- Gradle 8.1.4

## 🚀 Come Compilare

1. Apri il progetto in Android Studio
2. Sincronizza Gradle: `File → Sync Project with Gradle Files`
3. Compila: `Build → Make Project`
4. Installa su dispositivo: `Run → Run 'app'`

Oppure da terminale:
```bash
# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

## ⚙️ Configurazione Permessi

Al primo avvio, l'app richiederà i seguenti permessi:

1. **Statistiche Utilizzo (Usage Stats)**
   - Vai in: Impostazioni → App → Accesso speciale → Statistiche utilizzo
   - Attiva AppControl

2. **Overlay Schermo (System Alert Window)**
   - Vai in: Impostazioni → App → Accesso speciale → Mostra sopra altre app
   - Attiva AppControl

3. **Servizio Accessibilità**
   - Vai in: Impostazioni → Accessibilità → AppControl
   - Attiva il servizio

## 📱 Come Usare

1. **Visualizza Lista App**: Nella schermata principale vedi tutte le app installate
2. **Cerca App**: Usa la barra di ricerca per filtrare
3. **Filtra per Stato**: Usa i chip "Tutte", "Bloccate", "Non bloccate"
4. **Configura Blocco**: Tap su un'app per aprire i dettagli
5. **Imposta Blocco**:
   - Attiva lo switch "Attiva blocco app"
   - Seleziona orario di blocco (DA - A)
   - Seleziona tempo massimo di utilizzo
   - Salva le impostazioni

## 🔒 Note di Sicurezza

- L'app stessa non può essere bloccata (protezione integrata)
- Il tracking del tempo si resetta automaticamente a mezzanotte
- I dati vecchi (>7 giorni) vengono rimossi automaticamente

## 🛠️ Dipendenze Principali

- AndroidX Core KTX
- Material Components
- Room Database
- Lifecycle Components
- Coroutines

## 📝 Note Implementazione

- Gestione permessi runtime completa
- Tracking tempo utilizzo giornaliero che si resetta a mezzanotte
- L'app stessa non può essere bloccabile
- Foreground service per monitoraggio continuo
- AccessibilityService per intercettare l'apertura delle app

## 🐛 Troubleshooting

**L'app non blocca le app:**
- Verifica che tutti i permessi siano attivi (vai in PermissionsActivity)
- Assicurati che il Servizio Accessibilità sia attivo
- Riavvia l'app dopo aver dato i permessi

**Il tempo di utilizzo non viene tracciato:**
- Verifica il permesso "Statistiche Utilizzo"
- Controlla che il servizio in background sia in esecuzione

## 📄 Licenza

Progetto educativo - Uso libero

---

**Sviluppato con ❤️ per aiutare a ridurre le distrazioni digitali**


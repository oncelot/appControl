# 🏗️ ARCHITETTURA APPCONTROL

## 📐 Pattern Architetturale: MVVM + Repository

```
┌─────────────────────────────────────────────────────────┐
│                     UI LAYER                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │  MainActivity │  │ AppDetail    │  │ Permissions  │ │
│  │              │  │ Activity     │  │ Activity     │ │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘ │
│         │                  │                            │
│  ┌──────▼───────┐  ┌──────▼───────┐                   │
│  │ MainViewModel│  │ DetailViewModel                   │
│  │              │  │              │                    │
│  └──────┬───────┘  └──────┬───────┘                   │
└─────────┼──────────────────┼────────────────────────────┘
          │                  │
          │   LiveData       │
          │   Flow          │
          │                  │
┌─────────▼──────────────────▼────────────────────────────┐
│                  REPOSITORY LAYER                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │            AppRepository                         │  │
│  │  • getAllInstalledApps()                        │  │
│  │  • getAppSettings()                             │  │
│  │  • getTodayUsage()                              │  │
│  │  • updateUsageTime()                            │  │
│  └──────┬────────────────────────────┬──────────────┘  │
└─────────┼────────────────────────────┼─────────────────┘
          │                            │
┌─────────▼────────────┐    ┌─────────▼────────────────┐
│   DATA LAYER         │    │   ANDROID APIS           │
│  ┌────────────────┐  │    │  ┌──────────────────┐   │
│  │ Room Database  │  │    │  │ PackageManager   │   │
│  │                │  │    │  └──────────────────┘   │
│  │ ┌────────────┐ │  │    │  ┌──────────────────┐   │
│  │ │AppSettings │ │  │    │  │UsageStatsManager │   │
│  │ └────────────┘ │  │    │  └──────────────────┘   │
│  │ ┌────────────┐ │  │    └──────────────────────────┘
│  │ │ AppUsage   │ │  │
│  │ └────────────┘ │  │
│  └────────────────┘  │
└──────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   SERVICE LAYER                          │
│  ┌────────────────────┐  ┌─────────────────────────┐   │
│  │ AppMonitorService  │  │ AppBlockAccessibility   │   │
│  │ (Foreground)       │  │ Service                 │   │
│  │                    │  │                         │   │
│  │ • Track usage      │  │ • Detect app launch     │   │
│  │ • Every 5s check   │  │ • Block if needed       │   │
│  └────────┬───────────┘  └──────────┬──────────────┘   │
└───────────┼──────────────────────────┼──────────────────┘
            │                          │
    ┌───────▼────────┐        ┌───────▼────────┐
    │ UsageTracker   │        │ BlockChecker   │
    │                │        │                │
    │ • getCurrentApp│        │ • checkTime    │
    │ • trackUsage   │        │ • checkUsage   │
    └────────────────┘        └────────────────┘
```

## 🔄 Flusso Dati

### 1. Caricamento Lista App
```
MainActivity 
  → MainViewModel.loadApps()
    → AppRepository.getAllInstalledApps()
      → PackageManager (Android API)
      → AppSettingsDao (Database)
    → Return List<AppInfo>
  → Update RecyclerView via LiveData
```

### 2. Salvataggio Impostazioni
```
AppDetailActivity
  → User modifica settings
  → Click "Salva"
    → AppDetailViewModel.saveAppSettings()
      → AppRepository.insertOrUpdateAppSettings()
        → AppSettingsDao.insertOrUpdate()
          → Room Database
      → Success/Failure via LiveData
    → Show Toast + Finish Activity
```

### 3. Monitoraggio App (Background)
```
AppMonitorService (ogni 5 secondi)
  → UsageTracker.trackCurrentAppUsage()
    → UsageStatsManager.queryUsageStats()
    → Get current foreground app
    → Check if app is blocked
      → AppRepository.getAppSettings()
    → If blocked, update usage time
      → AppRepository.updateUsageTime()
        → AppUsageDao.updateUsageTime()
```

### 4. Blocco App (Real-time)
```
User tenta di aprire app bloccata
  → AccessibilityService.onAccessibilityEvent()
    → BlockChecker.shouldBlockApp()
      → Check time range
      → Check daily usage
      → Return BlockResult
    → If shouldBlock == true
      → Launch BlockOverlayActivity (popup)
      → Go to home screen
```

## 📊 Database Schema

### Tabella: app_settings
```sql
CREATE TABLE app_settings (
    packageName TEXT PRIMARY KEY,
    appName TEXT NOT NULL,
    isBlocked INTEGER DEFAULT 0,
    blockStartTime TEXT,
    blockEndTime TEXT,
    maxUsageMinutes INTEGER DEFAULT 30,
    lastResetDate TEXT
)
```

### Tabella: app_usage
```sql
CREATE TABLE app_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    packageName TEXT NOT NULL,
    date TEXT NOT NULL,
    totalUsageMillis INTEGER DEFAULT 0
)
```

## 🎯 Componenti Chiave

### Data Layer

#### AppSettings (Entity)
- Memorizza configurazione blocco per ogni app
- Package name come chiave primaria
- Flags: isBlocked, orari, limiti tempo

#### AppUsage (Entity)
- Traccia utilizzo giornaliero
- Chiave: packageName + date
- Valore: totalUsageMillis

#### AppRepository
- Single source of truth per i dati
- Combina Room Database + Android APIs
- Espone funzioni sospese (suspend) per operazioni async

### UI Layer

#### ViewModels
- Mantengono stato UI sopravvivendo a configuration changes
- Espongono LiveData per osservazione reattiva
- Gestiscono chiamate repository con coroutines

#### Activities
- UI pura con ViewBinding
- Osservano LiveData dai ViewModels
- Delegano business logic ai ViewModels

### Service Layer

#### AppMonitorService
- Foreground service persistente
- Polling ogni 5 secondi
- Traccia uso app tramite UsageTracker

#### AppBlockAccessibilityService
- Intercetta eventi apertura app
- Usa BlockChecker per decisioni
- Mostra overlay + va a home se blocco

### Utility Layer

#### BlockChecker
- Logica decisionale per blocco
- checkTimeBlock(): controlla orario
- checkUsageBlock(): controlla limite tempo

#### UsageTracker
- Interfaccia con UsageStatsManager
- Calcola tempo elapsed
- Aggiorna database via repository

#### PermissionHelper
- Verifica stato permessi
- Apre settings Android per configurazione
- Supporta tutti i permessi necessari

## 🔐 Gestione Permessi

### Runtime Permissions Flow
```
App Start
  → PermissionHelper.hasUsageStatsPermission()
  → PermissionHelper.hasOverlayPermission()
  → PermissionHelper.isAccessibilityServiceEnabled()
  → If any missing:
    → Show AlertDialog
    → Open PermissionsActivity
      → User clicks card
      → Open Android Settings
      → User grants permission
      → Return to app
    → Verify permissions granted
```

## ⚡ Performance

### Ottimizzazioni Implementate

1. **DiffUtil per RecyclerView**
   - Calcolo differenziale per aggiornamenti efficienti
   - Solo item modificati vengono ridisegnati

2. **Coroutines per IO**
   - Tutte le operazioni DB su Dispatchers.IO
   - UI rimane fluida durante operazioni lunghe

3. **Caching Repository**
   - LiveData cacheata per riduire query DB
   - Osservatori multipli condividono stesso stream

4. **Service Ottimizzato**
   - Polling 5s invece di continuo
   - Early return se app già vista
   - Max 10s per evitare conteggi errati

5. **Pulizia Automatica**
   - Record vecchi (>7 giorni) eliminati daily
   - Previene crescita infinita del database

## 🧪 Testing Strategy

### Unit Tests (da implementare)
- ViewModels: test logica business
- Repository: test con DB in-memory
- BlockChecker: test algoritmi decisione

### Integration Tests (da implementare)
- Room Database queries
- Repository + DAO interaction

### UI Tests (da implementare)
- Espresso per UI flow
- Test apertura activity
- Test blocco simulato

## 🔮 Future Enhancements

### Possibili Miglioramenti
1. **Analytics Dashboard**
   - Grafici utilizzo settimanale
   - Statistiche per categoria app
   - Trend temporali

2. **Smart Blocking**
   - ML per suggerire blocchi
   - Pattern usage detection
   - Blocco automatico intelligente

3. **Profiles**
   - Profili multipli (lavoro/casa/weekend)
   - Switch veloce tra profili
   - Scheduling automatico

4. **Export/Import**
   - Backup configurazioni
   - Sync cloud
   - Condivisione profili

5. **Gamification**
   - Achievements per obiettivi
   - Streak counter
   - Leaderboard tra amici

## 📏 Design Principles

### SOLID Principles
- ✅ **S**ingle Responsibility: Ogni classe ha un solo scopo
- ✅ **O**pen/Closed: Estensibile senza modifiche
- ✅ **L**iskov Substitution: Interfacce ben definite
- ✅ **I**nterface Segregation: Interfacce specifiche
- ✅ **D**ependency Inversion: Dipendenze via astrazione

### Clean Architecture
- ✅ Separation of Concerns
- ✅ Dependency Rule (inward)
- ✅ Testability
- ✅ Independence from frameworks

---

**Architettura robusta, scalabile e manutenibile! 🏗️**


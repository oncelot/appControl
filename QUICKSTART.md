# 🚀 QUICK START - AppControl

## 🎯 5 Passi per Avviare l'App

### Passo 1: Apri il Progetto
```
1. Apri Android Studio
2. File → Open
3. Seleziona: C:\Users\fferla\Documents\progetti\appControl
4. Click OK
```

### Passo 2: Attendi Sincronizzazione
```
- Gradle sincronizzerà automaticamente
- Attendi che appaia "Sync successful" in basso
- Tempo: 2-5 minuti (prima volta)
```

### Passo 3: Genera Icone (Opzionale)
```
1. Click destro su app/src/main/res
2. New → Image Asset
3. Icon Type: Launcher Icons
4. Scegli un clipart (es. blocco/lock)
5. Next → Finish
```

### Passo 4: Compila
```
Build → Make Project
Oppure: Ctrl+F9
```

### Passo 5: Esegui
```
Run → Run 'app'
Oppure: Shift+F10
```

## 📱 Primo Utilizzo

### Quando l'app si avvia:
1. Apparirà un popup "Permessi necessari"
2. Click su "Configura"
3. Attiva i 3 permessi uno per uno:
   - ✅ Statistiche utilizzo
   - ✅ Overlay schermo  
   - ✅ Servizio accessibilità
4. Torna all'app

### Blocca la tua prima app:
1. Cerca un'app (es. "Chrome")
2. Tap sull'app
3. Attiva "Blocco app"
4. Imposta orario: DA 09:00 A 18:00
5. Scegli tempo max: 30 minuti
6. Tap "Salva impostazioni"
7. Esci e prova ad aprire Chrome → Bloccata! ✅

## 🎮 Test Veloce

Per testare subito se funziona:
```
1. Blocca un'app (es. Calcolatrice)
2. Imposta orario: DA 00:00 A 23:59
3. Salva
4. Esci dall'app
5. Apri Calcolatrice → Dovrebbe chiudersi subito!
```

## ❓ Problemi Comuni

### "SDK not found"
```
Crea: C:\Users\fferla\Documents\progetti\appControl\local.properties
Scrivi: sdk.dir=C\:\\Users\\fferla\\AppData\\Local\\Android\\Sdk
```

### "Gradle sync failed"
```
File → Invalidate Caches → Invalidate and Restart
```

### L'app non blocca
```
Verifica che i 3 permessi siano TUTTI attivi:
- Impostazioni → App → AppControl → Permessi
```

## 📚 Documenti da Leggere

- **PROJECT_SUMMARY.md** ← Inizia da qui!
- **README.md** - Panoramica completa
- **INSTRUCTIONS.md** - Guida dettagliata
- **CHECKLIST.md** - Verifica completezza

## 🎉 Fatto!

Ora hai un'app completa che blocca le app per evitare distrazioni!

**Buon divertimento! 🚀**


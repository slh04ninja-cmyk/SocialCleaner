# 🧹 Social Cleaner

Application Android pour analyser et nettoyer les données des réseaux sociaux.

## ✨ Fonctionnalités

- **Scan intelligent** des données de 8+ applications sociales
- **Analyse par année** avec détails par catégorie
- **Interface moderne** avec thème sombre
- **Sélection granulaire** par catégorie ou par app
- **Suppression sécurisée** avec confirmation
- **Support multi-apps** : WhatsApp, Telegram, Facebook, Instagram, Snapchat, TikTok, Messenger, Signal

## 📱 Applications supportées

| Application | Images | Vidéos | Documents | Audio | Stickers |
|-------------|--------|--------|-----------|-------|----------|
| WhatsApp | ✅ | ✅ | ✅ | ✅ | ✅ |
| Telegram | ✅ | ✅ | ✅ | ✅ | ✅ |
| Facebook | ✅ | ✅ | ✅ | ❌ | ❌ |
| Instagram | ✅ | ✅ | ❌ | ❌ | ❌ |
| Snapchat | ✅ | ✅ | ❌ | ❌ | ❌ |
| TikTok | ✅ | ✅ | ❌ | ❌ | ❌ |
| Messenger | ✅ | ✅ | ❌ | ✅ | ❌ |
| Signal | ✅ | ✅ | ✅ | ✅ | ❌ |

## 🏗️ Architecture

```
com.socialcleaner/
├── MainActivity.kt          # Activité principale
├── model/
│   └── DataModels.kt        # Modèles de données
├── scanner/
│   └── MediaScanner.kt      # Moteur de scan
└── ui/
    ├── AppResultAdapter.kt   # Liste des résultats par app
    └── YearAdapter.kt        # Liste groupée par année
```

## 🛠️ Technologies

- **Kotlin** - Langage principal
- **Android Jetpack** - Composants modernes
- **Material Design 3** - Interface utilisateur
- **Coroutines** - Traitement asynchrone
- **RecyclerView** - Listes performantes

## 📦 Installation

### Prérequis
- Android Studio Hedgehog (2023.1.1) ou supérieur
- Android SDK 34
- Kotlin 1.9.22

### Compilation
```bash
# Cloner le projet
git clone <url-du-depot>
cd SocialCleaner

# Compiler l'APK
./gradlew assembleDebug

# L'APK sera dans app/build/outputs/apk/debug/
```

### Installation directe
```bash
# Installer sur un appareil connecté
./gradlew installDebug
```

## 🔐 Permissions

L'application nécessite :
- `READ_EXTERNAL_STORAGE` - Lecture des fichiers
- `WRITE_EXTERNAL_STORAGE` - Suppression des fichiers
- `MANAGE_EXTERNAL_STORAGE` - Accès complet (Android 11+)

## 🎨 Design

- **Thème sombre** avec accents rouge (#e94560)
- **Cartes arrondies** avec ombres subtiles
- **Animations fluides** pour l'expansion des sections
- **Icônes vectorielles** pour chaque application

## 📊 Fonctionnement

1. **Sélection de l'année** : L'utilisateur choisit une année spécifique ou "Toutes"
2. **Scan** : L'application parcourt les dossiers connus de chaque app
3. **Catégorisation** : Les fichiers sont classés par type (images, vidéos, etc.)
4. **Affichage** : Résultats groupés par année puis par application
5. **Sélection** : L'utilisateur coche les catégories/apps à supprimer
6. **Suppression** : Confirmation puis suppression définitive

## ⚠️ Avertissements

- La suppression est **irréversible**
- Certains fichiers peuvent être protégés par le système
- Les apps comme WhatsApp peuvent recréer les dossiers
- Toujours **vérifier la sélection** avant suppression

## 🔧 Personnalisation

### Ajouter une nouvelle app
Dans `MediaScanner.kt`, ajoutez une entrée à `AppRegistry.supportedApps` :

```kotlin
SocialApp(
    name = "NouvelleApp",
    icon = "nouvelle_app",
    packageName = "com.nouvelle.app",
    mediaPaths = listOf("Android/data/com.nouvelle.app/files"),
    categories = mapOf(
        "Images" to listOf("jpg", "png"),
        "Vidéos" to listOf("mp4")
    )
)
```

### Modifier le thème
Éditez `res/values/themes.xml` pour changer les couleurs.

## 📝 Licence

Ce projet est open-source. Utilisez-le comme vous le souhaitez.

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :
- Signaler des bugs
- Proposer des améliorations
- Ajouter le support pour de nouvelles apps

---

**Développé avec ❤️ pour simplifier le nettoyage des données sociales**

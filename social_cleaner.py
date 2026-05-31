#!/usr/bin/env python3
"""
🧹 Social Cleaner v2.0 - Version corrigée
Scan et nettoyage SÉCURISÉ des données des réseaux sociaux
"""

import os
import sys
import time
from datetime import datetime
from pathlib import Path
from collections import defaultdict

# Configuration des apps sociales
SOCIAL_APPS = {
    "WhatsApp": {
        "icon": "📱",
        "paths": [
            "Android/media/com.whatsapp/WhatsApp/Media",
            "WhatsApp/Media"
        ],
        "categories": {
            "Images": ["jpg", "jpeg", "png", "webp", "gif"],
            "Vidéos": ["mp4", "3gp", "mkv", "avi"],
            "Documents": ["pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "zip"],
            "Voice Notes": ["opus", "ogg", "m4a"],
            "Audio": ["mp3", "aac", "wav"],
            "Stickers": ["webp"],
            "Vidéo Notes": ["mp4"],
            "GIFs": ["gif", "mp4"]
        }
    },
    "Telegram": {
        "icon": "✈️",
        "paths": [
            "Android/data/org.telegram.messenger/files/Telegram",
            "Telegram"
        ],
        "categories": {
            "Images": ["jpg", "jpeg", "png", "webp", "gif"],
            "Vidéos": ["mp4", "mkv", "avi"],
            "Documents": ["pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip"],
            "Audio": ["mp3", "ogg", "m4a", "opus"],
            "Voice Notes": ["opus", "ogg"],
            "Stickers": ["webp", "tgs"],
            "GIFs": ["mp4", "gif"]
        }
    },
    "Facebook": {
        "icon": "👤",
        "paths": [
            "Android/data/com.facebook.katana/files",
            "Pictures/Facebook"
        ],
        "categories": {
            "Images": ["jpg", "jpeg", "png", "webp"],
            "Vidéos": ["mp4", "3gp"],
            "Documents": ["pdf", "doc", "docx"]
        }
    },
    "Instagram": {
        "icon": "📸",
        "paths": [
            "Android/data/com.instagram.android/files",
            "Pictures/Instagram",
            "DCIM/Instagram"
        ],
        "categories": {
            "Images": ["jpg", "jpeg", "png", "webp"],
            "Vidéos": ["mp4", "mkv"]
        }
    },
    "Snapchat": {
        "icon": "👻",
        "paths": [
            "Android/data/com.snapchat.android/files",
            "Snapchat",
            "DCIM/Snapchat"
        ],
        "categories": {
            "Images": ["jpg", "jpeg", "png", "webp"],
            "Vidéos": ["mp4", "mkv"]
        }
    },
    "TikTok": {
        "icon": "🎵",
        "paths": [
            "Android/data/com.zhiliaoapp.musically/files",
            "Movies/TikTok",
            "DCIM/TikTok"
        ],
        "categories": {
            "Images": ["jpg", "jpeg", "png", "webp"],
            "Vidéos": ["mp4", "mkv"]
        }
    },
    "Messenger": {
        "icon": "💬",
        "paths": [
            "Android/data/com.facebook.orca/files",
            "Pictures/Messenger"
        ],
        "categories": {
            "Images": ["jpg", "jpeg", "png", "webp", "gif"],
            "Vidéos": ["mp4", "3gp"],
            "Audio": ["mp3", "ogg", "m4a"],
            "Voice Notes": ["opus", "ogg"]
        }
    },
    "Signal": {
        "icon": "🔒",
        "paths": [
            "Android/data/org.thoughtcrime.securesms/files"
        ],
        "categories": {
            "Images": ["jpg", "jpeg", "png", "webp"],
            "Vidéos": ["mp4"],
            "Documents": ["pdf", "doc", "docx"],
            "Audio": ["mp3", "ogg", "m4a"]
        }
    }
}

def format_size(bytes_size):
    """Formater la taille en Ko, Mo, Go"""
    if bytes_size < 1024:
        return f"{bytes_size} o"
    elif bytes_size < 1024 * 1024:
        return f"{bytes_size / 1024:.1f} Ko"
    elif bytes_size < 1024 * 1024 * 1024:
        return f"{bytes_size / (1024 * 1024):.1f} Mo"
    else:
        return f"{bytes_size / (1024 * 1024 * 1024):.2f} Go"

def get_file_year(filepath):
    """Obtenir l'année de modification du fichier"""
    try:
        mtime = os.path.getmtime(filepath)
        return datetime.fromtimestamp(mtime).year
    except:
        return None

def scan_directory(base_path, categories, target_year=None):
    """Scanner un répertoire et catégoriser les fichiers"""
    results = defaultdict(lambda: defaultdict(list))
    
    if not os.path.exists(base_path):
        return results
    
    all_extensions = {}
    for cat_name, extensions in categories.items():
        for ext in extensions:
            all_extensions[ext.lower()] = cat_name
    
    for root, dirs, files in os.walk(base_path):
        for filename in files:
            filepath = os.path.join(root, filename)
            ext = filename.rsplit('.', 1)[-1].lower() if '.' in filename else ''
            
            year = get_file_year(filepath)
            if year is None:
                continue
            
            # FILTRE PAR ANNÉE - CRITIQUE !
            if target_year and year != target_year:
                continue
            
            try:
                size = os.path.getsize(filepath)
            except:
                size = 0
            
            category = all_extensions.get(ext, "Autres")
            results[year][category].append({
                "path": filepath,
                "name": filename,
                "size": size
            })
    
    return results

def scan_app(app_name, app_config, target_year=None):
    """Scanner une application avec filtre année"""
    base_paths = [
        "/storage/emulated/0",
        os.path.expanduser("~/storage/shared")
    ]
    
    all_results = defaultdict(lambda: defaultdict(list))
    
    for media_path in app_config["paths"]:
        for base in base_paths:
            full_path = os.path.join(base, media_path)
            results = scan_directory(full_path, app_config["categories"], target_year)
            
            for year, categories in results.items():
                for cat_name, files in categories.items():
                    all_results[year][cat_name].extend(files)
    
    return all_results

def clear_screen():
    """Effacer l'écran"""
    os.system('clear' if os.name == 'posix' else 'cls')

def print_header():
    """Afficher l'en-tête"""
    print("\033[1;35m" + "=" * 60)
    print("  🧹 SOCIAL CLEANER v2.0 - Version Sécurisée")
    print("=" * 60 + "\033[0m")
    print()

def print_apps_menu():
    """Afficher le menu des applications"""
    print("\033[1;36m📱 Applications disponibles :\033[0m")
    print()
    print("  0. 🔍 Scanner TOUTES les apps")
    apps = list(SOCIAL_APPS.items())
    for i, (name, config) in enumerate(apps, 1):
        print(f"  {i}. {config['icon']} {name}")
    print()
    print(f"  99. ❌ Quitter")
    print()

def get_year_selection():
    """Demander l'année à scanner"""
    current_year = datetime.now().year
    print("\033[1;36m📅 Sélectionnez l'année :\033[0m")
    print()
    print("  0. Toutes les années")
    for i, year in enumerate(range(current_year, 2017, -1), 1):
        print(f"  {i}. {year}")
    print()
    
    choice = input("\033[1;33m  Votre choix (année): \033[0m").strip()
    
    if choice == '0':
        return None  # Toutes les années
    
    try:
        idx = int(choice) - 1
        years = list(range(current_year, 2017, -1))
        if 0 <= idx < len(years):
            return years[idx]
    except:
        pass
    
    print("\033[1;31m  Choix invalide! Scan de toutes les années.\033[0m")
    time.sleep(1)
    return None

def display_results(results, app_name, target_year):
    """Afficher les résultats du scan avec contexte clair"""
    if not results:
        print("\033[1;33m  Aucune donnée trouvée.\033[0m")
        return None, 0, 0
    
    total_files = 0
    total_size = 0
    selections = {}
    
    year_text = str(target_year) if target_year else "Toutes les années"
    print(f"\n\033[1;33m📊 Résultats pour {app_name} - {year_text}\033[0m")
    print()
    
    for year in sorted(results.keys(), reverse=True):
        categories = results[year]
        year_files = sum(len(files) for files in categories.values())
        year_size = sum(f["size"] for files in categories.values() for f in files)
        
        print(f"\n\033[1;33m📅 {year}\033[0m")
        print(f"   Total: {year_files} fichiers • {format_size(year_size)}")
        print()
        
        for cat_name in sorted(categories.keys()):
            files = categories[cat_name]
            if not files:
                continue
            
            cat_size = sum(f["size"] for f in files)
            emoji = {
                "Images": "🖼️",
                "Vidéos": "🎬",
                "Documents": "📄",
                "Voice Notes": "🎤",
                "Audio": "🎵",
                "Stickers": "😀",
                "Vidéo Notes": "📹",
                "GIFs": "🎞️",
                "Autres": "📁"
            }.get(cat_name, "📁")
            
            key = f"{year}_{cat_name}"
            selections[key] = {"files": files, "selected": False}
            
            print(f"   {emoji} {cat_name}")
            print(f"      Fichiers: {len(files)}")
            print(f"      Taille: {format_size(cat_size)}")
            
            total_files += len(files)
            total_size += cat_size
    
    print()
    print("\033[1;32m" + "=" * 40)
    print(f"  TOTAL: {total_files} fichiers • {format_size(total_size)}")
    print("=" * 40 + "\033[0m")
    
    return selections, total_files, total_size

def get_selection(selections):
    """Demander à l'utilisateur ce qu'il veut supprimer"""
    print()
    print("\033[1;36m📋 Sélectionnez les éléments à supprimer :\033[0m")
    print()
    print("  Entrez les numéros séparés par des virgules")
    print("  Exemple: 1,3,5")
    print("  Ou 'all' pour tout sélectionner")
    print("  Ou 'q' pour annuler")
    print()
    
    items = list(selections.keys())
    for i, key in enumerate(items, 1):
        year, cat = key.split('_', 1)
        print(f"  {i}. {year} - {cat}")
    
    print()
    choice = input("\033[1;33m  Votre choix: \033[0m").strip()
    
    if choice.lower() == 'q':
        return []
    
    if choice.lower() == 'all':
        return items
    
    selected = []
    try:
        indices = [int(x.strip()) - 1 for x in choice.split(',')]
        for idx in indices:
            if 0 <= idx < len(items):
                selected.append(items[idx])
    except:
        print("\033[1;31m  Choix invalide!\033[0m")
        return []
    
    return selected

def confirm_deletion(selections, selected_keys, app_name):
    """Confirmer la suppression avec détails clairs"""
    print()
    print("\033[1;31m" + "=" * 50)
    print("  ⚠️  CONFIRMATION DE SUPPRESSION")
    print("=" * 50 + "\033[0m")
    print()
    print(f"  Application: \033[1;33m{app_name}\033[0m")
    print()
    print("  Éléments à supprimer :")
    
    total_files = 0
    total_size = 0
    
    for key in selected_keys:
        if key in selections:
            year, cat = key.split('_', 1)
            files = selections[key]["files"]
            size = sum(f["size"] for f in files)
            print(f"    • {year} - {cat}: {len(files)} fichiers ({format_size(size)})")
            total_files += len(files)
            total_size += size
    
    print()
    print(f"  \033[1;31mTOTAL: {total_files} fichiers • {format_size(total_size)}\033[0m")
    print()
    print("  \033[1;31m⚠️  Cette action est IRRÉVERSIBLE!\033[0m")
    print()
    
    confirm = input("  \033[1;33mTapez 'SUPPRIMER' pour confirmer: \033[0m").strip()
    return confirm == "SUPPRIMER"

def delete_files(selections, selected_keys):
    """Supprimer les fichiers sélectionnés"""
    deleted_count = 0
    deleted_size = 0
    errors = 0
    
    print()
    print("\033[1;31m🗑️  Suppression en cours...\033[0m")
    print()
    
    for key in selected_keys:
        if key not in selections:
            continue
        
        files = selections[key]["files"]
        year, cat = key.split('_', 1)
        
        print(f"  Suppression {year} - {cat} ({len(files)} fichiers)...")
        
        for file_info in files:
            try:
                filepath = file_info["path"]
                if os.path.exists(filepath):
                    os.remove(filepath)
                    deleted_count += 1
                    deleted_size += file_info["size"]
            except Exception as e:
                errors += 1
    
    print()
    print("\033[1;32m" + "=" * 40)
    print(f"  ✅ SUPPRESSION TERMINÉE")
    print(f"  Fichiers supprimés: {deleted_count}")
    print(f"  Espace libéré: {format_size(deleted_size)}")
    if errors > 0:
        print(f"  ⚠️  Erreurs: {errors}")
    print("=" * 40 + "\033[0m")

def main():
    """Fonction principale"""
    clear_screen()
    print_header()
    
    # Vérifier l'accès au stockage
    test_path = "/storage/emulated/0"
    if not os.path.exists(test_path):
        print("\033[1;31m❌ Accès au stockage impossible!\033[0m")
        print("   Exécutez: termux-setup-storage")
        print()
        input("Appuyez sur Entrée pour quitter...")
        return
    
    while True:
        clear_screen()
        print_header()
        print_apps_menu()
        
        choice = input("\033[1;33m  Sélectionnez une app (0-8): \033[0m").strip()
        
        if choice == '99':
            print("\n  Au revoir! 👋\n")
            break
        
        # Déterminer les apps à scanner
        if choice == '0':
            apps_to_scan = SOCIAL_APPS
            app_name = "Toutes les apps"
        else:
            try:
                idx = int(choice) - 1
                if 0 <= idx < len(SOCIAL_APPS):
                    app_name = list(SOCIAL_APPS.keys())[idx]
                    apps_to_scan = {app_name: SOCIAL_APPS[app_name]}
                else:
                    print("\033[1;31m  Choix invalide!\033[0m")
                    time.sleep(1)
                    continue
            except:
                print("\033[1;31m  Choix invalide!\033[0m")
                time.sleep(1)
                continue
        
        # Sélection de l'année
        clear_screen()
        print_header()
        target_year = get_year_selection()
        
        # Scanner
        clear_screen()
        print_header()
        year_text = str(target_year) if target_year else "Toutes les années"
        print(f"\033[1;33m🔍 Scan de {app_name} pour {year_text}...\033[0m")
        print()
        
        all_results = defaultdict(lambda: defaultdict(list))
        
        for name, config in apps_to_scan.items():
            print(f"  Scan {config['icon']} {name}...", end="", flush=True)
            results = scan_app(name, config, target_year)
            
            for year, categories in results.items():
                for cat_name, files in categories.items():
                    all_results[year][cat_name].extend(files)
            
            print(" ✅")
        
        # Afficher les résultats
        clear_screen()
        print_header()
        
        if not all_results:
            print(f"\033[1;33m  Aucune donnée trouvée pour {app_name} en {year_text}.\033[0m")
            print()
            input("  Appuyez sur Entrée pour continuer...")
            continue
        
        selections, total_files, total_size = display_results(all_results, app_name, target_year)
        
        if total_files == 0:
            print()
            input("  Appuyez sur Entrée pour continuer...")
            continue
        
        # Menu de suppression
        print()
        print("\033[1;36m  1. 🗑️  Supprimer des fichiers")
        print("  2. 🔍 Nouveau scan")
        print("  3. ❌ Retour\033[0m")
        print()
        
        action = input("\033[1;33m  Votre choix: \033[0m").strip()
        
        if action == '1':
            selected_keys = get_selection(selections)
            if selected_keys:
                # Double confirmation
                if confirm_deletion(selections, selected_keys, app_name):
                    delete_files(selections, selected_keys)
                else:
                    print("\n  \033[1;33mSuppression annulée.\033[0m")
        
        print()
        input("  Appuyez sur Entrée pour continuer...")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n  Au revoir! 👋\n")
        sys.exit(0)

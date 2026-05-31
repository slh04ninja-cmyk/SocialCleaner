#!/usr/bin/env python3
"""
🧹 Social Cleaner - Version Termux
Scan et nettoyage des données des réseaux sociaux
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

def scan_directory(base_path, categories):
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
    """Scanner une application"""
    base_paths = [
        "/storage/emulated/0",
        os.path.expanduser("~/storage/shared")
    ]
    
    all_results = defaultdict(lambda: defaultdict(list))
    
    for media_path in app_config["paths"]:
        for base in base_paths:
            full_path = os.path.join(base, media_path)
            results = scan_directory(full_path, app_config["categories"])
            
            for year, categories in results.items():
                if target_year and year != target_year:
                    continue
                for cat_name, files in categories.items():
                    all_results[year][cat_name].extend(files)
    
    return all_results

def clear_screen():
    """Effacer l'écran"""
    os.system('clear' if os.name == 'posix' else 'cls')

def print_header():
    """Afficher l'en-tête"""
    print("\033[1;35m" + "=" * 60)
    print("  🧹 SOCIAL CLEANER - Nettoyage Réseaux Sociaux")
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

def print_year_menu(years):
    """Afficher le menu des années"""
    print("\033[1;36m📅 Sélectionnez l'année :\033[0m")
    print()
    print("  0. Toutes les années")
    for i, year in enumerate(sorted(years, reverse=True), 1):
        print(f"  {i}. {year}")
    print()

def display_results(results, app_name=None):
    """Afficher les résultats du scan"""
    if not results:
        print("\033[1;33m  Aucune donnée trouvée.\033[0m")
        return
    
    total_files = 0
    total_size = 0
    selections = {}
    
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
        
        # Scanner
        clear_screen()
        print_header()
        print("\033[1;33m🔍 Scan en cours...\033[0m")
        print()
        
        all_results = defaultdict(lambda: defaultdict(list))
        
        for app_name, app_config in apps_to_scan.items():
            print(f"  Scan {app_config['icon']} {app_name}...", end="", flush=True)
            results = scan_app(app_name, app_config)
            
            for year, categories in results.items():
                for cat_name, files in categories.items():
                    all_results[year][cat_name].extend(files)
            
            print(" ✅")
        
        # Afficher les résultats
        clear_screen()
        print_header()
        
        if not all_results:
            print("\033[1;33m  Aucune donnée trouvée pour ces apps.\033[0m")
            print()
            input("  Appuyez sur Entrée pour continuer...")
            continue
        
        selections, total_files, total_size = display_results(all_results)
        
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
                # Calculer la taille totale à supprimer
                total_delete_size = 0
                total_delete_count = 0
                for key in selected_keys:
                    if key in selections:
                        for f in selections[key]["files"]:
                            total_delete_size += f["size"]
                            total_delete_count += 1
                
                print()
                print(f"\033[1;31m  ⚠️  Vous allez supprimer {total_delete_count} fichiers ({format_size(total_delete_size)})\033[0m")
                print(f"\033[1;31m  Cette action est IRRÉVERSIBLE!\033[0m")
                print()
                confirm = input("\033[1;33m  Confirmer (oui/non): \033[0m").strip()
                
                if confirm.lower() in ['oui', 'o', 'yes', 'y']:
                    delete_files(selections, selected_keys)
                else:
                    print("\n  Suppression annulée.")
        
        print()
        input("  Appuyez sur Entrée pour continuer...")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n  Au revoir! 👋\n")
        sys.exit(0)

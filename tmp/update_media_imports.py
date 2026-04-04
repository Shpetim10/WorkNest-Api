import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

media_imports = [
    ("MediaController", "com.worknest.auth.controller", "com.worknest.features.media.web"),
    ("MediaStorageService", "com.worknest.auth.service", "com.worknest.features.media.application"),
    ("LocalMediaStorageServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.media.application"),
    ("MediaUploadResponse", "com.worknest.auth.dto", "com.worknest.features.media.dto"),
]

def update_media_imports(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                for cls, old_pkg, new_pkg in media_imports:
                    old_import = f"import {old_pkg}.{cls};"
                    new_import = f"import {new_pkg}.{cls};"
                    new_content = new_content.replace(old_import, new_import)
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated imports in {filename}")

update_media_imports(os.path.join(base, "src"))

import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

replacements = [
    ("src/main/java/com/worknest/features/media/web", "com.worknest.auth.controller", "com.worknest.features.media.web"),
    ("src/main/java/com/worknest/features/media/application", "com.worknest.auth.service", "com.worknest.features.media.application"),
    ("src/main/java/com/worknest/features/media/application", "com.worknest.auth.service.impl", "com.worknest.features.media.application"),
    ("src/main/java/com/worknest/features/media/dto", "com.worknest.auth.dto", "com.worknest.features.media.dto"),
]

def update_packages(directory, old_pkg, new_pkg):
    dir_path = os.path.join(base, directory.replace("/", "\\"))
    if not os.path.exists(dir_path):
        return
    for filename in os.listdir(dir_path):
        if filename.endswith(".java"):
            filepath = os.path.join(dir_path, filename)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            new_content = content.replace(f"package {old_pkg};", f"package {new_pkg};")
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"Updated package in {filename}")

for directory, old_pkg, new_pkg in replacements:
    update_packages(directory, old_pkg, new_pkg)

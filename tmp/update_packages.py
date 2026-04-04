import os

def replace_in_dir(directory, old_pkg, new_pkg):
    if not os.path.exists(directory):
        print(f"Directory {directory} does not exist")
        return
    for filename in os.listdir(directory):
        if filename.endswith(".java"):
            filepath = os.path.join(directory, filename)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            new_content = content.replace(f"package {old_pkg};", f"package {new_pkg};")
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f"Updated {filename}")

base_path = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"
replace_in_dir(os.path.join(base_path, "src/main/java/com/worknest/domain/entities"), "com.worknest.auth.domain", "com.worknest.domain.entities")
replace_in_dir(os.path.join(base_path, "src/main/java/com/worknest/domain/enums"), "com.worknest.auth.domain", "com.worknest.domain.enums")

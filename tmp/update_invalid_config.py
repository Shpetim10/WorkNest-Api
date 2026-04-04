import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

# Update package declaration in InvalidConfigurationRequestException
filepath = os.path.join(base, "src\\main\\java\\com\\worknest\\features\\media\\exception\\InvalidConfigurationRequestException.java")
if os.path.exists(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = content.replace("package com.worknest.auth.exception;", "package com.worknest.features.media.exception;")
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print("Updated package in InvalidConfigurationRequestException.java")

# Update imports in all files
def update_invalid_config_imports(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                new_content = new_content.replace("import com.worknest.auth.exception.InvalidConfigurationRequestException;", "import com.worknest.features.media.exception.InvalidConfigurationRequestException;")
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated imports in {filename}")

update_invalid_config_imports(os.path.join(base, "src"))

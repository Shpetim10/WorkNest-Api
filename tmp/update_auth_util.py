import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

# Update package declaration in auth utility files
dir_path = os.path.join(base, "src\\main\\java\\com\\worknest\\features\\auth\\utility")
files = ["SecureTokenGenerator.java", "Sha256TokenHashUtility.java"]

for filename in files:
    filepath = os.path.join(dir_path, filename)
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        new_content = content.replace("package com.worknest.auth.utility;", "package com.worknest.features.auth.utility;")
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated package in {filename}")

# Update imports in all files
def update_auth_util_imports(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                new_content = new_content.replace("import com.worknest.auth.utility.SecureTokenGenerator;", "import com.worknest.features.auth.utility.SecureTokenGenerator;")
                new_content = new_content.replace("import com.worknest.auth.utility.Sha256TokenHashUtility;", "import com.worknest.features.auth.utility.Sha256TokenHashUtility;")
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated imports in {filename}")

update_auth_util_imports(os.path.join(base, "src"))

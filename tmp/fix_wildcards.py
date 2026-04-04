import os

def fix_wildcards(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                old_wildcard = "import com.worknest.auth.domain.*;"
                if old_wildcard in new_content:
                    new_wildcards = "import com.worknest.domain.entities.*;\nimport com.worknest.domain.enums.*;"
                    new_content = new_content.replace(old_wildcard, new_wildcards)
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Fixed wildcard in {filename}")

base_path = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"
fix_wildcards(os.path.join(base_path, "src"))

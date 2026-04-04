import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"
entities_dir = os.path.join(base, "src\\main\\java\\com\\worknest\\domain\\entities")

def add_enum_imports(directory):
    if not os.path.exists(directory):
        return
    for filename in os.listdir(directory):
        if filename.endswith(".java"):
            filepath = os.path.join(directory, filename)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if "import com.worknest.domain.enums.*;" in content:
                continue
                
            lines = content.splitlines()
            new_lines = []
            import_added = False
            for line in lines:
                new_lines.append(line)
                if line.startswith("package com.worknest.domain.entities;") and not import_added:
                    new_lines.append("")
                    new_lines.append("import com.worknest.domain.enums.*;")
                    import_added = True
            
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write("\n".join(new_lines) + "\n")
            print(f"Added enum wildcard import to {filename}")

add_enum_imports(entities_dir)

import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

# Update package declaration in InvitationEmail files
dir_path = os.path.join(base, "src\\main\\java\\com\\worknest\\features\\notification\\email\\service")
files = ["InvitationEmailService.java", "InvitationEmailServiceImpl.java"]

for filename in files:
    filepath = os.path.join(dir_path, filename)
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        new_content = content.replace("package com.worknest.auth.service;", "package com.worknest.features.notification.email.service;")
        new_content = new_content.replace("package com.worknest.auth.service.impl;", "package com.worknest.features.notification.email.service;")
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated package in {filename}")

# Update imports in all files
def update_inv_email_imports(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                new_content = new_content.replace("import com.worknest.auth.service.InvitationEmailService;", "import com.worknest.features.notification.email.service.InvitationEmailService;")
                new_content = new_content.replace("import com.worknest.auth.service.impl.InvitationEmailServiceImpl;", "import com.worknest.features.notification.email.service.InvitationEmailServiceImpl;")
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated imports in {filename}")

update_inv_email_imports(os.path.join(base, "src"))

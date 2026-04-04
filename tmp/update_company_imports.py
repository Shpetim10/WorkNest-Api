import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

company_imports = [
    ("CompanyRegistrationController", "com.worknest.auth.controller", "com.worknest.features.company.web"),
    ("CompanyRegistrationService", "com.worknest.auth.service", "com.worknest.features.company.application"),
    ("CompanyRegistrationServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.company.application"),
    ("CompanyRepository", "com.worknest.auth.repository", "com.worknest.features.company.repository"),
    ("CompanySiteRepository", "com.worknest.auth.repository", "com.worknest.features.company.repository"),
    ("SiteTrustedNetworkRepository", "com.worknest.auth.repository", "com.worknest.features.company.repository"),
    ("CompanyRegistrationRequest", "com.worknest.auth.dto", "com.worknest.features.company.dto"),
    ("CompanyRegistrationResponse", "com.worknest.auth.dto", "com.worknest.features.company.dto"),
    ("CompanySiteResponse", "com.worknest.auth.dto", "com.worknest.features.company.dto"),
    ("CreateCompanySiteRequest", "com.worknest.auth.dto", "com.worknest.features.company.dto"),
    ("CreateTrustedNetworkRequest", "com.worknest.auth.dto", "com.worknest.features.company.dto"),
    ("SiteTrustedNetworkResponse", "com.worknest.auth.dto", "com.worknest.features.company.dto"),
    ("AdminEmailAlreadyExistsException", "com.worknest.auth.exception", "com.worknest.features.company.exception"),
    ("CompanyAlreadyExistsException", "com.worknest.auth.exception", "com.worknest.features.company.exception"),
    ("CompanySlugAlreadyExistsException", "com.worknest.auth.exception", "com.worknest.features.company.exception"),
    ("InvalidRegistrationDataException", "com.worknest.auth.exception", "com.worknest.features.company.exception"),
]

def update_company_imports(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                for cls, old_pkg, new_pkg in company_imports:
                    old_import = f"import {old_pkg}.{cls};"
                    new_import = f"import {new_pkg}.{cls};"
                    new_content = new_content.replace(old_import, new_import)
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated imports in {filename}")

update_company_imports(os.path.join(base, "src"))

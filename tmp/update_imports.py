import os

entities = [
    "Company", "CompanySite", "PasswordResetToken", "Permission", "RefreshToken",
    "RoleAssignment", "RoleAssignmentPermission", "SiteTrustedNetwork", "User", "UserInvitation"
]

enums = [
    "CompanyStatus", "GeofenceShapeType", "InvitationKind", "MediaCategory", "NetworkIpVersion",
    "NetworkType", "PlatformAccess", "PlatformRole", "SiteStatus", "SiteType",
    "SubscriptionPlan", "SubscriptionStatus", "UserStatus"
]

def update_imports(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                for entity in entities:
                    old_import = f"import com.worknest.auth.domain.{entity};"
                    new_import = f"import com.worknest.domain.entities.{entity};"
                    new_content = new_content.replace(old_import, new_import)
                
                for enum in enums:
                    old_import = f"import com.worknest.auth.domain.{enum};"
                    new_import = f"import com.worknest.domain.enums.{enum};"
                    new_content = new_content.replace(old_import, new_import)
                
                # Also handle wildcard imports if any (though discouraged)
                # old_wildcard = "import com.worknest.auth.domain.*;"
                # This is tricky because we split them. Let's assume no wildcards for now.
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated imports in {filename}")

base_path = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"
update_imports(os.path.join(base_path, "src"))

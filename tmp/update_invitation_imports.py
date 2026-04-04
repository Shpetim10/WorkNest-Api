import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

inv_imports = [
    ("UserInvitationController", "com.worknest.auth.controller", "com.worknest.features.invitation.web"),
    ("RoleSelectionController", "com.worknest.auth.controller", "com.worknest.features.invitation.web"),
    
    ("InvitationService", "com.worknest.auth.service", "com.worknest.features.invitation.application"),
    ("InvitationServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.invitation.application"),
    ("InvitationActivationService", "com.worknest.auth.service", "com.worknest.features.invitation.application"),
    ("InvitationActivationServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.invitation.application"),
    ("RoleSelectionService", "com.worknest.auth.service", "com.worknest.features.invitation.application"),
    ("RoleSelectionServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.invitation.application"),
    
    ("UserInvitationRepository", "com.worknest.auth.repository", "com.worknest.features.invitation.repository"),
    
    ("ActivateInvitationRequest", "com.worknest.auth.dto", "com.worknest.features.invitation.dto"),
    ("ActivateInvitationResponse", "com.worknest.auth.dto", "com.worknest.features.invitation.dto"),
    ("CreateInvitationRequest", "com.worknest.auth.dto", "com.worknest.features.invitation.dto"),
    ("CreateInvitationResponse", "com.worknest.auth.dto", "com.worknest.features.invitation.dto"),
    ("SelectRoleRequest", "com.worknest.auth.dto", "com.worknest.features.invitation.dto"),
    ("SelectRoleResponse", "com.worknest.auth.dto", "com.worknest.features.invitation.dto"),
    
    ("InvalidInvitationException", "com.worknest.auth.exception", "com.worknest.features.invitation.exception"),
    ("InvalidInvitationRequestException", "com.worknest.auth.exception", "com.worknest.features.invitation.exception"),
    ("InvitationAlreadyExistsException", "com.worknest.auth.exception", "com.worknest.features.invitation.exception"),
    ("InvitationAlreadyUsedException", "com.worknest.auth.exception", "com.worknest.features.invitation.exception"),
    ("InvitationDeliveryFailedException", "com.worknest.auth.exception", "com.worknest.features.invitation.exception"),
    ("InvitationTokenExpiredException", "com.worknest.auth.exception", "com.worknest.features.invitation.exception"),
    ("InvitationTokenInvalidException", "com.worknest.auth.exception", "com.worknest.features.invitation.exception"),
]

def update_inv_imports(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                for cls, old_pkg, new_pkg in inv_imports:
                    old_import = f"import {old_pkg}.{cls};"
                    new_import = f"import {new_pkg}.{cls};"
                    new_content = new_content.replace(old_import, new_import)
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated imports in {filename}")

update_inv_imports(os.path.join(base, "src"))

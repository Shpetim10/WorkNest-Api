import os

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

auth_imports = [
    ("AuthController", "com.worknest.auth.controller", "com.worknest.features.auth.web"),
    ("PasswordResetController", "com.worknest.auth.controller", "com.worknest.features.auth.web"),
    ("RefreshTokenController", "com.worknest.auth.controller", "com.worknest.features.auth.web"),
    
    ("AuthLoginService", "com.worknest.auth.service", "com.worknest.features.auth.application"),
    ("AuthLoginServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.auth.application"),
    ("PasswordResetService", "com.worknest.auth.service", "com.worknest.features.auth.application"),
    ("PasswordResetServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.auth.application"),
    ("PasswordResetRequestService", "com.worknest.auth.service", "com.worknest.features.auth.application"),
    ("PasswordResetRequestServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.auth.application"),
    ("RefreshTokenService", "com.worknest.auth.service", "com.worknest.features.auth.application"),
    ("RefreshTokenServiceImpl", "com.worknest.auth.service.impl", "com.worknest.features.auth.application"),
    
    ("UserRepository", "com.worknest.auth.repository", "com.worknest.features.auth.repository"),
    ("RefreshTokenRepository", "com.worknest.auth.repository", "com.worknest.features.auth.repository"),
    ("PasswordResetTokenRepository", "com.worknest.auth.repository", "com.worknest.features.auth.repository"),
    ("PermissionRepository", "com.worknest.auth.repository", "com.worknest.features.auth.repository"),
    ("RoleAssignmentRepository", "com.worknest.auth.repository", "com.worknest.features.auth.repository"),
    ("RoleAssignmentPermissionRepository", "com.worknest.auth.repository", "com.worknest.features.auth.repository"),
    
    ("AvailableLoginContextDto", "com.worknest.auth.dto", "com.worknest.features.auth.dto"),
    ("ForgotPasswordRequest", "com.worknest.auth.dto", "com.worknest.features.auth.dto"),
    ("LoginRequest", "com.worknest.auth.dto", "com.worknest.features.auth.dto"),
    ("LoginResponse", "com.worknest.auth.dto", "com.worknest.features.auth.dto"),
    ("RefreshTokenRequest", "com.worknest.auth.dto", "com.worknest.features.auth.dto"),
    ("RefreshTokenResponse", "com.worknest.auth.dto", "com.worknest.features.auth.dto"),
    ("ResetPasswordRequest", "com.worknest.auth.dto", "com.worknest.features.auth.dto"),
    ("GenericMessageResponse", "com.worknest.auth.dto", "com.worknest.features.auth.dto"),
    
    ("AuthenticationFailedException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("InvalidCredentialsException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("NoPlatformAccessException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("RefreshTokenExpiredException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("RefreshTokenInvalidException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("RefreshTokenRevokedException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("ResetTokenAlreadyUsedException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("ResetTokenExpiredException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("ResetTokenInvalidException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("TokenExpiredException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("UserAlreadyActiveException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
    ("WeakPasswordException", "com.worknest.auth.exception", "com.worknest.features.auth.exception"),
]

def update_auth_imports(directory):
    for root, dirs, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".java"):
                filepath = os.path.join(root, filename)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                for cls, old_pkg, new_pkg in auth_imports:
                    old_import = f"import {old_pkg}.{cls};"
                    new_import = f"import {new_pkg}.{cls};"
                    new_content = new_content.replace(old_import, new_import)
                
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated imports in {filename}")

update_auth_imports(os.path.join(base, "src"))

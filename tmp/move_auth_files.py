import os
import shutil

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

moves = [
    ("src/main/java/com/worknest/auth/controller/AuthController.java", "src/main/java/com/worknest/features/auth/web"),
    ("src/main/java/com/worknest/auth/controller/PasswordResetController.java", "src/main/java/com/worknest/features/auth/web"),
    ("src/main/java/com/worknest/auth/controller/RefreshTokenController.java", "src/main/java/com/worknest/features/auth/web"),
    
    ("src/main/java/com/worknest/auth/service/AuthLoginService.java", "src/main/java/com/worknest/features/auth/application"),
    ("src/main/java/com/worknest/auth/service/impl/AuthLoginServiceImpl.java", "src/main/java/com/worknest/features/auth/application"),
    ("src/main/java/com/worknest/auth/service/PasswordResetService.java", "src/main/java/com/worknest/features/auth/application"),
    ("src/main/java/com/worknest/auth/service/impl/PasswordResetServiceImpl.java", "src/main/java/com/worknest/features/auth/application"),
    ("src/main/java/com/worknest/auth/service/PasswordResetRequestService.java", "src/main/java/com/worknest/features/auth/application"),
    ("src/main/java/com/worknest/auth/service/impl/PasswordResetRequestServiceImpl.java", "src/main/java/com/worknest/features/auth/application"),
    ("src/main/java/com/worknest/auth/service/RefreshTokenService.java", "src/main/java/com/worknest/features/auth/application"),
    ("src/main/java/com/worknest/auth/service/impl/RefreshTokenServiceImpl.java", "src/main/java/com/worknest/features/auth/application"),
    
    ("src/main/java/com/worknest/auth/repository/UserRepository.java", "src/main/java/com/worknest/features/auth/repository"),
    ("src/main/java/com/worknest/auth/repository/RefreshTokenRepository.java", "src/main/java/com/worknest/features/auth/repository"),
    ("src/main/java/com/worknest/auth/repository/PasswordResetTokenRepository.java", "src/main/java/com/worknest/features/auth/repository"),
    ("src/main/java/com/worknest/auth/repository/PermissionRepository.java", "src/main/java/com/worknest/features/auth/repository"),
    ("src/main/java/com/worknest/auth/repository/RoleAssignmentRepository.java", "src/main/java/com/worknest/features/auth/repository"),
    ("src/main/java/com/worknest/auth/repository/RoleAssignmentPermissionRepository.java", "src/main/java/com/worknest/features/auth/repository"),
    
    ("src/main/java/com/worknest/auth/dto/AvailableLoginContextDto.java", "src/main/java/com/worknest/features/auth/dto"),
    ("src/main/java/com/worknest/auth/dto/ForgotPasswordRequest.java", "src/main/java/com/worknest/features/auth/dto"),
    ("src/main/java/com/worknest/auth/dto/LoginRequest.java", "src/main/java/com/worknest/features/auth/dto"),
    ("src/main/java/com/worknest/auth/dto/LoginResponse.java", "src/main/java/com/worknest/features/auth/dto"),
    ("src/main/java/com/worknest/auth/dto/RefreshTokenRequest.java", "src/main/java/com/worknest/features/auth/dto"),
    ("src/main/java/com/worknest/auth/dto/RefreshTokenResponse.java", "src/main/java/com/worknest/features/auth/dto"),
    ("src/main/java/com/worknest/auth/dto/ResetPasswordRequest.java", "src/main/java/com/worknest/features/auth/dto"),
    ("src/main/java/com/worknest/auth/dto/GenericMessageResponse.java", "src/main/java/com/worknest/features/auth/dto"),
    
    ("src/main/java/com/worknest/auth/exception/AuthenticationFailedException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/InvalidCredentialsException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/NoPlatformAccessException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/RefreshTokenExpiredException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/RefreshTokenInvalidException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/RefreshTokenRevokedException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/ResetTokenAlreadyUsedException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/ResetTokenExpiredException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/ResetTokenInvalidException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/TokenExpiredException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/UserAlreadyActiveException.java", "src/main/java/com/worknest/features/auth/exception"),
    ("src/main/java/com/worknest/auth/exception/WeakPasswordException.java", "src/main/java/com/worknest/features/auth/exception"),
]

for src, dst in moves:
    src_path = os.path.join(base, src.replace("/", "\\"))
    dst_dir = os.path.join(base, dst.replace("/", "\\"))
    if not os.path.exists(dst_dir):
        os.makedirs(dst_dir)
    if os.path.exists(src_path):
        shutil.move(src_path, dst_dir)
        print(f"Moved {os.path.basename(src_path)}")
    else:
        print(f"File not found: {src_path}")

import os
import shutil

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

moves = [
    ("src/main/java/com/worknest/auth/controller/UserInvitationController.java", "src/main/java/com/worknest/features/invitation/web"),
    ("src/main/java/com/worknest/auth/controller/RoleSelectionController.java", "src/main/java/com/worknest/features/invitation/web"),
    
    ("src/main/java/com/worknest/auth/service/InvitationService.java", "src/main/java/com/worknest/features/invitation/application"),
    ("src/main/java/com/worknest/auth/service/impl/InvitationServiceImpl.java", "src/main/java/com/worknest/features/invitation/application"),
    ("src/main/java/com/worknest/auth/service/InvitationActivationService.java", "src/main/java/com/worknest/features/invitation/application"),
    ("src/main/java/com/worknest/auth/service/impl/InvitationActivationServiceImpl.java", "src/main/java/com/worknest/features/invitation/application"),
    ("src/main/java/com/worknest/auth/service/RoleSelectionService.java", "src/main/java/com/worknest/features/invitation/application"),
    ("src/main/java/com/worknest/auth/service/impl/RoleSelectionServiceImpl.java", "src/main/java/com/worknest/features/invitation/application"),
    
    ("src/main/java/com/worknest/auth/repository/UserInvitationRepository.java", "src/main/java/com/worknest/features/invitation/repository"),
    
    ("src/main/java/com/worknest/auth/dto/ActivateInvitationRequest.java", "src/main/java/com/worknest/features/invitation/dto"),
    ("src/main/java/com/worknest/auth/dto/ActivateInvitationResponse.java", "src/main/java/com/worknest/features/invitation/dto"),
    ("src/main/java/com/worknest/auth/dto/CreateInvitationRequest.java", "src/main/java/com/worknest/features/invitation/dto"),
    ("src/main/java/com/worknest/auth/dto/CreateInvitationResponse.java", "src/main/java/com/worknest/features/invitation/dto"),
    ("src/main/java/com/worknest/auth/dto/SelectRoleRequest.java", "src/main/java/com/worknest/features/invitation/dto"),
    ("src/main/java/com/worknest/auth/dto/SelectRoleResponse.java", "src/main/java/com/worknest/features/invitation/dto"),
    
    ("src/main/java/com/worknest/auth/exception/InvalidInvitationException.java", "src/main/java/com/worknest/features/invitation/exception"),
    ("src/main/java/com/worknest/auth/exception/InvalidInvitationRequestException.java", "src/main/java/com/worknest/features/invitation/exception"),
    ("src/main/java/com/worknest/auth/exception/InvitationAlreadyExistsException.java", "src/main/java/com/worknest/features/invitation/exception"),
    ("src/main/java/com/worknest/auth/exception/InvitationAlreadyUsedException.java", "src/main/java/com/worknest/features/invitation/exception"),
    ("src/main/java/com/worknest/auth/exception/InvitationDeliveryFailedException.java", "src/main/java/com/worknest/features/invitation/exception"),
    ("src/main/java/com/worknest/auth/exception/InvitationTokenExpiredException.java", "src/main/java/com/worknest/features/invitation/exception"),
    ("src/main/java/com/worknest/auth/exception/InvitationTokenInvalidException.java", "src/main/java/com/worknest/features/invitation/exception"),
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

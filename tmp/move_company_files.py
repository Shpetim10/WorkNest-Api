import os
import shutil

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

moves = [
    ("src/main/java/com/worknest/auth/controller/CompanyRegistrationController.java", "src/main/java/com/worknest/features/company/web"),
    
    ("src/main/java/com/worknest/auth/service/CompanyRegistrationService.java", "src/main/java/com/worknest/features/company/application"),
    ("src/main/java/com/worknest/auth/service/impl/CompanyRegistrationServiceImpl.java", "src/main/java/com/worknest/features/company/application"),
    
    ("src/main/java/com/worknest/auth/repository/CompanyRepository.java", "src/main/java/com/worknest/features/company/repository"),
    ("src/main/java/com/worknest/auth/repository/CompanySiteRepository.java", "src/main/java/com/worknest/features/company/repository"),
    ("src/main/java/com/worknest/auth/repository/SiteTrustedNetworkRepository.java", "src/main/java/com/worknest/features/company/repository"),
    
    ("src/main/java/com/worknest/auth/dto/CompanyRegistrationRequest.java", "src/main/java/com/worknest/features/company/dto"),
    ("src/main/java/com/worknest/auth/dto/CompanyRegistrationResponse.java", "src/main/java/com/worknest/features/company/dto"),
    ("src/main/java/com/worknest/auth/dto/CompanySiteResponse.java", "src/main/java/com/worknest/features/company/dto"),
    ("src/main/java/com/worknest/auth/dto/CreateCompanySiteRequest.java", "src/main/java/com/worknest/features/company/dto"),
    ("src/main/java/com/worknest/auth/dto/CreateTrustedNetworkRequest.java", "src/main/java/com/worknest/features/company/dto"),
    ("src/main/java/com/worknest/auth/dto/SiteTrustedNetworkResponse.java", "src/main/java/com/worknest/features/company/dto"),
    
    ("src/main/java/com/worknest/auth/exception/AdminEmailAlreadyExistsException.java", "src/main/java/com/worknest/features/company/exception"),
    ("src/main/java/com/worknest/auth/exception/CompanyAlreadyExistsException.java", "src/main/java/com/worknest/features/company/exception"),
    ("src/main/java/com/worknest/auth/exception/CompanySlugAlreadyExistsException.java", "src/main/java/com/worknest/features/company/exception"),
    ("src/main/java/com/worknest/auth/exception/InvalidRegistrationDataException.java", "src/main/java/com/worknest/features/company/exception"),
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

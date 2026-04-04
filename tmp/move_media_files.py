import os
import shutil

base = r"c:\Users\shaba\Desktop\WorkNest - SPM\WorkNest-Api"

moves = [
    ("src/main/java/com/worknest/auth/controller/MediaController.java", "src/main/java/com/worknest/features/media/web"),
    
    ("src/main/java/com/worknest/auth/service/MediaStorageService.java", "src/main/java/com/worknest/features/media/application"),
    ("src/main/java/com/worknest/auth/service/impl/LocalMediaStorageServiceImpl.java", "src/main/java/com/worknest/features/media/application"),
    
    ("src/main/java/com/worknest/auth/dto/MediaUploadResponse.java", "src/main/java/com/worknest/features/media/dto"),
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

import os

path = r"C:\Users\kar\StudioProjects\22222\Th3GradeLibraryKMP"
old_str = "com.grade3.library"
new_str = "com.Nightjar.gradeiraqi3library"

for root, dirs, files in os.walk(path):
    if ".git" in root or ".gradle" in root or "build" in root or ".idea" in root or ".kotlin" in root:
        continue
    for file in files:
        if file.endswith((".kt", ".kts", ".xml", ".java", "AndroidManifest.xml")):
            file_path = os.path.join(root, file)
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                if old_str in content:
                    print(f"Updating {file_path}")
                    content = content.replace(old_str, new_str)
                    with open(file_path, "w", encoding="utf-8") as f:
                        f.write(content)
            except Exception as e:
                print(f"Error processing {file_path}: {e}")

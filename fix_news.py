import re

with open(r"C:\Users\kar\StudioProjects\22222\Th3GradeLibraryKMP\shared\src\commonMain\kotlin\com\kar\th3grade\library\ui\NewsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# I will use a simple regex to find the blocks and fix them.

# Actually, my previous replace_file_content at 14:12 corrupted the file.
# I will just write a very resilient string replacement script.

# Wait, the easiest way to fix it is to take the original from my view_file output. 

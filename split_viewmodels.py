import os
import re

with open('app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt', 'r') as f:
    text = f.read()

# Create directory if needed
os.makedirs('app/src/main/java/com/example/ui/viewmodel', exist_ok=True)

# Find all classes and their contents
classes = re.split(r'\n// -{61}\n// (.*?) ViewModel\n// -{61}\n', text)

# The first element is the imports and package
header = classes[0].strip()

# Now we iterate through pairs of (Name, Content)
for i in range(1, len(classes), 2):
    name = classes[i].strip()
    content = classes[i+1]
    
    # Extract just the data classes and viewmodel class related to this
    file_content = f"{header}\n\n// -------------------------------------------------------------\n// {name} ViewModel\n// -------------------------------------------------------------\n{content.strip()}\n"
    
    filename = f"app/src/main/java/com/example/ui/viewmodel/{name.replace(' ', '')}ViewModel.kt"
    with open(filename, 'w') as out:
        out.write(file_content)
        print(f"Created {filename}")


with open('app/src/main/java/com/example/ui/screens/JamRoomScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
imports = []
for line in lines:
    if line.startswith('import ') and 'androidx.compose.ui.platform.LocalClipboardManager' in line:
        imports.append(line)
    elif line.startswith('import ') and 'androidx.compose.ui.text.AnnotatedString' in line:
        imports.append(line)
    elif line.startswith('import ') and 'android.widget.Toast' in line:
        imports.append(line)
    elif line.startswith('import ') and 'androidx.compose.ui.platform.LocalContext' in line:
        imports.append(line)
    else:
        new_lines.append(line)

final_lines = []
for i, line in enumerate(new_lines):
    final_lines.append(line)
    if line.startswith('package '):
        final_lines.append('\n')
        final_lines.extend(imports)

with open('app/src/main/java/com/example/ui/screens/JamRoomScreen.kt', 'w') as f:
    f.writelines(final_lines)

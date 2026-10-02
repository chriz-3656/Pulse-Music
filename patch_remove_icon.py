import re

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'r') as f:
    text = f.read()

old_code = """Icon(androidx.compose.material.icons.filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))"""

text = text.replace(old_code, "")
text = text.replace('import androidx.compose.material.icons.filled.Timer\n', '')

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'w') as f:
    f.write(text)

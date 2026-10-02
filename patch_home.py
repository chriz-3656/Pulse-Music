import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('width(136.dp)', 'width(148.dp)')
text = text.replace('size(120.dp)', 'size(132.dp)')
text = text.replace('padding(horizontal = 8.dp)', 'padding(horizontal = 12.dp)')
text = text.replace('Arrangement.spacedBy(16.dp)', 'Arrangement.spacedBy(24.dp)')
text = text.replace('Arrangement.spacedBy(12.dp)', 'Arrangement.spacedBy(20.dp)')

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(text)

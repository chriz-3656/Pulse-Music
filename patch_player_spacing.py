import re

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('.padding(horizontal = 20.dp, vertical = 12.dp)', '.padding(horizontal = 24.dp, vertical = 24.dp)')
text = text.replace('.padding(horizontal = 14.dp, vertical = 10.dp)', '.padding(horizontal = 16.dp, vertical = 16.dp)')
text = text.replace('.padding(horizontal = 12.dp, vertical = 10.dp)', '.padding(horizontal = 16.dp, vertical = 16.dp)')
text = text.replace('Spacer(modifier = Modifier.height(10.dp))', 'Spacer(modifier = Modifier.height(24.dp))')
text = text.replace('Spacer(modifier = Modifier.height(8.dp))', 'Spacer(modifier = Modifier.height(16.dp))')

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'w') as f:
    f.write(text)

import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    text = f.read()

target = 'color = MaterialTheme.colorScheme.onSurfaceVariant\n                )\n            }\n        }\n    }'

replacement = '''color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            SkeuoTactileButton(
                onClick = { showJamDialog = true },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Pulse Jam",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }'''

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(text)

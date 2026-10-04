import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    text = f.read()

# I will add it in the same Row with SkeuoAppLogo, replacing the right side if there's space.
# Wait, let's insert it inside the Column below the text!

old_text = """                Text(
                    text = "Lossless acoustics & ultra-fast playback response",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }"""

new_text = """                Text(
                    text = "Lossless acoustics & ultra-fast playback response",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            SkeuoTactileButton(
                onClick = { showJamDialog = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Pulse Jam",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }"""

text = text.replace(old_text, new_text)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(text)

import re

with open('app/src/main/java/com/example/ui/theme/Color.kt', 'r') as f:
    text = f.read()

text = re.sub(r'val NeuLightAccent = Color\(.*?x.*?\)', 'val NeuLightAccent = Color(0xFFFFC107)', text)
text = re.sub(r'val NeuDarkAccent = Color\(.*?x.*?\)', 'val NeuDarkAccent = Color(0xFFFFCA28)', text)
text = re.sub(r'val SkeuoAmberGlow = NeuDarkAccent', 'val SkeuoAmberGlow = Color(0xFFFFC107)', text)
text = re.sub(r'val SkeuoAmberDim = Color\(.*?x.*?\)', 'val SkeuoAmberDim = Color(0x66FFC107)', text)
text = re.sub(r'val SkeuoLcdCyan = NeuDarkAccent', 'val SkeuoLcdCyan = Color(0xFF00E5FF)', text)

with open('app/src/main/java/com/example/ui/theme/Color.kt', 'w') as f:
    f.write(text)

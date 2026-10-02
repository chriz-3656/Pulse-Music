import re

with open('app/src/main/java/com/example/ui/components/CommonComponents.kt', 'r') as f:
    text = f.read()

# Make sure we add necessary imports for pointerInput, animateDpAsState, etc.
if 'import androidx.compose.animation.core.animateDpAsState' not in text:
    text = text.replace('import androidx.compose.runtime.Composable', 'import androidx.compose.animation.core.animateDpAsState\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.gestures.detectTapGestures\nimport androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.runtime.Composable')

old_button = r'''@Composable
fun SkeuoTactileButton\(
    onClick: \(\) -> Unit,
    modifier: Modifier = Modifier,
    isPressedOrActive: Boolean = false,
    shape: Shape = RoundedCornerShape\(20\.dp\),
    accentColor: Color = MaterialTheme\.colorScheme\.primary,
    content: @Composable \(\) -> Unit
\) \{.*?\}\n\}'''

new_button = '''@Composable
fun SkeuoTactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPressedOrActive: Boolean = false,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val neu = LocalNeuColors.current
    val cornerSize = if (shape is RoundedCornerShape) 20.dp else 16.dp
    
    var isTapped by remember { mutableStateOf(false) }
    val isActuallyPressed = isPressedOrActive || isTapped
    
    val animatedOffsetX by animateDpAsState(targetValue = if (isActuallyPressed) (-2).dp else 6.dp, animationSpec = tween(150), label = "")
    val animatedOffsetY by animateDpAsState(targetValue = if (isActuallyPressed) (-2).dp else 6.dp, animationSpec = tween(150), label = "")
    val animatedBlur by animateDpAsState(targetValue = if (isActuallyPressed) 4.dp else 12.dp, animationSpec = tween(150), label = "")
    
    Box(
        modifier = modifier
            .neuShadow(
                lightShadow = neu.lightShadow,
                darkShadow = neu.darkShadow,
                cornerRadius = cornerSize,
                offsetX = animatedOffsetX,
                offsetY = animatedOffsetY,
                blurRadius = animatedBlur,
                isPressed = isActuallyPressed
            )
            .clip(shape)
            .background(neu.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isTapped = true
                        tryAwaitRelease()
                        isTapped = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}'''

text = re.sub(old_button, new_button, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/components/CommonComponents.kt', 'w') as f:
    f.write(text)

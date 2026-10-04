import re

with open('app/src/main/java/com/example/ui/components/CommonComponents.kt', 'r') as f:
    text = f.read()

target = '''    var isTapped by remember { mutableStateOf(false) }
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
            },'''

replacement = '''    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActuallyPressed = isPressedOrActive || isPressed
    
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),'''

text = text.replace(target, replacement)

# Add imports if missing
if 'import androidx.compose.foundation.interaction.collectIsPressedAsState' not in text:
    text = text.replace('import androidx.compose.ui.unit.dp', 'import androidx.compose.ui.unit.dp\nimport androidx.compose.foundation.interaction.collectIsPressedAsState\nimport androidx.compose.foundation.clickable')

with open('app/src/main/java/com/example/ui/components/CommonComponents.kt', 'w') as f:
    f.write(text)

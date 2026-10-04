import re

with open('app/src/main/java/com/example/ui/screens/JamRoomScreen.kt', 'r') as f:
    text = f.read()

target1 = '''fun JamRoomScreen(
    jamViewModel: JamViewModel,
    onBackClick: () -> Unit,
    onGoToPlayer: () -> Unit
) {'''
replacement1 = '''import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamRoomScreen(
    jamViewModel: JamViewModel,
    onBackClick: () -> Unit,
    onGoToPlayer: () -> Unit,
    onPlaySong: (com.example.domain.model.Song) -> Unit
) {'''
text = text.replace(target1, replacement1)

target2 = '''    val searchResults by jamViewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }'''
replacement2 = '''    val searchResults by jamViewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current'''
text = text.replace(target2, replacement2)

target3 = '''                        Text(
                            text = activeRoom.roomId,
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )'''
replacement3 = '''                        Text(
                            text = activeRoom.roomId,
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(activeRoom.roomId))
                                Toast.makeText(context, "Room Code Copied", Toast.LENGTH_SHORT).show()
                            }
                        )'''
text = text.replace(target3, replacement3)

target4 = '''                items(activeRoom.queue) { song ->
                    SkeuoBevelCard(modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {'''
replacement4 = '''                items(activeRoom.queue) { song ->
                    SkeuoTactileButton(
                        onClick = { onPlaySong(song) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {'''
text = text.replace(target4, replacement4)

with open('app/src/main/java/com/example/ui/screens/JamRoomScreen.kt', 'w') as f:
    f.write(text)

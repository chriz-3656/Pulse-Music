import re

with open('app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt', 'r') as f:
    text = f.read()

# Add import
text = text.replace('import com.example.domain.usecase.ManageSettingsUseCase', 'import com.example.domain.usecase.ManageSettingsUseCase\nimport com.example.ui.viewmodel.JamViewModel')

# Add branch in when statement
old_branch = """            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    appContainer.manageSettingsUseCase, 
                    appContainer.playerController
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")"""

new_branch = """            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    appContainer.manageSettingsUseCase, 
                    appContainer.playerController
                ) as T
            }
            modelClass.isAssignableFrom(JamViewModel::class.java) -> {
                JamViewModel(appContainer.jamSessionManager) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")"""

text = text.replace(old_branch, new_branch)

with open('app/src/main/java/com/example/ui/viewmodel/SettingsViewModel.kt', 'w') as f:
    f.write(text)

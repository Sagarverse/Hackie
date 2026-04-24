import os
import re

files_to_check = [
    "app/src/main/java/com/example/rabit/ui/automation/InjectorScreen.kt",
    "app/src/main/java/com/example/rabit/ui/automation/RemoteExplorerScreen.kt",
    "app/src/main/java/com/example/rabit/ui/automation/ReverseShellScreen.kt",
    "app/src/main/java/com/example/rabit/ui/automation/TerminalScannerScreen.kt",
    "app/src/main/java/com/example/rabit/ui/automation/AutomationDashboardScreen.kt",
    "app/src/main/java/com/example/rabit/ui/automation/AutoClickerScreen.kt",
    "app/src/main/java/com/example/rabit/ui/airplay/AirPlayReceiverScreen.kt",
    "app/src/main/java/com/example/rabit/ui/webbridge/WebBridgeScreen.kt",
    "app/src/main/java/com/example/rabit/ui/assistant/AssistantScreen.kt"
]

def remove_scaffold(content):
    # This regex tries to match the Scaffold block. 
    # It finds Scaffold(...) { padding -> and replaces it with nothing (or just the body).
    # Removing just the topBar from these Scaffolds would be 100x safer and completely solves the "double app bar" issue without messing up closing braces!
    
    # Wait, replacing topBar = { CenterAlignedTopAppBar(...) }, with nothing is much safer!
    
    # Regex to carefully match topBar block inside Scaffold
    new_content = re.sub(r'topBar\s*=\s*\{\s*(?:CenterAligned)?TopAppBar\([\s\S]*?(?=\n\s*(?:contentWindowInsets|snackbarHost|containerColor|floatingActionButton|bottomBar|modifier|\)\s*\{))\n?', '', content)
    
    
    # If the file still has TopAppBar but it was the ONLY argument before ) { padding ->
    # Let's write a targeted regex:
    # Match `topBar = { ... CenterAlignedTopAppBar( ... ) ... },`
    return new_content

for f in files_to_check:
    print("Checking", f)
    with open(f, "r") as file:
        content = file.read()
    
    # Easiest way to fix double app bars is literally just stripping the topBar = { ... } line completely.
    # Let's just find the bounds of topBar = { ... } inside Scaffold.

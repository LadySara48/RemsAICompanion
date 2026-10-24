"""Package verified Fabric ports for manual testing/release uploads."""
from pathlib import Path
import hashlib, json, subprocess, sys, zipfile

here = Path(__file__).resolve().parent
root = here.parents[1]
subprocess.run([sys.executable, str(here / 'verify-dist.py')], check=True)
mod_version = next(line.split('=', 1)[1].strip() for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
targets = ['1.18.2', '1.20.1', '1.21.1', '1.21.4', '1.21.11', '26.2']
files = [root / 'dist/fabric' / f'rems-ai-companion-{mod_version}+fabric-{mc}.jar' for mc in targets]
output = root / 'dist' / ('rems-ai-companion-' + mod_version + '-fabric-ports.zip')
checksums = ''.join(hashlib.sha256(file.read_bytes()).hexdigest() + '  ' + file.name + '\n' for file in files)
readme = '''Rem's AI Companion - Fabric ports

Extract this archive. Install ONLY the JAR matching your exact Minecraft version.
Put it in that installation's mods folder alongside Fabric API for that version.
Fabric Loader 0.19.5 or newer is required. This mod is client-side.

Minecraft 1.18.2 / 1.20.1: Java 17 or newer.
Minecraft 1.21.1 / 1.21.4 / 1.21.11: Java 21 or newer.
Minecraft 26.2: Java 25 or newer.
Fabric 26.3 is built separately at the repository root.

R opens the menu; Esc closes it. Configure your provider/model/API key in API Settings.
All 16 translations, images, voices and AI features are included in each JAR.
Your existing config/rems_ai_companion data format is preserved.

Compilation, shared tests, remapping, metadata, bytecode and packaged assets were checked.
An in-game test is still required before publishing each target as tested.
Do not upload your local configuration, API keys or chat history with the mod.
'''
with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
    for file in files: archive.write(file, file.name)
    archive.writestr('README.txt', readme)
    archive.writestr('LICENSE-ASSETS.txt', (root / 'LICENSE-ASSETS.txt').read_bytes())
    archive.writestr('SHA256SUMS.txt', checksums)
print('Packaged', len(files), 'Fabric ports:', output)

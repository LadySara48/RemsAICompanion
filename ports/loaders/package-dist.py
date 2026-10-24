from pathlib import Path
import hashlib, json, subprocess, sys, zipfile

here = Path(__file__).resolve().parent
root = here.parents[1]
matrix = json.loads(here.joinpath('versions.json').read_text())
mod_version = next(line.split('=',1)[1].strip() for line in root.joinpath('gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
for loader, targets in matrix.items():
    subprocess.run([sys.executable, str(here/'verify-dist.py'), loader], check=True)
    files = [root/'dist'/loader/f'rems-ai-companion-{mod_version}+{loader}-{mc}.jar' for mc in targets]
    output = root/'dist'/f'rems-ai-companion-{mod_version}-{loader}-ports.zip'
    description = f'''Rem's AI Companion - {loader} ports

Install only ONE JAR: the one matching your exact Minecraft version and loader.
Fabric API is not required. This is a client-side mod. R opens the menu; Esc closes it.
Forge targets require Java 17 or newer; NeoForge 1.21.x requires Java 21 or newer;
NeoForge 26.x requires Java 25 or newer.

Your API keys and chats are not included in this archive.
The 16 translations, images, sounds, AI providers, prompts, summaries and chat features are shared.
Compilation and automated tests passed; in-game tests still need to be completed for each target.

Build versions (use these or later compatible builds for the same Minecraft version):
''' + '\n'.join(f'{mc}: {platform}' for mc, platform in targets.items()) + '\n'
    checksums = ''.join(hashlib.sha256(file.read_bytes()).hexdigest()+'  '+file.name+'\n' for file in files)
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as archive:
        for file in files: archive.write(file,file.name)
        archive.writestr('README.txt',description)
        archive.writestr('LICENSE-ASSETS.txt', (root / 'LICENSE-ASSETS.txt').read_bytes())
        archive.writestr('SHA256SUMS.txt',checksums)
    print('Packaged',len(files),'ports:',output)

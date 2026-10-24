"""Validate installable Forge/NeoForge artifacts without starting Minecraft or making AI calls."""
from pathlib import Path
import json, re, struct, sys, tomllib, zipfile

root = Path(__file__).resolve().parents[2]
matrix = json.loads((Path(__file__).with_name('versions.json')).read_text())
loader = sys.argv[1]
targets = sys.argv[2:] or list(matrix[loader])
mod_version = next(line.split('=',1)[1].strip() for line in (root/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
resources = root/'src/main/resources'
assets = {p.relative_to(resources).as_posix():p.read_bytes() for p in (resources/'assets').rglob('*') if p.is_file()}
for mc in targets:
    java = 17 if loader == 'forge' else 25 if mc.startswith('26.') else 21
    filename = f'rems-ai-companion-{mod_version}+{loader}-{mc}.jar'
    path = root/'dist'/loader/filename
    with zipfile.ZipFile(path) as jar:
        assert jar.testzip() is None, path
        names = jar.namelist()
        metadata_name = 'META-INF/mods.toml' if loader == 'forge' else 'META-INF/neoforge.mods.toml'
        metadata = tomllib.loads(jar.read(metadata_name).decode())
        assert metadata['modLoader'] == 'javafml', path
        assert metadata['mods'][0]['logoFile'] == 'assets/rems_ai_companion/icon.png', path
        assert jar.read('LICENSE-ASSETS.txt') == (root / 'LICENSE-ASSETS.txt').read_bytes(), path
        assert metadata['mods'][0]['modId'] == 'rems_ai_companion', path
        assert metadata['mods'][0]['version'] == f'{mod_version}+{loader}-{mc}', path
        dependencies = {item['modId']:item for item in metadata['dependencies']['rems_ai_companion']}
        assert dependencies['minecraft']['versionRange'] == f'[{mc}]', path
        assert loader in dependencies, path
        assert 'fabric.mod.json' not in names, path
        assert 'org/hearlov/remscompanion/RemsAICompanion.class' in names, path
        assert 'org/hearlov/remscompanion/api/platform/PlatformChatStorage.class' in names, path
        assert json.loads(jar.read('pack.mcmeta'))['pack']['description'], path
        reobfuscated = False
        for name in names:
            assert not name.startswith(('config/','run/','chats/','backup/')), name
            if not name.endswith('.class'): continue
            data = jar.read(name)
            assert struct.unpack('>H', data[6:8])[0] <= java + 44, (name, java)
            assert b'net/fabricmc/' not in data, (name, loader)
            reobfuscated |= re.search(rb'm_\d+_', data) is not None
        if loader == 'forge': assert reobfuscated, 'Forge artifact is not reobfuscated: ' + filename
        for name, data in assets.items(): assert jar.read(name) == data, (mc, name)
    print(f'{loader} {mc}: Java {java}, metadata, platform isolation and {len(assets)} assets verified')

"""Check shipping JAR metadata, runtime bytecode, shared assets and translations."""
from pathlib import Path
import json, struct, sys, zipfile

root = Path(__file__).resolve().parents[2]
versions = sys.argv[1:] or ['1.18.2', '1.20.1', '1.21.1', '1.21.4', '1.21.11', '26.2']
resources = root / 'src/main/resources'
mod_version = next(line.split('=', 1)[1].strip() for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
assets = {p.relative_to(resources).as_posix(): p.read_bytes() for p in (resources / 'assets').rglob('*') if p.is_file()}
for version in versions:
    path = root / 'dist/fabric' / ('rems-ai-companion-' + mod_version + '+fabric-' + version + '.jar')
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        metadata = json.loads(jar.read('fabric.mod.json'))
        java = 25 if version.startswith('26.') else 21 if version.startswith('1.21') else 17
        assert metadata['depends']['minecraft'] == version, path
        assert metadata['depends']['java'] == '>=' + str(java), path
        assert metadata['environment'] == 'client', path
        assert metadata['icon'] == 'assets/rems_ai_companion/icon.png', path
        assert jar.read('LICENSE-ASSETS.txt') == (root / 'LICENSE-ASSETS.txt').read_bytes(), path
        assert ({'fabric-lifecycle-events-v1', 'fabric-key-mapping-api-v1'} if version.startswith('26.') else {'fabric-lifecycle-events-v1', 'fabric-key-binding-api-v1'}) <= metadata['depends'].keys(), path
        for entry in metadata['entrypoints']['client']:
            assert entry.replace('.', '/') + '.class' in names, entry
        for name in names:
            if name.endswith('.class'):
                major = struct.unpack('>H', jar.read(name)[6:8])[0]
                assert major <= java + 44, (name, major, java)
            assert not name.startswith(('config/', 'run/', 'chats/', 'backup/')), name
        for name, data in assets.items():
            assert jar.read(name) == data, (version, name)
        # A shipping Fabric JAR must reference intermediary names, not Mojang's development classes.
        screen = jar.read('org/hearlov/remscompanion/MinimalScreen.class')
        assert (b'net/minecraft/client/gui/screens/Screen' if version.startswith('26.') else b'net/minecraft/class_437') in screen, 'Wrong Minecraft namespace: ' + str(path)
    print(version + ': metadata, Java ' + str(java) + ', Minecraft namespace and all ' + str(len(assets)) + ' assets verified')

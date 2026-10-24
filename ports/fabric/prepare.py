"""Generate version bridges from the canonical client; assets/translations are never copied or edited."""
from pathlib import Path
import sys, re

mc, destination = sys.argv[1], Path(sys.argv[2])
loader = sys.argv[3] if len(sys.argv) > 3 else 'fabric'
if loader not in ('fabric', 'forge', 'neoforge'):
    raise ValueError('Unknown loader')
root = Path(__file__).resolve().parents[2]
destination = destination.resolve()
if not destination.is_relative_to(root / ('ports/' + loader + '/build')):
    raise ValueError('Generated sources must stay inside the loader build directory')
minor = tuple(map(int, mc.split('.')))
unobfuscated = minor >= (26,)
old = minor < (1, 20)
new_input = minor >= (1, 21, 9)
destination.mkdir(parents=True, exist_ok=True)
for obsolete in destination.rglob('*.java'):
    obsolete.unlink()
package = destination / 'org/hearlov/remscompanion'
package.mkdir(parents=True, exist_ok=True)

def transform(text):
    text = text.replace('net.minecraft.client.gui.GuiGraphicsExtractor', 'org.hearlov.remscompanion.compat.GuiGraphicsExtractor')
    text = text.replace('net.minecraft.client.input.KeyEvent', 'org.hearlov.remscompanion.compat.KeyEvent')
    text = text.replace('net.minecraft.client.input.MouseButtonEvent', 'org.hearlov.remscompanion.compat.MouseButtonEvent')
    text = text.replace('net.minecraft.client.gui.components.MultiLineEditBox', 'org.hearlov.remscompanion.MultiLineEditBox')
    text = text.replace('net.minecraft.client.gui.components.EditBox', 'org.hearlov.remscompanion.EditBox')
    text = text.replace('net.minecraft.client.gui.components.Tooltip', 'org.hearlov.remscompanion.compat.Tip')
    text = re.sub(r'([\w.()]+)\.setTooltip\(([^;]+)\);', r'org.hearlov.remscompanion.compat.Tip.set(\1, \2);', text)
    text = text.replace('net.minecraft.resources.Identifier', 'net.minecraft.resources.ResourceLocation').replace('Identifier', 'ResourceLocation')
    factory = 'ResourceLocation.fromNamespaceAndPath(' if minor >= (1, 21) else 'new ResourceLocation('
    text = text.replace('ResourceLocation.fromNamespaceAndPath(', factory)
    text = text.replace('.gui.setScreen(', '.setScreen(').replace('.gui.screen()', '.screen')
    text = text.replace('.dimension().identifier()', '.dimension().location()')
    text = text.replace('SharedConstants.getCurrentVersion().name()', 'SharedConstants.getCurrentVersion().getName()' if minor < (1,21,6) else 'SharedConstants.getCurrentVersion().name()')
    if not new_input:
        text = text.replace('.addFormatter(', '.setFormatter(')
    for name in ('session.messages', 'current.messages', 'SESSIONS'):
        text = text.replace(name + '.getLast()', name + '.get(' + name + '.size() - 1)')
        text = text.replace(name + '.getFirst()', name + '.get(0)')
    text = text.replace('session.heldDrafts.removeFirst()', 'session.heldDrafts.remove(0)')
    text = text.replace('Math.clamp(', 'org.hearlov.remscompanion.compat.Numbers.clamp(')
    text = text.replace('InputConstants.MOUSE_BUTTON_LEFT', 'org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT')
    text = text.replace('InputConstants.KEY_RETURN', 'org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER').replace('InputConstants.KEY_NUMPADENTER', 'org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER')
    if minor < (1,21,5):
        text = text.replace('style.getClickEvent() instanceof ClickEvent.OpenUrl link', 'style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL')
        text = text.replace('hoveredLink = link;', 'hoveredLink = style.getClickEvent();')
        text = text.replace('new ClickEvent.OpenUrl(URI.create(matcher.group(7)))', 'new ClickEvent(ClickEvent.Action.OPEN_URL, matcher.group(7))')
    if minor < (1,21,6):
        text = text.replace('defaultHandleClickEvent(content.link(), minecraft, this)', 'handleComponentClicked(Style.EMPTY.withClickEvent(content.link()))')
        if 'handleComponentClicked(Style.' in text: text = text.replace('import net.minecraft.network.chat.Component;', 'import net.minecraft.network.chat.Component;\nimport net.minecraft.network.chat.Style;')
    if minor < (1,19):
        text = text.replace('net.minecraft.network.chat.Component.', 'Component.')
        text = text.replace('Component.translatable(', 'new net.minecraft.network.chat.TranslatableComponent(')
        text = text.replace('Component.literal(', 'new net.minecraft.network.chat.TextComponent(')
        text = text.replace('Component.empty()', 'new net.minecraft.network.chat.TextComponent("")')
    if old:
        text = text.replace('SoundEvent.createVariableRangeEvent(', 'new SoundEvent(')
    if minor >= (1,21,11):
        text = text.replace('ResourceLocation', 'Identifier').replace('.dimension().location()', '.dimension().identifier()')
    return text

excluded = {'MinimalScreen.java', 'MinimalButton.java', 'RemsAICompanionClient.java'}
if unobfuscated:
    excluded = set() if loader == 'fabric' else {'RemsAICompanionClient.java'}
for source in (root / 'src/client/java').rglob('*.java'):
    if source.name in excluded: continue
    relative = source.relative_to(root / 'src/client/java')
    target = destination / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    original = source.read_text(encoding='utf-8-sig')
    if unobfuscated and minor < (26,2):
        original = original.replace('.gui.setScreen(', '.setScreen(').replace('.gui.screen()', '.screen')
    target.write_text(original if unobfuscated else transform(original), encoding='utf-8')

tokens = {
    '@NATIVE_GRAPHICS@': 'com.mojang.blaze3d.vertex.PoseStack' if old else 'net.minecraft.client.gui.GuiGraphics',
    '@WIDGET_RENDER@': 'renderButton' if old else 'renderWidget',
    '@BUTTON_RENDER@': 'renderContents' if minor >= (1,21,11) else 'renderButton' if old else 'renderWidget',
    '@BUTTON_SUPER@': 'super(x, y, width, height, label, b -> action.run());' if old else 'super(x, y, width, height, label, b -> action.run(), DEFAULT_NARRATION);',
    '@X@': 'x' if old else 'getX()', '@Y@': 'y' if old else 'getY()',
    '@KEY_CATEGORY@': '"key.category.rems_ai_companion.main"',
}
for source in Path(__file__).with_name('compat').rglob('*.java'):
    if unobfuscated: continue
    text = source.read_text(encoding='utf-8')
    # Select version fragments before the ordinary name transformation.
    for tag, enabled in [('OLD', old), ('MODERN', not old), ('LEGACY_INPUT', not new_input), ('NEW_INPUT', new_input), ('TEXT18', minor < (1,19)), ('TEXT19', minor >= (1,19)), ('PRE21', minor < (1,21)), ('POST21', minor >= (1,21)), ('PRE212', minor < (1,21,2)), ('POST212', minor >= (1,21,2)), ('PRE215', minor < (1,21,5)), ('POST215', minor >= (1,21,5)), ('PRE216', minor < (1,21,6)), ('POST216', minor >= (1,21,6)), ('PRE219', minor < (1,21,9)), ('POST219', minor >= (1,21,9))]:
        text = re.sub(r'/\*' + tag + r'\*/(.*?)/\*END' + tag + r'\*/', lambda m: m.group(1) if enabled else '', text, flags=re.S)
    for token, value in tokens.items(): text = text.replace(token, value)
    target = destination / source.relative_to(Path(__file__).with_name('compat'))
    target.parent.mkdir(parents=True, exist_ok=True)
    text = text.replace('net.minecraft.client.input.', 'NATIVE_INPUT.')
    text = text.replace('extends net.minecraft.client.gui.components.EditBox', 'extends NATIVE_EDIT_BOX')
    text = transform(text).replace('NATIVE_INPUT.', 'net.minecraft.client.input.').replace('NATIVE_EDIT_BOX', 'net.minecraft.client.gui.components.EditBox')
    target.write_text(text, encoding='utf-8')
if loader != 'fabric':
    # Platform boundaries only: directory selection, loader label, and lifecycle/key binding entrypoint.
    fml = 'net.minecraftforge' if loader == 'forge' else 'net.neoforged'
    for target in destination.rglob('*.java'):
        text = target.read_text(encoding='utf-8')
        text = text.replace('import net.fabricmc.loader.api.FabricLoader;', 'import ' + fml + '.fml.loading.FMLPaths;')
        text = text.replace('FabricLoader.getInstance().getConfigDir()', 'FMLPaths.CONFIGDIR.get()')
        text = text.replace('"Fabric"', '"Forge"' if loader == 'forge' else '"NeoForge"')
        text = text.replace('org.hearlov.remscompanion.api.fabric', 'org.hearlov.remscompanion.api.platform')
        text = text.replace('FabricChatStorage', 'PlatformChatStorage')
        if target.name == 'RemsAICompanionClient.java':
            target.unlink(); continue
        if target.name == 'FabricChatStorage.java':
            target.unlink(); target = package / 'api/platform/PlatformChatStorage.java'; target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding='utf-8')
print('Prepared', loader, mc)

"""Generates creature sprite sheets and lodge art with the Codex CLI image tool.

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in
the run's output is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py              # everything missing
    python tools/gen_art.py smudge lodge # only these
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, cozy warm palette, cute and round, big shiny eyes. Use the creature's own colours exactly as described "
    "(do not make it green or mossy unless that is described). It is an invented fantasy creature, not a real animal species."
)
OBJ_STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, mossy forest palette with warm lantern accents. These are plain inanimate objects: "
    "NO faces, NO eyes, NO mouths, NO blush, nothing alive about them."
)
GRID = (
    "Layout: 3 columns x 2 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. "
    "One creature per cell, centered, with clear margin; nothing touches the cell borders. No text, no grid lines, "
    "no shadows on the background, no other objects except those listed."
)

# Wild beasts: bad habits. Tamed by NOT feeding them. id: (look, signature move)
WILD = {
    "smudge": ("a small soot dragon made of grey-lavender smoke-fur, tail ending in curling smoke wisps, ember-orange eyes", "puffing a perfect smoke ring"),
    "froth": ("a round amber-gold beast like a honey-coloured barrel bear with a big white frothy foam mane like the head on a mug and a cork-shaped nose; amber, gold and white colours, NOT green", "wobbling and hiccuping little foam bubbles"),
    "toffee": ("a sticky little lizard made of amber candy glass with caramel drips on its back", "licking a big swirl lollipop"),
    "flicker": ("a fluffy moth-sprite with wings made of glowing blue rectangular panels like tiny screens", "hovering mesmerised, wings glowing bright"),
    "grabble": ("a little goblin-critter whose body is a tan burlap sack with rust-red patches, many tiny grabbing paws and ears like white paper price tags; burlap tan and rust-red colours, NOT green", "hugging a pile of small wrapped parcels"),
    "tumble": ("a small armadillo-like creature whose shell is a white cube with black pips like a die", "rolling itself like a thrown die"),
    "gobble": ("a plump round creature made of soft cream-white dough like a dumpling with an orange crust top, a wide toothy grin and tiny legs; cream and orange colours, NOT green", "stuffing a huge bun into its wide mouth"),
    "gnash": ("a small spiky goblin with slate-blue skin and grey-silver quills, long chisel teeth and claws like tiny clippers; slate blue and silver colours, NOT green", "nibbling the tips of its own claws"),
    "grumble": ("a tiny storm-cloud imp, dark grey cloud body with a crackling yellow lightning-bolt tail", "shouting with little lightning sparks and swirls above its head"),
    "jitter": ("a skinny jittery creature with long twitchy ears, coffee-bean patterned fur and a spring-coiled tail", "bouncing wide-eyed holding a tiny steaming cup"),
    "nightjar": ("a slinky shadow creature with starry dark-blue fur and a glowing crescent moon mark on its forehead", "wide awake at night, eyes glowing, tail flicking"),
    "murk": ("a shaggy creature made of grey fog with two warm lantern-like eyes and wisps for paws", "shifting its shape into a vague question mark of fog"),
}

# Shy creatures: good habits. Lured by doing the habit again and again. id: (look, signature move)
SHY = {
    "rambler": ("a small moss-furred creature with tiny antlers made of twigs and a leaf on its back", "walking along with a little walking stick"),
    "drip": ("a round translucent water-drop sprite with little fins and a ripple pattern", "splashing happily in a tiny puddle"),
    "folio": ("a soft creature whose long ears look like open book pages, ink-dot freckles", "reading a tiny book held in its paws"),
    "boing": ("a springy long-legged creature with ears like red mushroom caps", "doing a high star jump"),
    "drowse": ("a soft lamb-like creature of cloud-wool with a misty nightcap", "yawning a big sleepy yawn"),
    "lotus": ("a pale floating creature with lotus-petal ears and a calm face", "floating cross-legged in calm meditation"),
    "radish": ("a little round radish-bodied creature with leafy green hair", "munching a crisp carrot stick"),
    "glimmer": ("a small creature with a softly glowing lantern belly and moth-soft fur", "holding a tiny glowing heart-shaped light"),
    "stargaze": ("a small creature with a cloak-like wings patterned with star charts and constellations", "pointing up with a little brass telescope"),
    "whisk": ("a fluffy creature with a tail like a straw broom", "sweeping with its broom tail, dust sparkles"),
    "dawnling": ("a fluffy peach-and-gold creature with a crest of sun rays", "stretching up toward the sunrise"),
    "snowdrop": ("a tiny creature with a white snowdrop flower bell as a hood and leaf arms", "ringing its flower bell"),
}

# Wild beasts for the user's own bad habits.
WILD_OWN = {
    "bramble": ("a thorny bramble wolf-cub made of dark purple-black briar twigs with red berries and thorns", "snagging a leaf on its thorns and tugging"),
    "embermaw": ("a lava-rock lizard with charcoal-black skin and glowing orange cracks", "breathing a little puff of sparks"),
    "gloomcap": ("a sulky gremlin with a big dusky purple mushroom cap with pale spots for a head", "sulking with arms crossed under its cap"),
    "sludge": ("a murky teal swamp slime blob wearing a lily pad as a hat", "stretching into a gooey puddle"),
    "rattle": ("a beetle-imp with bone-white armour plates and black joints", "rattling its plates like a maraca"),
    "snarl": ("a little monster made of a tangled knot of bright red yarn with loose threads", "tangling itself up in its own threads"),
    "frostbite": ("a spiky ice imp with pale cyan crystal spikes and frosty breath", "breathing a cloud of frost"),
    "clank": ("a rusty tin-can goblin, rust orange and grey metal with bolts and a dented lid hat", "banging a spoon on its own tin belly"),
    "wailer": ("a lavender ghostly wisp shaped like a little blanket with a stitched mouth", "wailing with its sleeves flapping"),
    "grimble": ("a gremlin with a hood of glossy black feathers and amber eyes", "clutching a stolen shiny button"),
}

# Shy creatures for the user's own good habits.
SHY_OWN = {
    "petal": ("a pink cherry-blossom sprite like a tiny fawn with petals for ears and a blossom tail", "twirling in falling petals"),
    "quill": ("a small ink-blue creature with long quill-feather ears and a white belly", "writing with the tip of its quill ear"),
    "honeybun": ("a round honey-gold fluffball with tiny translucent wings and a striped tail", "carrying a tiny honey pot"),
    "fernlet": ("a creature of curly spring-green fern fronds with a pale belly", "unfurling a fern frond like a stretch"),
    "moonmoth": ("a pale silver fluffy moth-creature with crescent moon wings", "fluttering under a little glowing moon"),
    "tidekin": ("a seafoam-aqua creature wearing a spiral seashell as a hat", "listening to its seashell"),
    "cocoa": ("a cocoa-brown fluffy loaf-shaped creature with white marshmallow ears", "sipping a tiny mug of cocoa"),
    "lumen": ("a small creature with an amethyst crystal body that glows softly from inside", "glowing brightly like a lamp"),
    "plume": ("a sunset-orange fluffy round bird-puff with a long elegant tail plume", "fanning out its tail plume"),
    "mistral": ("a pale-blue cloud-weasel with swirls of wind around it", "riding a little gust of wind"),
}

# Finds: rare guests for milestones.
FIND = {
    "ashling": ("a little chick of warm ember feathers rising from grey ash, glowing softly", "spreading small flaming wings, reborn"),
    "wisp": ("a small will-o'-wisp creature of pale blue flame with a round soft body", "lighting a path with a floating glow"),
    "elder": ("an old gentle forest spirit like a small mossy stag with a lantern hanging from its branch antlers", "bowing its head wisely"),
    "pebble": ("a tiny round stone golem with moss on its head and a small flower", "sitting steady and still as a rock, holding a pebble"),
}

FRAMES = {
    "wild": """1) wild: scruffy, tense and wary, crouched low, hackles up
2) curious: cautious but interested, ears up, one step forward
3) calm: trusting, sitting relaxed, soft eyes
4) calm {sig}
5) happy, cheering, jumping with joy
6) asleep, curled up, peaceful face""",
    "shy": """1) shy: hiding half behind its own paws or tail, only peeking out
2) curious: cautiously stepping forward
3) calm: content and trusting, sitting relaxed
4) calm {sig}
5) happy, cheering, jumping with joy
6) asleep, curled up, peaceful face""",
}


def pet_prompt(kind, desc, sig):
    role = {"wild": "a wild beast that stands for a bad habit and slowly gets tamed",
            "shy": "a shy forest creature that stands for a good habit and slowly comes closer",
            "find": "a rare forest guest that visits for a milestone"}[kind]
    frames = FRAMES["shy" if kind == "find" else kind].format(sig=sig)
    return f"""Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet for a cozy habit app creature, {role}: {desc}.
{STYLE}
{GRID}
Cells, left to right, top to bottom:
{frames}
The same character and palette in every cell, facing three-quarters right."""


SCENE_STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, 3-4 tone shading ramps, "
    "mossy forest palette with warm lantern light."
)
ICON_STYLE = (
    "Style: chunky 16-bit pixel art icons like a cozy indie game's inventory, crisp pixels, clean dark outline, 3-4 tone shading, warm palette. "
    "Plain objects: NO faces, NO eyes."
)

OTHER = {
    "landmarks": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 small landmarks for a hand-drawn adventure map in a cozy forest habit app, each a separate little scene object seen from slightly above, like map icons in a cozy indie RPG.
""" + ICON_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One landmark per cell, centered, filling most of the cell, with clear margin; no text, no grid lines.
Landmarks in order (left to right, top to bottom):
1 a wooden gate with a hanging lantern, 2 a small campfire with logs and a stump seat, 3 a thorny bramble thicket under a small dark storm cloud, 4 a crossroads signpost with three arrows (blank, no text),
5 a wooden rope bridge over a little gap, 6 a murky swamp pool with reeds, 7 a stone spring well with clear water, 8 a cozy roadside tavern with glowing windows,
9 a grassy hilltop with a small red flag, 10 a river ford with stepping stones, 11 a small wooden windmill, 12 a huge old oak tree,
13 a snowy mountain pass, 14 a cozy log lodge on a hill with smoke from the chimney, 15 a patch of soft grey fog, 16 a little canvas tent.""",
    "weather": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 8 weather icons that stand for moods in a cozy forest diary app.
""" + ICON_STYLE + """
Layout: 4 columns x 2 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One icon per cell, centered, filling most of the cell, with clear margin; no text, no grid lines.
Icons in order (left to right, top to bottom):
1 a calm clear sky patch: a small soft cloud resting on a gentle blue circle of sky, 2 a radiant golden sun with bold rays, 3 a white feather floating on a little breeze swirl, 4 a flat grey fog bank of layered mist,
5 a gusty wind swirl carrying a few autumn leaves, 6 a dark storm cloud with a yellow lightning bolt, 7 a blue-grey rain cloud with falling drops, 8 a crescent moon behind a dark purple swirling cloud.""",
    "yard": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy habit app: the yard of a small wooden forest lodge, side-on like a 2D game level, seen from the front. Landscape 3:2.
""" + SCENE_STYLE + """
From LEFT to RIGHT: the dark edge of a fairy-tale forest with big old trees and deep shadows between them (left third), then a sunny grassy yard with a low wooden fence across the middle and three empty raised garden beds of dark soil in front of it, then on the RIGHT the front of a small moss-roofed log lodge with a wide covered porch, three porch steps and a closed round-topped wooden front door, two lanterns on the porch posts.
The SKY above everything must be flat solid pure magenta (#FF00FF) (it will be painted later), no clouds, no sun, no moon.
IMPORTANT: no animals, no creatures, no people, no text. The yard, the porch and the beds must be clear, free space (creatures and plants will be added later).""",
    "house": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a cut-away dollhouse view of the inside of a cozy two-storey wooden forest lodge, side-on like a 2D game, seen from the front, with four rooms. Landscape 3:2.
""" + SCENE_STYLE + """
The four rooms, each a clear open box with a wooden plank floor:
- ground floor LEFT: a small kitchen with a cast-iron stove, a wooden table, shelves with jars;
- ground floor RIGHT (the biggest room): a living room with a big warm stone hearth with a fire and a round woven rug on the floor;
- upper floor LEFT: a bedroom with a patchwork quilt bed and a small round window;
- upper floor RIGHT: an attic study under the sloping roof with a round window, a telescope and a bookshelf.
A wooden staircase connects the floors in the middle. A mossy roof on top. Around the house (sky) flat solid pure magenta (#FF00FF).
IMPORTANT: no animals, no creatures, no people, no text. Every floor must have clear free space in the middle for creatures to be added later.""",
    "clouds": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 6 soft fluffy pixel art clouds for a cozy game sky.
Style: detailed 16-bit pixel art, crisp pixels, soft white and pale blue-grey shading, no outline or a very light one.
Layout: 3 columns x 2 rows of equal cells on a flat solid pure magenta (#FF00FF) background. One cloud per cell, centered, wide and flat, different shapes and sizes; nothing touches the cell borders. No text.""",
    "icons": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 icons for a cozy forest habit app.
""" + ICON_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One icon per cell, centered, filling most of the cell, with clear margin; no text, no grid lines.
Icons in order (left to right, top to bottom):
1 a tiny log cabin with a moss roof and a lit window, 2 an open leather diary with a quill, 3 a big paw print made of moss and bark, 4 a rolled parchment scroll with a red ribbon,
5 a glowing brass oil lantern, 6 an iron gear cog, 7 a small sprouting acorn, 8 a folded treasure map,
9 a brass hourglass, 10 a red heart made of wood, 11 a golden star, 12 a lit candle,
13 a seed packet, 14 a pencil, 15 a small brass bell, 16 a wooden signpost pointing right.""",
    "lodge": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy habit app: a small wooden forest lodge at the edge of a dark fairy-tale forest, side-on like a 2D game level, seen from the front.
""" + STYLE.replace("It is an invented fantasy creature, not a real animal species.", "") + """ Landscape 3:2.
From LEFT to RIGHT: the dark edge of the forest with big old trees (left fifth), then a grassy yard with a low wooden fence and three empty dark-soil garden beds, then a covered wooden porch with steps, then the lodge itself shown as a cut-away dollhouse view: a cosy room with a warm stone hearth with a fire, a round rug on a plank floor.
The SKY above everything must be flat solid pure magenta (#FF00FF) (it will be painted later), no clouds, no sun, no moon.
IMPORTANT: no animals, no creatures, no people, no text. The yard, the porch and the rug must be clear, free space (creatures and plants will be added later). The garden beds are empty soil.""",
    "items": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 cozy decorations for a forest lodge and its yard, each a separate object, seen from the front (side-on like a 2D game).
""" + OBJ_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a wooden rocking chair with a knitted blanket, 2 a lantern on a wooden post, glowing, 3 a wooden bench, 4 a stack of split firewood,
5 a wooden birdhouse on a pole, 6 a small stone well with a bucket, 7 wooden wind chimes hanging, 8 a mailbox on a post,
9 a rope swing on a short frame, 10 a straw scarecrow with a hat (just a straw figure, no face), 11 a potted red geranium, 12 a small bookshelf,
13 a garland of little cloth flags (horizontal), 14 a hammock on two posts, 15 a wooden sign post with arrows (blank, no text), 16 a copper kettle on a little stand.""",
    "props": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 small things for a cozy forest habit app, each separate, front view.
""" + OBJ_STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a single pinecone, 2 a small heap of five pinecones, 3 a tiny green sprout in soil, 4 a young leafy seedling in soil,
5 a tall sunflower in bloom, 6 a big orange pumpkin with leaves, 7 a lavender bush in bloom, 8 a young apple tree with red apples,
9 a blueberry bush with berries, 10 a white cabbage, 11 a pink rose bush, 12 a tomato plant tied to a stick with red tomatoes,
13 a sealed letter with a red wax seal, 14 a small brass hourglass, 15 a small leather notebook with a pencil, 16 a wooden bowl of berries and nuts.""",
}


def image_of(output):
    """The image made in the Codex session named in its output (other sessions may draw at the same time)."""
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(job):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    r = subprocess.run([shutil.which("codex.cmd") or shutil.which("codex") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
    p = image_of((r.stdout or "") + (r.stderr or ""))
    if p is None:
        print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)
        return
    shutil.copy(p, dst)
    print("ok", name, p, flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = list(OTHER.items())
    for kind, table in (("wild", WILD), ("shy", SHY), ("find", FIND), ("wild", WILD_OWN), ("shy", SHY_OWN)):
        jobs += [(k, pet_prompt(kind, *v)) for k, v in table.items()]
    jobs = [j for j in jobs if not want or j[0] in want]
    with ThreadPoolExecutor(3) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()

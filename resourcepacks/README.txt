Blockcraft Resource Packs
=========================
Place custom resource pack folders or .zip archives in this directory.

Supported files inside a pack folder or .zip:
  - pack.properties      : name, description, base=Default|Classic, saturation, contrast
  - atlas.png            : 256x256 or 512x1024 terrain/item atlas image (non-empty 16x16 cells override tiles)
  - tiles/<id>.png       : 16x16 PNG override for tile index <id> (0..2047)
  - tiles/<block>.png    : 16x16 PNG override by block name (e.g. grass_top.png, cobblestone.png, oak_planks.png)
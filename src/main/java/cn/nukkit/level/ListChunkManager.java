package cn.nukkit.level;

import cn.nukkit.api.PowerNukkitOnly;
import cn.nukkit.api.Since;
import cn.nukkit.block.Block;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.level.format.generic.BaseFullChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Since("1.3.2.0-PN")
@SuppressWarnings("deprecation")
public class ListChunkManager implements ChunkManager {

    private final Level level;
    private final List<Block> blocks;

    @Since("1.3.2.0-PN")
    public ListChunkManager(Level parent) {
        this.level = parent;
        this.blocks = new ArrayList<>();
    }
    
    private Optional<Block> findBlockAt(int x, int y, int z, int layer) {
        return this.blocks.stream().filter(block -> block.x == x && block.y == y && block.z == z && block.layer == layer).findAny();
    }

    @Override
    public void setBlockIdAt(int x, int y, int z, int id) {
        this.setBlockIdAt(x, y, z, 0, id);
    }

    @Override
    public void setBlockIdAt(int x, int y, int z, int layer, int id) {
        int blockData = this.getBlockDataAt(x, y, z, layer);
        this.findBlockAt(x, y, z, layer).ifPresent(this.blocks::remove);
        this.blocks.add(Block.get(id, blockData, this.level, x, y, z, layer));
    }

    @Override
    public int getBlockIdAt(int x, int y, int z) {
        return this.getBlockIdAt(x, y, z, 0);
    }
    
    @Override
    public int getBlockIdAt(int x, int y, int z, int layer) {
        return this.findBlockAt(x, y, z, layer).map(Block::getId).orElseGet(() -> this.level.getBlockIdAt(x, y, z, layer));
    }

    @Override
    public void setBlockDataAt(int x, int y, int z, int data) {
        this.setBlockDataAt(x, y, z, 0, data);
    }

    @Override
    public void setBlockDataAt(int x, int y, int z, int layer, int data) {
        int blockId = this.getBlockIdAt(x, y, z, layer);
        this.findBlockAt(x, y, z, layer).ifPresent(this.blocks::remove);
        this.blocks.add(Block.get(blockId, data, this.level, x, y, z, layer));
    }

    @Override
    public int getBlockDataAt(int x, int y, int z) {
        return this.getBlockDataAt(x, y, z, 0);
    }

    @Override
    public int getBlockDataAt(int x, int y, int z, int layer) {
        return this.findBlockAt(x, y, z, layer).map(Block::getDamage).orElseGet(() -> this.level.getBlockDataAt(x, y, z, layer));
    }

    @Override
    public void setBlockFullIdAt(int x, int y, int z, int fullId) {
        this.setBlockFullIdAt(x, y, z, 0, fullId);
    }

    @Override
    public void setBlockFullIdAt(int x, int y, int z, int layer, int fullId) {
        this.findBlockAt(x, y, z, layer).ifPresent(this.blocks::remove);
        this.blocks.add(Block.get(fullId, this.level, x, y, z, layer));
    }

    @Override
    public void setBlockAt(int x, int y, int z, int id, int data) {
        this.setBlockAtLayer(x, y, z, 0, id, data);
    }

    @Override
    public boolean setBlockAtLayer(int x, int y, int z, int layer, int id, int data) {
        this.findBlockAt(x, y, z, layer).ifPresent(this.blocks::remove);
        return this.blocks.add(Block.get(id, data, this.level, x, y, z, layer));
    }

    @Since("1.4.0.0-PN")
    @PowerNukkitOnly
    @Override
    public boolean setBlockStateAt(int x, int y, int z, int layer, BlockState state) {
        this.findBlockAt(x, y, z, layer).ifPresent(this.blocks::remove);
        return this.blocks.add(state.getBlock(this.level, x, y, z, layer));
    }

    @Since("1.4.0.0-PN")
    @PowerNukkitOnly
    @Override
    public BlockState getBlockStateAt(int x, int y, int z, int layer) {
        return this.findBlockAt(x, y, z, layer).map(Block::getCurrentState).orElseGet(() -> this.level.getBlockStateAt(x, y, z, layer));
    }

    @Override
    public BaseFullChunk getChunk(int chunkX, int chunkZ) {
        return this.level.getChunk(chunkX, chunkZ);
    }

    @Override
    public void setChunk(int chunkX, int chunkZ) {
        this.level.setChunk(chunkX, chunkZ);
    }

    @Override
    public void setChunk(int chunkX, int chunkZ, BaseFullChunk chunk) {
        this.level.setChunk(chunkX, chunkZ, chunk);
    }

    @Override
    public long getSeed() {
        return this.level.getSeed();
    }

    @Since("1.3.2.0-PN")
    public List<Block> getBlocks() {
        return this.blocks;
    }

}

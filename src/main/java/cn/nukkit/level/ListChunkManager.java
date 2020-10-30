package cn.nukkit.level;

import cn.nukkit.api.PowerNukkitOnly;
import cn.nukkit.api.Since;
import cn.nukkit.block.Block;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.level.format.generic.BaseFullChunk;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListChunkManager implements ChunkManager {

    private final ChunkManager parent;
    private final List<Block> blocks;

    public ListChunkManager(ChunkManager parent) {
        this.parent = parent;
        this.blocks = new ArrayList<>();
    }

    @Override
    public int getBlockIdAt(int x, int y, int z, int layer) {
        Optional<Block> optionalBlock = this.blocks.stream().filter(block -> block.getFloorX() == x && block.getFloorY() == y && block.getFloorZ() == z && block.layer == layer).findAny();
        return optionalBlock.map(Block::getId).orElseGet(() -> this.parent.getBlockIdAt(x, y, z, layer));
    }

    @Override
    public int getBlockIdAt(int x, int y, int z) {
        return this.getBlockIdAt(x, y, z, 0);
    }

    @Override
    public void setBlockFullIdAt(int x, int y, int z, int layer, int fullId) {
        this.blocks.removeIf(block -> block.getFloorX() == x && block.getFloorY() == y && block.getFloorZ() == z && block.layer == layer);
        this.blocks.add(Block.get(fullId, null, x, y, z, layer));
    }

    @Override
    public void setBlockFullIdAt(int x, int y, int z, int fullId) {
        this.setBlockFullIdAt(x, y, z, 0, fullId);
    }

    @Override
    public void setBlockIdAt(int x, int y, int z, int layer, int id) {
        Optional<Block> optionalBlock = this.blocks.stream().filter(block -> block.getFloorX() == x && block.getFloorY() == y && block.getFloorZ() == z && block.layer == layer).findAny();
        optionalBlock.ifPresent(this.blocks::remove);
        this.blocks.add(BlockState.of(id, optionalBlock.map(Block::getHugeDamage).orElseGet(() -> BigInteger.valueOf(this.getBlockDataAt(x, y, z, layer)))).getBlock(null, x, y, z, layer));
    }

    @Override
    public void setBlockIdAt(int x, int y, int z, int id) {
        this.setBlockIdAt(x, y, z, 0, id);
    }

    @Override
    public boolean setBlockAtLayer(int x, int y, int z, int layer, int id, int data) {
        this.blocks.removeIf(block -> block.getFloorX() == x && block.getFloorY() == y && block.getFloorZ() == z && block.layer == layer);
        return this.blocks.add(BlockState.of(id, data).getBlock(null, x, y, z, layer));
    }

    @Since("1.4.0.0-PN")
    @PowerNukkitOnly
    @Override
    public boolean setBlockStateAt(int x, int y, int z, int layer, BlockState state) {
        this.blocks.removeIf(block -> block.getFloorX() == x && block.getFloorY() == y && block.getFloorZ() == z && block.layer == layer);
        return this.blocks.add(state.getBlock(null, x, y, z, layer));
    }

    @Since("1.4.0.0-PN")
    @PowerNukkitOnly
    @Override
    public BlockState getBlockStateAt(int x, int y, int z, int layer) {
        Optional<Block> optionalBlock = this.blocks.stream().filter(block -> block.getFloorX() == x && block.getFloorY() == y && block.getFloorZ() == z).findAny();
        return optionalBlock.map(Block::getCurrentState).orElseGet(() -> this.parent.getBlockStateAt(x, y, z, layer));
    }

    @Override
    public void setBlockAt(int x, int y, int z, int id, int data) {
        this.setBlockAtLayer(x, y, z, 0, id, data);
    }

    @Override
    public int getBlockDataAt(int x, int y, int z, int layer) {
        Optional<Block> optionalBlock = this.blocks.stream().filter(block -> block.getFloorX() == x && block.getFloorY() == y && block.getFloorZ() == z).findAny();
        return optionalBlock.map(Block::getHugeDamage).orElseGet(() -> BigInteger.valueOf(this.parent.getBlockDataAt(x, y, z, layer))).intValue();
    }

    @Override
    public int getBlockDataAt(int x, int y, int z) {
        return this.getBlockDataAt(x, y, z, 0);
    }

    @Override
    public void setBlockDataAt(int x, int y, int z, int layer, int data) {
        Optional<Block> optionalBlock = this.blocks.stream().filter(block -> block.getFloorX() == x && block.getFloorY() == y && block.getFloorZ() == z && block.layer == layer).findAny();
        optionalBlock.ifPresent(this.blocks::remove);
        this.blocks.add(BlockState.of((int) optionalBlock.map(Block::getId).orElseGet(() -> this.getBlockIdAt(x, y, z, layer)), data).getBlock(null, x, y, z, layer));
    }

    @Override
    public void setBlockDataAt(int x, int y, int z, int data) {
        this.setBlockDataAt(x, y, z, 0, data);
    }

    @Override
    public BaseFullChunk getChunk(int chunkX, int chunkZ) {
        return this.parent.getChunk(chunkX, chunkZ);
    }

    @Override
    public void setChunk(int chunkX, int chunkZ) {
        this.parent.setChunk(chunkX, chunkZ);
    }

    @Override
    public void setChunk(int chunkX, int chunkZ, BaseFullChunk chunk) {
        this.parent.setChunk(chunkX, chunkZ, chunk);
    }

    @Override
    public long getSeed() {
        return this.parent.getSeed();
    }

    public List<Block> getBlocks() {
        return this.blocks;
    }

}

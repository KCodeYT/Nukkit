package cn.nukkit.blockentity;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.CreatureSpawnEvent;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddEntityPacket;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class BlockEntityMobSpawner extends BlockEntitySpawnable {

    private short maxNearbyEntities;
    private short requiredPlayerRange;

    private short spawnCount;
    private short spawnRange;

    private short delay;
    private short minSpawnDelay;
    private short maxSpawnDelay;

    private String entityIdentifier;
    private int entityNetworkId;
    private float displayEntityWidth;
    private float displayEntityHeight;
    private float displayEntityScale;

    public BlockEntityMobSpawner(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    protected void initBlockEntity() {
        if (!this.namedTag.containsShort("MaxNearbyEntities"))
            this.namedTag.putShort("MaxNearbyEntities", 6);
        this.maxNearbyEntities = (short) this.namedTag.getShort("MaxNearbyEntities");

        if (!this.namedTag.containsShort("RequiredPlayerRange"))
            this.namedTag.putShort("RequiredPlayerRange", 16);
        this.requiredPlayerRange = (short) this.namedTag.getShort("RequiredPlayerRange");

        if (!this.namedTag.containsShort("SpawnCount"))
            this.namedTag.putShort("SpawnCount", 4);
        this.spawnCount = (short) this.namedTag.getShort("SpawnCount");

        if (!this.namedTag.containsShort("SpawnRange"))
            this.namedTag.putShort("SpawnRange", 4);
        this.spawnRange = (short) this.namedTag.getShort("SpawnRange");

        if (!this.namedTag.containsShort("Delay"))
            this.namedTag.putShort("Delay", 4);
        this.delay = (short) this.namedTag.getShort("Delay");

        if (!this.namedTag.containsShort("MinSpawnDelay"))
            this.namedTag.putShort("MinSpawnDelay", 200);
        this.minSpawnDelay = (short) this.namedTag.getShort("MinSpawnDelay");

        if (!this.namedTag.containsShort("MaxSpawnDelay"))
            this.namedTag.putShort("MaxSpawnDelay", 800);
        this.maxSpawnDelay = (short) this.namedTag.getShort("MaxSpawnDelay");

        if (!this.namedTag.containsString("EntityIdentifier"))
            this.namedTag.putString("EntityIdentifier", "");
        this.entityIdentifier = this.namedTag.getString("EntityIdentifier");

        if (!this.namedTag.containsFloat("DisplayEntityWidth"))
            this.namedTag.putFloat("DisplayEntityWidth", 1F);
        this.displayEntityWidth = this.namedTag.getFloat("DisplayEntityWidth");

        if (!this.namedTag.containsFloat("DisplayEntityHeight"))
            this.namedTag.putFloat("DisplayEntityHeight", 1F);
        this.displayEntityHeight = this.namedTag.getFloat("DisplayEntityHeight");

        if (!this.namedTag.containsFloat("DisplayEntityScale"))
            this.namedTag.putFloat("DisplayEntityScale", 1F);
        this.displayEntityScale = this.namedTag.getFloat("DisplayEntityScale");

        this.entityNetworkId = AddEntityPacket.LEGACY_IDS.entrySet().stream().filter(entry -> entry.getValue().equals(this.entityIdentifier)).map(Map.Entry::getKey).findAny().orElse(-1);

        this.scheduleUpdate();
        super.initBlockEntity();
    }

    @Override
    public boolean isBlockEntityValid() {
        return this.getBlock().getId() == BlockID.MONSTER_SPAWNER;
    }

    public void setDirty() {
        this.spawnToAll();
        super.setDirty();
    }

    @Override
    public boolean onUpdate() {
        if (this.closed)
            return false;
        if (!this.level.getServer().getPropertyBoolean("spawn-mobs"))
            return true;

        Random random = ThreadLocalRandom.current();
        if (this.delay < 0)
            this.setDelay(this.rand(random, this.minSpawnDelay, this.maxSpawnDelay));
        if (this.delay-- < 0) {
            this.delay = this.rand(random, this.minSpawnDelay, this.maxSpawnDelay);
            if (this.entityNetworkId == -1)
                return true;
            AxisAlignedBB boundingBox = new SimpleAxisAlignedBB(
                    this.subtract(this.spawnRange, 1, this.spawnRange),
                    this.add(this.spawnRange, 1, this.spawnRange)
            );
            List<Block> validBlocks = this.level.scanBlocks(boundingBox, (pos, state) ->
                    this.level.getBlock(pos.asVector3()).canPassThrough() &&
                            this.level.getBlock(pos.add(0, 1).asVector3()).canPassThrough());
            if (validBlocks.isEmpty())
                return true;
            short spawnCount = this.rand(random, (short) 1, this.spawnCount);
            for (short spawnI = 0; spawnI < spawnCount; spawnI++) {
                List<Entity> entities = Arrays.stream(this.level.getEntities()).
                        filter(entity -> entity.distance(this) <= this.requiredPlayerRange && entity.getNetworkId() == this.entityNetworkId).
                        collect(Collectors.toList());
                if (this.level.getPlayers().values().stream().anyMatch(player -> player.distance(this) <= this.requiredPlayerRange) && entities.size() <= this.maxNearbyEntities) {
                    Position validPos = validBlocks.get(random.nextInt(validBlocks.size())).add(0.5, 0, 0.5);
                    CreatureSpawnEvent ev = new CreatureSpawnEvent(this.entityNetworkId, validPos, new CompoundTag(), CreatureSpawnEvent.SpawnReason.SPAWNER);
                    this.level.getServer().getPluginManager().callEvent(ev);
                    if (ev.isCancelled())
                        continue;
                    Entity entity = Entity.createEntity(this.entityNetworkId, ev.getPosition());
                    if (entity != null)
                        entity.spawnToAll();
                }
            }
        }
        return true;
    }

    private short rand(Random random, short min, short max) {
        return (short) (random.nextInt(max - min + 1) + min);
    }

    public short getMaxNearbyEntities() {
        return this.maxNearbyEntities;
    }

    public void setMaxNearbyEntities(short maxNearbyEntities) {
        this.maxNearbyEntities = maxNearbyEntities;
        this.namedTag.putShort("MaxNearbyEntities", maxNearbyEntities);
        this.setDirty();
    }

    public short getRequiredPlayerRange() {
        return this.requiredPlayerRange;
    }

    public void setRequiredPlayerRange(short requiredPlayerRange) {
        this.requiredPlayerRange = requiredPlayerRange;
        this.namedTag.putShort("RequiredPlayerRange", requiredPlayerRange);
        this.setDirty();
    }

    public short getSpawnCount() {
        return this.spawnCount;
    }

    public void setSpawnCount(short spawnCount) {
        this.spawnCount = spawnCount;
        this.namedTag.putShort("SpawnCount", spawnCount);
        this.setDirty();
    }

    public short getSpawnRange() {
        return this.spawnRange;
    }

    public void setSpawnRange(short spawnRange) {
        this.spawnRange = spawnRange;
        this.namedTag.putShort("SpawnRange", spawnRange);
        this.setDirty();
    }

    public short getDelay() {
        return this.delay;
    }

    public void setDelay(short delay) {
        this.delay = delay;
        this.namedTag.putShort("Delay", delay);
        this.setDirty();
    }

    public short getMinSpawnDelay() {
        return this.minSpawnDelay;
    }

    public void setMinSpawnDelay(short minSpawnDelay) {
        this.minSpawnDelay = minSpawnDelay;
        this.namedTag.putShort("MinSpawnDelay", minSpawnDelay);
        this.setDirty();
    }

    public short getMaxSpawnDelay() {
        return this.maxSpawnDelay;
    }

    public void setMaxSpawnDelay(short maxSpawnDelay) {
        this.maxSpawnDelay = maxSpawnDelay;
        this.namedTag.putShort("MaxSpawnDelay", maxSpawnDelay);
        this.setDirty();
    }

    public String getEntityIdentifier() {
        return this.entityIdentifier;
    }

    public void setEntityIdentifier(String entityIdentifier) {
        this.entityIdentifier = entityIdentifier;
        this.entityNetworkId = AddEntityPacket.LEGACY_IDS.entrySet().stream().filter(entry -> entry.getValue().equals(this.entityIdentifier)).map(Map.Entry::getKey).findAny().orElse(-1);
        this.namedTag.putString("EntityIdentifier", entityIdentifier);
        this.setDirty();
    }

    public float getDisplayEntityWidth() {
        return this.displayEntityWidth;
    }

    public void setDisplayEntityWidth(float displayEntityWidth) {
        this.displayEntityWidth = displayEntityWidth;
        this.namedTag.putFloat("DisplayEntityWidth", displayEntityWidth);
        this.setDirty();
    }

    public float getDisplayEntityHeight() {
        return this.displayEntityHeight;
    }

    public void setDisplayEntityHeight(float displayEntityHeight) {
        this.displayEntityHeight = displayEntityHeight;
        this.namedTag.putFloat("DisplayEntityHeight", displayEntityHeight);
        this.setDirty();
    }

    public float getDisplayEntityScale() {
        return this.displayEntityScale;
    }

    public void setDisplayEntityScale(float displayEntityScale) {
        this.displayEntityScale = displayEntityScale;
        this.namedTag.putFloat("DisplayEntityScale", displayEntityScale);
        this.setDirty();
    }

}

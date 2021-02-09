package cn.nukkit.blockentity;

import cn.nukkit.block.Block;
import cn.nukkit.blockstate.BlockState;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.projectile.EntityEnderPearl;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.player.PlayerTeleportEvent.TeleportCause;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.BlockEventPacket;

import static cn.nukkit.block.BlockID.BEDROCK;

/**
 * @author GoodLucky777
 */
public class BlockEntityEndGateway extends BlockEntitySpawnable {

    private static final BlockState STATE_BEDROCK = BlockState.of(BEDROCK);
    private static final BlockVector3 DEFAULT_EXIT_PORTAL = new BlockVector3(0, 0, 0);

    private int age;
    private BlockVector3 exitPortal;
    public int teleportCooldown;

    public BlockEntityEndGateway(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    protected void initBlockEntity() {
        if (this.namedTag.contains("Age")) {
            this.age = this.namedTag.getInt("Age");
        } else {
            this.age = 0;
        }

        if (this.namedTag.contains("ExitPortal")) {
            ListTag<IntTag> exitPortalList = this.namedTag.getList("ExitPortal", IntTag.class);
            this.exitPortal = new BlockVector3(exitPortalList.get(0).data, exitPortalList.get(1).data, exitPortalList.get(2).data);
        } else {
            this.exitPortal = DEFAULT_EXIT_PORTAL.clone();
        }

        this.teleportCooldown = 0;

        super.initBlockEntity();

        scheduleUpdate();
    }

    @Override
    public boolean isBlockEntityValid() {
        return this.getLevel().getBlockIdAt(getFloorX(), getFloorY(), getFloorZ()) == Block.END_GATEWAY;
    }

    @Override
    public void saveNBT() {
        super.saveNBT();

        this.namedTag.putInt("Age", this.age);
        this.namedTag.putList(new ListTag<IntTag>("ExitPortal")
                .add(new IntTag("0", this.exitPortal.x))
                .add(new IntTag("1", this.exitPortal.y))
                .add(new IntTag("2", this.exitPortal.z))
        );
    }

    @Override
    public boolean onUpdate() {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean isGenerated = isGenerating();

        this.age++;

        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
            if (this.teleportCooldown == 0) {
                setDirty();
                this.spawnToAll();
            }
        } else {
            if (this.age % 2400 == 0) {
                this.setTeleportCooldown();
            }
        }

        if (isGenerated != isGenerating()) {
            setDirty();
            this.spawnToAll();
        }

        this.timing.stopTiming();

        return true;
    }

    public void teleportEntity(Entity entity) {
        if (this.exitPortal != null) {
            if (entity instanceof EntityEnderPearl) {
                if (((EntityProjectile) entity).shootingEntity != null) {
                    ((EntityProjectile) entity).shootingEntity.teleport(getSafeExitPortal().asVector3().add(0.5, 0, 0.5), TeleportCause.END_GATEWAY);
                    entity.close();
                } else {
                    entity.teleport(getSafeExitPortal().asVector3().add(0.5, 0, 0.5), TeleportCause.END_GATEWAY);
                }
            } else {
                entity.teleport(getSafeExitPortal().asVector3().add(0.5, 0, 0.5), TeleportCause.END_GATEWAY);
            }
        }

        setTeleportCooldown();
    }

    public BlockVector3 getSafeExitPortal() {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                int chunkX = (this.exitPortal.getX() >> 4) + x;
                int chunkZ = (this.exitPortal.getZ() >> 4) + z;
                FullChunk chunk = this.getLevel().getChunk(chunkX, chunkZ, false);
                if (chunk == null || !(chunk.isGenerated() || chunk.isPopulated())) {
                    this.getLevel().generateChunk(chunkX, chunkZ, true);
                }
            }
        }

        for (int x = this.exitPortal.getX() - 5; x <= this.exitPortal.getX() + 5; x++) {
            for (int z = this.exitPortal.getZ() - 5; z <= this.exitPortal.getZ() + 5; z++) {
                for (int y = 255; y > Math.max(0, this.exitPortal.getY() + 2); y--) {
                    if (!this.getLevel().getBlockStateAt(x, y, z).equals(BlockState.AIR)) {
                        if (!this.getLevel().getBlockStateAt(x, y, z).equals(STATE_BEDROCK)) {
                            return new BlockVector3(x, y + 1, z);
                        }
                    }
                }
            }
        }

        return this.exitPortal.up(2);
    }

    public int getAge() {
        return this.age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public BlockVector3 getExitPortal() {
        return this.exitPortal;
    }

    public void setExitPortal(BlockVector3 exitPortal) {
        this.exitPortal = exitPortal;
    }

    public boolean isGenerating() {
        return this.age < 200;
    }

    public boolean isTeleportCooldown() {
        return this.teleportCooldown > 0;
    }

    public void setTeleportCooldown() {
        this.setTeleportCooldown(40);
    }

    public void setTeleportCooldown(int teleportCooldown) {
        this.teleportCooldown = teleportCooldown;
        setDirty();
        sendBlockEventPacket(0);
        this.spawnToAll();
    }

    private void sendBlockEventPacket(int eventData) {
        if (this.closed) {
            return;
        }

        if (this.getLevel() == null) {
            return;
        }

        BlockEventPacket pk = new BlockEventPacket();
        pk.x = this.getFloorX();
        pk.y = this.getFloorY();
        pk.z = this.getFloorZ();
        pk.case1 = 1;
        pk.case2 = eventData;
        this.getLevel().addChunkPacket(this.getChunkX(), this.getChunkZ(), pk);
    }

}

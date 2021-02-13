package cn.nukkit.entity.item;

import cn.nukkit.Player;
import cn.nukkit.api.DeprecationDetails;
import cn.nukkit.api.PowerNukkitDifference;
import cn.nukkit.api.PowerNukkitOnly;
import cn.nukkit.api.Since;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWater;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.data.ByteEntityData;
import cn.nukkit.entity.data.FloatEntityData;
import cn.nukkit.entity.passive.EntityWaterAnimal;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.vehicle.VehicleMoveEvent;
import cn.nukkit.event.vehicle.VehicleUpdateEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Location;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector3;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AnimatePacket;
import cn.nukkit.network.protocol.SetEntityLinkPacket;

import java.util.ArrayList;

/**
 * @author yescallop
 * @since 2016/2/13
 */
public class EntityBoat extends EntityVehicle {

    public static final int NETWORK_ID = 90;

    @Deprecated @DeprecationDetails(since = "1.3.2.0-PN", by = "Cloudburst Nukkit", 
            reason = "Was removed because it is already defined in Entity.DATA_VARIANT",
            replaceWith = "Entity.DATA_VARIANT")
    @PowerNukkitOnly public static final int DATA_WOOD_ID = 20;
    
    public static final Vector3f RIDER_PLAYER_OFFSET = new Vector3f(0, 1.02001f, 0);
    public static final Vector3f RIDER_OFFSET = new Vector3f(0, -0.2f, 0);

    public static final Vector3f PASSENGER_OFFSET = new Vector3f(-0.6f);
    public static final Vector3f RIDER_PASSENGER_OFFSET = new Vector3f(0.2f);

    public static final int RIDER_INDEX = 0;
    public static final int PASSENGER_INDEX = 1;

    public static final double SINKING_DEPTH = 0.07;
    public static final double SINKING_SPEED = 0.0005;
    public static final double SINKING_MAX_SPEED = 0.005;

    protected boolean sinking = true;
    
    @Deprecated
    @DeprecationDetails(since = "1.3.2.0-PN", by = "PowerNukkit", 
            reason = "Unreliable direct field access", replaceWith = "getVariant(), setVariant(int)")
    @Since("1.3.2.0-PN") public int woodID;

    public EntityBoat(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.setMaxHealth(40);
        this.setHealth(40);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        if (this.namedTag.contains("Variant")) {
            woodID = this.namedTag.getInt("Variant");
        } else if (this.namedTag.contains("woodID")) {
            woodID = this.namedTag.getByte("woodID");
        }

        this.dataProperties.putInt(DATA_VARIANT, woodID);
    }

    @Override
    public float getHeight() {
        return 0.455f;
    }

    @Override
    public float getWidth() {
        return 1.4f;
    }

    @Override
    protected float getDrag() {
        return 0.1f;
    }

    @Override
    protected float getGravity() {
        return 0.03999999910593033F;
    }

    @Override
    public float getBaseOffset() {
        return 0.375F;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public String getInteractButtonText() {
        return "action.interact.ride.boat";
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (invulnerable) {
            return false;
        } else {
            source.setDamage(source.getDamage() * 2);

            boolean attack = super.attack(source);

            if (isAlive()) {
                performHurtAnimation();
            }

            return attack;
        }
    }

    @Override
    public void close() {
        super.close();

        for (Entity linkedEntity : this.passengers) {
            linkedEntity.riding = null;
        }
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }

        this.lastUpdate = currentTick;

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (this.isAlive()) {
            super.onUpdate(currentTick);

            double waterDiff = getWaterLevel();
            if (!hasControllingPassenger()) {

                if (waterDiff > SINKING_DEPTH && !sinking) {
                    sinking = true;
                } else if (waterDiff < -SINKING_DEPTH && sinking) {
                    sinking = false;
                }

                if (waterDiff < -SINKING_DEPTH) {
                    this.motionY = Math.min(0.05, this.motionY + 0.005);
                } else if (waterDiff < 0 || !sinking) {
                    this.motionY = this.motionY > SINKING_MAX_SPEED ? Math.max(this.motionY - 0.02, SINKING_MAX_SPEED) : this.motionY + SINKING_SPEED;
//                    this.motionY = this.motionY + SINKING_SPEED > SINKING_MAX_SPEED ? this.motionY - SINKING_SPEED : this.motionY + SINKING_SPEED;
                }
            }

            if (this.checkObstruction(this.x, this.y, this.z)) {
                hasUpdate = true;
            }

            this.move(this.motionX, this.motionY, this.motionZ);

            double friction = 1 - this.getDrag();

            if (this.onGround && (Math.abs(this.motionX) > 0.00001 || Math.abs(this.motionZ) > 0.00001)) {
                friction *= this.getLevel().getBlock(this.temporalVector.setComponents((int) Math.floor(this.x), (int) Math.floor(this.y - 1), (int) Math.floor(this.z) - 1)).getFrictionFactor();
            }

            this.motionX *= friction;

            if (!hasControllingPassenger()) {
                if (waterDiff > SINKING_DEPTH || sinking) {
                    this.motionY = waterDiff > 0.5 ? this.motionY - this.getGravity() : (this.motionY - SINKING_SPEED < -SINKING_MAX_SPEED ? this.motionY : this.motionY - SINKING_SPEED);
                }
            }

            this.motionZ *= friction;

            Location from = new Location(lastX, lastY, lastZ, lastYaw, lastPitch, level);
            Location to = new Location(this.x, this.y, this.z, this.yaw, this.pitch, level);

            this.getServer().getPluginManager().callEvent(new VehicleUpdateEvent(this));

            if (!from.equals(to)) {
                this.getServer().getPluginManager().callEvent(new VehicleMoveEvent(this, from, to));
            }

            //TODO: lily pad collision
            this.updateMovement();

            if (this.passengers.size() < 2) {
                for (Entity entity : this.level.getCollidingEntities(this.boundingBox.grow(0.20000000298023224, 0.0D, 0.20000000298023224), this)) {
                    if (entity.riding != null || !(entity instanceof EntityLiving) || entity instanceof Player || entity instanceof EntityWaterAnimal || isPassenger(entity)) {
                        continue;
                    }

                    this.mountEntity(entity);

                    if (this.passengers.size() >= 2) {
                        break;
                    }
                }
            }
        }

        return hasUpdate || !this.onGround || Math.abs(this.motionX) > 0.00001 || Math.abs(this.motionY) > 0.00001 || Math.abs(this.motionZ) > 0.00001;
    }

    public void updatePassengers() {
        updatePassengers(false);
    }

    public void updatePassengers(boolean sendLinks) {
        if (this.passengers.isEmpty()) {
            return;
        }

        for (Entity passenger : new ArrayList<>(passengers)) {
            if (!passenger.isAlive()) {
                dismountEntity(passenger);
            }
        }

        if (passengers.size() == 1) {
            Entity passenger = this.passengers.get(0);
            passenger.setSeatPosition(getMountedOffset(passenger));
            super.updatePassengerPosition(passenger);

            if (sendLinks) {
                broadcastLinkPacket(passenger, SetEntityLinkPacket.TYPE_RIDE);
                passenger.setDataProperty(new ByteEntityData(DATA_RIDER_ROTATION_LOCKED, 1));
                passenger.setDataProperty(new FloatEntityData(DATA_RIDER_MAX_ROTATION, 90));
                passenger.setDataProperty(new FloatEntityData(DATA_RIDER_MIN_ROTATION, -90));
            }
        } else if (passengers.size() == 2) {
            Entity firstPassenger = this.passengers.get(0);
            Entity secondPassenger = this.passengers.get(1);
            if (!(firstPassenger instanceof Player)) { //swap
                if (secondPassenger instanceof Player) {
                    this.passengers.set(0, secondPassenger);
                    this.passengers.set(1, firstPassenger);

                    Entity tempPassenger = firstPassenger;
                    firstPassenger = secondPassenger;
                    secondPassenger = tempPassenger;
                }
            }

            firstPassenger.setSeatPosition(getMountedOffset(firstPassenger));
            super.updatePassengerPosition(firstPassenger);
            if (sendLinks) {
                broadcastLinkPacket(firstPassenger, SetEntityLinkPacket.TYPE_RIDE);
                firstPassenger.setDataProperty(new ByteEntityData(DATA_RIDER_ROTATION_LOCKED, 1));
                firstPassenger.setDataProperty(new FloatEntityData(DATA_RIDER_MAX_ROTATION, 90));
                firstPassenger.setDataProperty(new FloatEntityData(DATA_RIDER_MIN_ROTATION, -90));
            }

            secondPassenger.setSeatPosition(getMountedOffset(secondPassenger).add(PASSENGER_OFFSET));

            super.updatePassengerPosition(secondPassenger);

            if (sendLinks) {
                broadcastLinkPacket(secondPassenger, SetEntityLinkPacket.TYPE_PASSENGER);
                secondPassenger.setDataProperty(new ByteEntityData(DATA_RIDER_ROTATION_LOCKED, 1));
                secondPassenger.setDataProperty(new FloatEntityData(DATA_RIDER_MAX_ROTATION, 90));
                secondPassenger.setDataProperty(new FloatEntityData(DATA_RIDER_MIN_ROTATION, -90));
            }
        } else {
            for (Entity passenger : passengers) {
                super.updatePassengerPosition(passenger);
            }
        }
    }

    public double getWaterLevel() {
        double maxY = this.boundingBox.getMinY() + getBaseOffset();
        AxisAlignedBB.BBConsumer<Double> consumer = new AxisAlignedBB.BBConsumer<Double>() {

            private double diffY = Double.MAX_VALUE;

            @Override
            public void accept(int x, int y, int z) {
                Block block = EntityBoat.this.level.getBlock(EntityBoat.this.temporalVector.setComponents(x, y, z));

                if (block instanceof BlockWater) {
                    double level = block.getMaxY();

                    diffY = Math.min(maxY - level, diffY);
                }
            }

            @Override
            public Double get() {
                return diffY;
            }
        };

        this.boundingBox.forEach(consumer);

        return consumer.get();
    }

    @Override
    public boolean mountEntity(Entity entity) {
        Block blockAbove = this.getLevel().getBlock(this.add(0, this.getHeight(), 0));
        if((blockAbove instanceof BlockWater || blockAbove.getId() == Block.WATER || blockAbove.getId() == Block.STILL_WATER) && entity.riding == null)
            return false;
        boolean player = this.passengers.size() >= 1 && this.passengers.get(0) instanceof Player;
        byte mode = SetEntityLinkPacket.TYPE_PASSENGER;

        if (!player && (entity instanceof Player || this.passengers.size() == 0)) {
            mode = SetEntityLinkPacket.TYPE_RIDE;
        }

        boolean r = super.mountEntity(entity, mode);

        if (entity.riding != null) {
            updatePassengers(true);

            entity.setDataProperty(new ByteEntityData(DATA_RIDER_ROTATION_LOCKED, 1));
            entity.setDataProperty(new FloatEntityData(DATA_RIDER_MAX_ROTATION, 90));
            entity.setDataProperty(new FloatEntityData(DATA_RIDER_MIN_ROTATION, -90));

            //            if(entity instanceof Player && mode == SetEntityLinkPacket.TYPE_RIDE){ //TODO: controlling?
//                entity.setDataProperty(new ByteEntityData(DATA_FLAG_WASD_CONTROLLED))
//            }
        }

        return r;
    }

    @Override
    protected void updatePassengerPosition(Entity passenger) {
        updatePassengers();
    }

    @Override
    public boolean dismountEntity(Entity entity) {
        boolean r = super.dismountEntity(entity);

        updatePassengers(true);
        entity.setDataProperty(new ByteEntityData(DATA_RIDER_ROTATION_LOCKED, 0));
        if(entity instanceof Player)
            ((Player) entity).resetInAirTicks();

        return r;
    }

    @Override
    public boolean isControlling(Entity entity) {
        return entity instanceof Player && this.passengers.indexOf(entity) == 0;
    }

    @Override
    public boolean onInteract(Player player, Item item, Vector3 clickedPos) {
        if (this.passengers.size() >= 2) {
            return false;
        }

        this.mountEntity(player);
        return super.onInteract(player, item, clickedPos);
    }

    @Override
    public Vector3f getMountedOffset(Entity entity) {
        return entity instanceof Player ? RIDER_PLAYER_OFFSET : RIDER_OFFSET;
    }

    public void onPaddle(AnimatePacket.Action animation, float value) {
        int propertyId = animation == AnimatePacket.Action.ROW_RIGHT ? DATA_PADDLE_TIME_RIGHT : DATA_PADDLE_TIME_LEFT;

        if (getDataPropertyFloat(propertyId) != value) {
            this.setDataProperty(new FloatEntityData(propertyId, value));
        }
    }

    @Override
    public void applyEntityCollision(Entity entity) {
        if (this.riding == null && entity.riding != this && !entity.passengers.contains(this)) {
            if (!entity.boundingBox.intersectsWith(this.boundingBox.grow(0.20000000298023224, -0.1, 0.20000000298023224))
                    || entity instanceof Player && ((Player) entity).isSpectator()) {
                return;
            }

            double diffX = entity.x - this.x;
            double diffZ = entity.z - this.z;

            double direction = NukkitMath.getDirection(diffX, diffZ);

            if (direction >= 0.009999999776482582D) {
                direction = Math.sqrt(direction);
                diffX /= direction;
                diffZ /= direction;

                double d3 = Math.min(1 / direction, 1);

                diffX *= d3;
                diffZ *= d3;
                diffX *= 0.05000000074505806;
                diffZ *= 0.05000000074505806;
                diffX *= 1 + entityCollisionReduction;

                if (this.riding == null) {
                    motionX -= diffX;
                    motionZ -= diffZ;
                }
            }
        }
    }

    @Override
    public boolean canPassThrough() {
        return false;
    }

    @PowerNukkitDifference(info = "Fixes a dupe issue when attacking too quickly", since = "1.3.1.2-PN")
    @Override
    public void kill() {
        if (!isAlive()) {
            return;
        }
        super.kill();

        if (level.getGameRules().getBoolean(GameRule.DO_ENTITY_DROPS)) {
            this.level.dropItem(this, Item.get(ItemID.BOAT, this.woodID));
        }
    }

    @Override
    public void saveNBT() {
        super.saveNBT();
        this.namedTag.putInt("Variant", this.woodID); // Correct way in Bedrock Edition
        this.namedTag.putByte("woodID", this.woodID); // Compatibility with Cloudburst Nukkit
    }

    @PowerNukkitOnly
    @Since("1.3.2.0-PN")
    public int getVariant() {
        return this.woodID;
    }

    @PowerNukkitOnly
    @Since("1.3.2.0-PN")
    public void setVariant(int variant) {
        this.woodID = variant;
        this.dataProperties.putInt(DATA_VARIANT, variant);
    }
}

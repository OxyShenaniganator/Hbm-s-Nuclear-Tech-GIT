package com.hbm.entity.logic;

import com.hbm.config.BombConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.entity.effect.EntityDrying;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.entity.effect.EntityFalloutUnderGround;
import com.hbm.entity.effect.EntityRainDrop;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeRayBatched;
import com.hbm.main.MainRegistry;
import com.hbm.saveddata.RadiationSavedData;
import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.List;

public class EntityNukeExplosionMK5 extends Entity implements IChunkLoader {
	// Strength of the blast
	public int strength;
	// How many rays are calculated per tick
	public int speed;
	public int length;

	public boolean mute = false;

	public boolean fallout = true;
	private boolean floodPlease = false;
	private int falloutAdd = 0;
	private Ticket loaderTicket;

	ExplosionNukeRayBatched explosion;
	EntityFalloutUnderGround falloutBall;
	EntityDrying dryingBomb;
	EntityFalloutRain falloutRain;
	EntityRainDrop rainDrop;
	EntityDrying waterBomb;

	public EntityNukeExplosionMK5(World p_i1582_1_) {
		super(p_i1582_1_);
	}

	public EntityNukeExplosionMK5(World world, int strength, int speed, int length) {
		super(world);
		this.strength = strength;
		this.speed = speed;
		this.length = length;
	}

	@Override
	public void onUpdate() {
		if(strength == 0) {
			this.setDead();
			return;
		}

		if(!world.isRemote && fallout && explosion != null && falloutRain == null) {
			RadiationSavedData.getData(world);

			// float radMax = (float) (length / 2F * Math.pow(length, 2) / 35F);
			float radMax = Math.min((float) (length * Math.pow(length, 1.5) * 17.5F), 1500000);
			// System.out.println(radMax);
			float rad = radMax / 4F;
			RadiationSavedData.incrementRad(world, this.getPosition(), rad, radMax);
		}

		if(!mute) {
			this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.AMBIENT, 10000.0F, 0.8F + this.rand.nextFloat() * 0.2F);
			if(rand.nextInt(5) == 0)
				this.world.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 10000.0F, 0.8F + this.rand.nextFloat() * 0.2F);
		}
		ExplosionNukeGeneric.dealDamage(this.world, this.posX, this.posY, this.posZ, this.length * 2);

		if(dryingBomb == null){
			dryingBomb = new EntityDrying(this.world);
			dryingBomb.posX = this.posX;
			dryingBomb.posY = this.posY;
			dryingBomb.posZ = this.posZ;
			dryingBomb.setScale(this.length +16);
			this.world.spawnEntity(dryingBomb);
		}
		if(dryingBomb.done){

			if(explosion == null) {

				explosion = new ExplosionNukeRayBatched(world, (int)this.posX, (int)this.posY, (int)this.posZ, this.strength, this.speed, this.length);
			}
			if(!explosion.isAusf3Completed) {
				explosion.collectTip(speed * 10);
			} else if(explosion.perChunk.size() > 0) {
				long start = System.currentTimeMillis();

				while(explosion.perChunk.size() > 0 && System.currentTimeMillis() < start + BombConfig.mk5) explosion.processChunk();
			} else if(fallout) {
				if(falloutBall == null){
					falloutBall = new EntityFalloutUnderGround(this.world);
					falloutBall.posX = this.posX;
					falloutBall.posY = this.posY;
					falloutBall.posZ = this.posZ;
					falloutBall.setScale((int) (this.length * (BombConfig.falloutRange / 100) + falloutAdd));
					this.world.spawnEntity(falloutBall);
				}
				if(falloutBall.done){
					if(floodPlease){
						if(waterBomb == null){
							waterBomb = new EntityDrying(this.world);
							waterBomb.posX = this.posX;
							waterBomb.posY = this.posY;
							waterBomb.posZ = this.posZ;
							waterBomb.dryingmode = false;
							waterBomb.setScale(this.length +18);
							this.world.spawnEntity(waterBomb);
						} else if(waterBomb.done){
							falloutRain = new EntityFalloutRain(this.world);
							falloutRain.posX = this.posX;
							falloutRain.posY = this.posY;
							falloutRain.posZ = this.posZ;
							falloutRain.setScale((int) (this.length * (1F+(BombConfig.falloutRange / 100)) + falloutAdd));
							this.world.spawnEntity(falloutRain);
							this.setDead();

						}
					} else {
						falloutRain = new EntityFalloutRain(this.world);
						falloutRain.posX = this.posX;
						falloutRain.posY = this.posY;
						falloutRain.posZ = this.posZ;
						falloutRain.setScale((int) (this.length * (1F+(BombConfig.falloutRange / 100)) + falloutAdd));
						this.world.spawnEntity(falloutRain);
						this.setDead();
					}
				}
			} else {
				if(floodPlease){
					if(waterBomb == null){
						waterBomb = new EntityDrying(this.world);
						waterBomb.posX = this.posX;
						waterBomb.posY = this.posY;
						waterBomb.posZ = this.posZ;
						waterBomb.dryingmode = false;
						waterBomb.setScale(this.length +18);
						this.world.spawnEntity(waterBomb);
					} else if(waterBomb.done){
						rainDrop = new EntityRainDrop(this.world);
						rainDrop.posX = this.posX;
						rainDrop.posY = this.posY;
						rainDrop.posZ = this.posZ;
						rainDrop.setScale((int)this.length +16);
						this.world.spawnEntity(rainDrop);
						this.setDead();
					}
				} else {
					rainDrop = new EntityRainDrop(this.world);
					rainDrop.posX = this.posX;
					rainDrop.posY = this.posY;
					rainDrop.posZ = this.posZ;
					rainDrop.setScale((int)this.length +16);
					this.world.spawnEntity(rainDrop);
					this.setDead();
				}
			}
		}
	}

	@Override
	protected void entityInit() {
		init(ForgeChunkManager.requestTicket(MainRegistry.instance, world, Type.ENTITY));
	}

	@Override
	public void init(Ticket ticket) {
		if(!world.isRemote) {
			
            if(ticket != null) {
            	
                if(loaderTicket == null) {
                	
                	loaderTicket = ticket;
                	loaderTicket.bindEntity(this);
                	loaderTicket.getModData();
                }

                ForgeChunkManager.forceChunk(loaderTicket, new ChunkPos(chunkCoordX, chunkCoordZ));
            }
        }
	}

	List<ChunkPos> loadedChunks = new ArrayList<ChunkPos>();
	@Override
	public void loadNeighboringChunks(int newChunkX, int newChunkZ) {
		if(!world.isRemote && loaderTicket != null)
        {
            for(ChunkPos chunk : loadedChunks)
            {
                ForgeChunkManager.unforceChunk(loaderTicket, chunk);
            }

            loadedChunks.clear();
            loadedChunks.add(new ChunkPos(newChunkX, newChunkZ));
            loadedChunks.add(new ChunkPos(newChunkX + 1, newChunkZ + 1));
            loadedChunks.add(new ChunkPos(newChunkX - 1, newChunkZ - 1));
            loadedChunks.add(new ChunkPos(newChunkX + 1, newChunkZ - 1));
            loadedChunks.add(new ChunkPos(newChunkX - 1, newChunkZ + 1));
            loadedChunks.add(new ChunkPos(newChunkX + 1, newChunkZ));
            loadedChunks.add(new ChunkPos(newChunkX, newChunkZ + 1));
            loadedChunks.add(new ChunkPos(newChunkX - 1, newChunkZ));
            loadedChunks.add(new ChunkPos(newChunkX, newChunkZ - 1));

            for(ChunkPos chunk : loadedChunks)
            {
                ForgeChunkManager.forceChunk(loaderTicket, chunk);
            }
        }
	}

	private static boolean isWet(World world, BlockPos pos){
		Biome b = world.getBiome(pos);
		return b.getTempCategory() == Biome.TempCategory.OCEAN || b.isHighHumidity() || b == Biomes.BEACH || b == Biomes.OCEAN || b == Biomes.RIVER  || b == Biomes.DEEP_OCEAN || b == Biomes.FROZEN_OCEAN || b == Biomes.FROZEN_RIVER || b == Biomes.STONE_BEACH || b == Biomes.SWAMPLAND;
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {

	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {

	}

	public static EntityNukeExplosionMK5 statFac(World world, int r, double x, double y, double z) {
		if(GeneralConfig.enableExtendedLogging && !world.isRemote)
			MainRegistry.logger.log(Level.INFO, "[NUKE] Initialized eX explosion at " + x + " / " + y + " / " + z + " with radius " + r + "!");

		if(r == 0)
			r = 25;

		r *= 2;

		EntityNukeExplosionMK5 mk5 = new EntityNukeExplosionMK5(world);
		mk5.strength = (int)(r);
		mk5.speed = (int)Math.ceil(100000 / mk5.strength);
		mk5.setPosition(x, y, z);
		mk5.length = mk5.strength / 2;
		return mk5;
	}

	public static EntityNukeExplosionMK5 statFacExperimental(World world, int r, double x, double y, double z) {

		if(GeneralConfig.enableExtendedLogging && !world.isRemote)
			MainRegistry.logger.log(Level.INFO, "[NUKE] Initialized eX explosion at " + x + " / " + y + " / " + z + " with radius " + r + "!");

		if(r == 0)
			r = 25;

		r *= 2;

		EntityNukeExplosionMK5 mk5 = new EntityNukeExplosionMK5(world);
		mk5.strength = (int)(r);
		mk5.speed = (int)Math.ceil(100000 / mk5.strength);
		mk5.setPosition(x, y, z);
		mk5.length = mk5.strength / 2;
		return mk5;
	}

	public static EntityNukeExplosionMK5 statFacNoRad(World world, int r, double x, double y, double z) {

		if(GeneralConfig.enableExtendedLogging && !world.isRemote)
			MainRegistry.logger.log(Level.INFO, "[NUKE] Initialized nR explosion at " + x + " / " + y + " / " + z + " with radius " + r + "!");

		EntityNukeExplosionMK5 mk5 = statFac(world, r, x, y ,z);
		mk5.fallout = false;
		return mk5;
	}
	
	public EntityNukeExplosionMK5 moreFallout(int fallout) {
		falloutAdd = fallout;
		return this;
	}
	
	public EntityNukeExplosionMK5 mute() {
		this.mute = true;
		return this;
	}
}

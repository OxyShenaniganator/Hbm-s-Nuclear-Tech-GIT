package com.hbm.explosion;

// Mark 5 Algorithm
// Original by HBMMods, aka The Bobcat
// Port by OxyShenaniganator, aka OxyOksirotl

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public class ExplosionNukeRayBatched {

    public HashMap<ChunkPos, List<FloatTriplet>> perChunk = new HashMap();
    public List<ChunkPos> orderedChunks = new ArrayList();
    private final ChunkPosComparator comparator = new ChunkPosComparator();

    int posX;
    int posY;
    int posZ;
    World world;

    int strength;
    int length;

    int gspNumMax;
    int gspNum;
    double gspX;
    double gspY;

    public boolean isAusf3Completed = false;

    public ExplosionNukeRayBatched(World world, int x, int y, int z, int strength, int speed, int length) {

        this.world = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.length = length;

        // Total number of points
        this.gspNumMax = (int) (2.5 * Math.PI * Math.pow(this.strength, 2));
        this.gspNum = 1;

        // The beginning of the generalized spiral points
        this.gspX = Math.PI;
        this.gspY = 0.0;

    }

    private void generateGspUp(){

        if (this.gspNum < this.gspNumMax) {

            int k = this.gspNum + 1;
            double hk = -1.0 + 2.0 * (k - 1.0) / (this.gspNumMax - 1.0);
            this.gspX = Math.acos(hk);

            double prev_lon = this.gspY;
            double lon = prev_lon + 3.6 / Math.sqrt(this.gspNumMax) / Math.sqrt(1.0 - hk * hk);
            this.gspY = lon % (Math.PI * 2);

        } else {

            this.gspX = 0.0;
            this.gspY = 0.0;

        }

        this.gspNum++;

    }

    // Get Cartesian coordinates for spherical coordinates
    private Vec3d getSpherical2cartesian(){
        double dx = Math.sin(this.gspX) * Math.cos(this.gspY);
        double dz = Math.sin(this.gspX) * Math.sin(this.gspY);
        double dy = Math.cos(this.gspX);
        return new Vec3d(dx, dy, dz);
    }

    public void collectTip(int count) {

        int amountProcessed = 0;

        while (this.gspNumMax >= this.gspNum) {

            // Get Cartesian coordinates for spherical coordinates
            Vec3d vec = this.getSpherical2cartesian();

            int length = (int) Math.ceil(strength);
            float res = strength;

            FloatTriplet lastPos = null;
            HashSet<ChunkPos> chunkPos = new HashSet();

            for (int i = 0; i < length; i++) {

                if (i > this.length) break;

                float x0 = (float) (posX + (vec.x * i));
                float y0 = (float) (posY + (vec.y * i));
                float z0 = (float) (posZ + (vec.z * i));

                int iX = (int) Math.floor(x0);
                int iY = (int) Math.floor(y0);
                int iZ = (int) Math.floor(z0);

                double fac = 100 - ((double) i) / ((double) length) * 100;
                fac *= 0.07D;

                BlockPos targetBlock = new BlockPos(iX, iY, iZ);

                if (!world.getBlockState(targetBlock).getMaterial().isLiquid())
                    res -= Math.pow(world.getBlockState(targetBlock).getBlock().getExplosionResistance(null), 7.5D - fac);

                if (res > 0 && world.getBlockState(targetBlock) != Blocks.AIR.getDefaultState()) {
                    lastPos = new FloatTriplet(x0, y0, z0);

                    // All-air chunks don't need to be buffered at all
                    ChunkPos targetChunk = new ChunkPos(iX >> 4, iZ >> 4);
                    chunkPos.add(targetChunk);

                }

                if (res <= 0 || i + 1 >= this.length) break;

            }

            for (ChunkPos pos : chunkPos) {

                List<FloatTriplet> triplets = perChunk.get(pos);

                if(triplets == null) {

                    triplets = new ArrayList();
                    perChunk.put(pos, triplets); // Re-using the same pos to save RAM

                }

                triplets.add(lastPos);

            }

            // Raise one generalized spiral points

            this.generateGspUp();

            amountProcessed++;
            if (amountProcessed >= count) return;

        }

        orderedChunks.addAll(perChunk.keySet());
        orderedChunks.sort(comparator);

        isAusf3Completed = true;

    }

    public void processChunk() {

        if(this.perChunk.isEmpty()) return;

        ChunkPos pos = orderedChunks.get(0);
        List<FloatTriplet> list = perChunk.get(pos);
        HashSet<BlockPos> toRem = new HashSet();

        int chunkX = pos.x;
        int chunkZ = pos.z;

        int enter = (int) (Math.min(
                Math.abs(posX - (chunkX << 4)),
                Math.abs(posZ - (chunkZ << 4)))) - 16;

        for (FloatTriplet triplet : list) {

            float x = triplet.xPos;
            float y = triplet.yPos;
            float z = triplet.zPos;
            Vec3d vec = new Vec3d(x - this.posX, y - this.posY, z - this.posZ);
            double pX = vec.x / vec.lengthVector();
            double pY = vec.y / vec.lengthVector();
            double pZ = vec.z / vec.lengthVector();

            boolean inChunk = false;

            for (int i = enter; i < vec.lengthVector(); i++) {
                int x0 = (int) Math.floor(posX + pX * i);
                int y0 = (int) Math.floor(posY + pY * i);
                int z0 = (int) Math.floor(posZ + pZ * i);

                if(x0 >> 4 != chunkX || z0 >> 4 != chunkZ) {
                    if (inChunk) break;
                    else continue;
                }

                inChunk = true;

                BlockPos removeBlock = new BlockPos(x0, y0, z0);

                if(!world.isAirBlock(removeBlock)) {
                    toRem.add(removeBlock);
                }

            }

        }

        for (BlockPos removeBlock : toRem) {
            world.setBlockState(removeBlock, Blocks.AIR.getDefaultState());
        }

        perChunk.remove(pos);
        orderedChunks.remove(0);

    }

    public class ChunkPosComparator implements Comparator<ChunkPos> {

        @Override
        public int compare(ChunkPos o1, ChunkPos o2) {

            int chunkX = ExplosionNukeRayBatched.this.posX >> 4;
            int chunkZ = ExplosionNukeRayBatched.this.posZ >> 4;

            int diff1 = Math.abs((chunkX - o1.x)) + Math.abs((chunkZ - o1.z));
            int diff2 = Math.abs((chunkX - o2.x)) + Math.abs((chunkZ - o2.z));

            return Integer.compare(diff1, diff2);

        }
    }

    public class FloatTriplet {

        public float xPos;
        public float yPos;
        public float zPos;

        public FloatTriplet(float x, float y, float z) {
            xPos = x;
            yPos = y;
            zPos = z;
        }

    }
}

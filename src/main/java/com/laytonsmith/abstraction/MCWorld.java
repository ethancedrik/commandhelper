

package com.laytonsmith.abstraction;

import com.laytonsmith.abstraction.blocks.MCBlock;
import com.laytonsmith.abstraction.blocks.MCFallingBlock;
import com.laytonsmith.abstraction.enums.MCBiomeType;
import com.laytonsmith.abstraction.enums.MCEffect;
import com.laytonsmith.abstraction.enums.MCMobs;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.Target;
import java.util.List;

/**
 *
 * @author layton
 */
public interface MCWorld extends AbstractionObject{
    public List<MCLivingEntity> getLivingEntities();
    public String getName();
    public MCBlock getBlockAt(int x, int y, int z);
	
	public boolean regenerateChunk(int x, int y);

    public MCEntity spawn(MCLocation l, Class mobType);

    public void playEffect(MCLocation l, MCEffect mCEffect, int e, int data);

    public MCItem dropItemNaturally(MCLocation l, MCItemStack is);

    public MCItem dropItem(MCLocation l, MCItemStack is);

    public void strikeLightning(MCLocation GetLocation);

    public void strikeLightningEffect(MCLocation GetLocation);

    public void setStorm(boolean b);

    public MCLocation getSpawnLocation();
    public void setSpawnLocation(int x, int y, int z);

    public void refreshChunk(int x, int z);

    public void setTime(long time);
    
    public long getTime();
    
    public CArray spawnMob(MCMobs name, String subClass, int qty, MCLocation location, Target t);
	
	public MCFallingBlock spawnFallingBlock(MCLocation loc, int type, byte data);

    public MCBiomeType getBiome(int x, int z);
    
    public void setBiome(int x, int z, MCBiomeType type);

    public MCBlock getHighestBlockAt(int x, int z);

    public void explosion(double x, double y, double z, float size, boolean safe);
	
	/**
	 * This method performs some check on the world to ensure it exists.
	 * @return 
	 */
	public boolean exists();
}

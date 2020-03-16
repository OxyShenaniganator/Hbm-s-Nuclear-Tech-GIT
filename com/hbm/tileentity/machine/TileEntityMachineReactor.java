package com.hbm.tileentity.machine;

import com.hbm.blocks.machine.MachineReactor;
import com.hbm.inventory.MachineRecipes;
import com.hbm.items.ModItems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityMachineReactor extends TileEntity implements ITickable {

	public ItemStackHandler inventory;
	
	public int dualCookTime;
	public int dualPower;
	public static final int maxPower = 1000;
	public static final int processingSpeed = 1000;
	public boolean runsOnRtg = false;
	
	//private static final int[] slots_top = new int[] {1};
	//private static final int[] slots_bottom = new int[] {2, 0};
	//private static final int[] slots_side = new int[] {0};
	
	private String customName;
	
	public TileEntityMachineReactor() {
		inventory = new ItemStackHandler(3){
			@Override
			protected void onContentsChanged(int slot) {
				markDirty();
				super.onContentsChanged(slot);
			}
		};
	}
	
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container.reactor";
	}

	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}
	
	public void setCustomName(String name) {
		this.customName = name;
	}
	
	public boolean isUseableByPlayer(EntityPlayer player) {
		if(world.getTileEntity(pos) != this)
		{
			return false;
		}else{
			return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <=64;
		}
	}
	
	public boolean hasItemPower(ItemStack itemStack) {
		return getItemPower(itemStack) > 0;
	}
	
	private static int getItemPower(ItemStack itemStack) {
		if(itemStack == null)
		{
			return 0;
		}else{
		Item item = itemStack.getItem();
		
		if(item == ModItems.rod_u238) return 1;
		if(item == ModItems.rod_dual_u238) return 2;
		if(item == ModItems.rod_quad_u238) return 4;
		if(item == ModItems.rod_u235) return 3;
		if(item == ModItems.rod_dual_u235) return 6;
		if(item == ModItems.rod_quad_u235) return 12;
		if(item == ModItems.rod_pu238) return 5;
		if(item == ModItems.rod_dual_pu238) return 10;
		if(item == ModItems.rod_quad_pu238) return 20;
		if(item == ModItems.rod_pu239) return 3;
		if(item == ModItems.rod_dual_pu239) return 6;
		if(item == ModItems.rod_quad_pu239) return 12;
		if(item == ModItems.rod_pu240) return 1;
		if(item == ModItems.rod_dual_pu240) return 2;
		if(item == ModItems.rod_quad_pu240) return 4;
		if(item == ModItems.rod_neptunium) return 3;
		if(item == ModItems.rod_dual_neptunium) return 6;
		if(item == ModItems.rod_quad_neptunium) return 12;
		if(item == ModItems.rod_schrabidium) return 15;
		if(item == ModItems.rod_dual_schrabidium) return 30;
		if(item == ModItems.rod_quad_schrabidium) return 60;
		if(item == ModItems.rod_solinium) return 20;
		if(item == ModItems.rod_dual_solinium) return 40;
		if(item == ModItems.rod_quad_solinium) return 80;
		
		return 0;
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		dualPower = nbt.getShort("powerTime");
		dualCookTime = nbt.getShort("CookTime");
		runsOnRtg = nbt.getBoolean("runsOnRtg");
		if(nbt.hasKey("inventory"))
			inventory.deserializeNBT(nbt.getCompoundTag("inventory"));
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		
		nbt.setShort("powerTime", (short) dualPower);
		nbt.setShort("cookTime", (short) dualCookTime);
		nbt.setBoolean("runsOnRtg", runsOnRtg);
		nbt.setTag("inventory", inventory.serializeNBT());
		
		return super.writeToNBT(nbt);
	}
	
	public int getDiFurnaceProgressScaled(int i) {
		return (dualCookTime * i) / processingSpeed;
	}
	
	public int getPowerRemainingScaled(int i) {
		return (dualPower * i) / maxPower;
	}
	
	public boolean canProcess() {
		if(inventory.getStackInSlot(1).isEmpty())
		{
			return false;
		}
		ItemStack itemStack = MachineRecipes.getReactorProcessingResult(inventory.getStackInSlot(1).getItem());
		if(itemStack == null)
		{
			return false;
		}
		
		if(inventory.getStackInSlot(2).isEmpty())
		{
			return true;
		}
		
		if(!inventory.getStackInSlot(2).isItemEqual(itemStack)) {
			return false;
		}
		
		if(inventory.getStackInSlot(2).getCount() < inventory.getSlotLimit(2) && inventory.getStackInSlot(2).getCount() < inventory.getStackInSlot(2).getMaxStackSize()) {
			return true;
		}else{
			return inventory.getStackInSlot(2).getCount() < itemStack.getMaxStackSize();
		}
	}
	
	private void processItem() {
		if(canProcess()) {
			ItemStack itemStack = MachineRecipes.getReactorProcessingResult(inventory.getStackInSlot(1).getItem());
			
			if(inventory.getStackInSlot(2).isEmpty())
			{
				inventory.setStackInSlot(2, itemStack.copy());
			}else if(inventory.getStackInSlot(2).isItemEqual(itemStack)) {
				inventory.getStackInSlot(2).grow(itemStack.getCount());
			}
			
			for(int i = 1; i < 2; i++)
			{
				if(inventory.getStackInSlot(i).getCount() <= 0)
				{
					inventory.setStackInSlot(i, new ItemStack(inventory.getStackInSlot(i).getItem().setFull3D()));
				}else{
					inventory.getStackInSlot(i).shrink(1);
				}
				if(inventory.getStackInSlot(i).getCount() <= 0)
				{
					inventory.setStackInSlot(i, ItemStack.EMPTY);
				}
			}
			
			if(!runsOnRtg)
			{
				dualPower--;
			}
		}
	}
	
	public boolean hasPower() {
		return dualPower > 0;
	}
	
	public boolean isProcessing() {
		return this.dualCookTime > 0;
	}
	
	@Override
	public void update() {
		boolean flag1 = false;
		
		if(!world.isRemote)
		{
			if(this.hasItemPower(inventory.getStackInSlot(0)) && this.dualPower == 0)
			{
				this.dualPower += getItemPower(inventory.getStackInSlot(0));
				if(!inventory.getStackInSlot(0).isEmpty())
				{
					flag1 = true;
					ItemStack container = inventory.getStackInSlot(0).getItem().getContainerItem(inventory.getStackInSlot(0));
					inventory.getStackInSlot(0).shrink(1);
					if(inventory.getStackInSlot(0).isEmpty())
					{
						inventory.setStackInSlot(0, container);
					}
				}
			}
			
			if(!inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(0).getItem() == ModItems.pellet_rtg && this.dualPower == 0)
			{
				ItemStack container = inventory.getStackInSlot(0).getItem().getContainerItem(inventory.getStackInSlot(0));
				inventory.getStackInSlot(0).shrink(1);
				if(inventory.getStackInSlot(0).isEmpty())
				{
					inventory.setStackInSlot(0, container);
				}
				
				this.runsOnRtg = true;
				
				this.dualPower = 1;
			}
			
			if(hasPower() && canProcess())
			{
				dualCookTime++;
				
				if(this.dualCookTime == TileEntityMachineReactor.processingSpeed)
				{
					this.dualCookTime = 0;
					this.processItem();
					flag1 = true;
				}
			}else{
				dualCookTime = 0;
			}
			
			boolean trigger = true;
			
			if(hasPower() && canProcess() && this.dualCookTime == 0)
			{
				trigger = false;
			}
			
			if(trigger)
            {
                flag1 = true;
                MachineReactor.updateBlockState(this.dualCookTime > 0, this.world, pos);
            }
		}
		
		if(flag1)
		{
			this.markDirty();
		}
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory) : super.getCapability(capability, facing);
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}
	
}

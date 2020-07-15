package com.buuz135.industrial.block.resourceproduction.tile;

import com.buuz135.industrial.block.tile.IndustrialProcessingTile;
import com.buuz135.industrial.config.machine.resourceproduction.ResourcefulFurnaceConfig;
import com.buuz135.industrial.module.ModuleCore;
import com.buuz135.industrial.module.ModuleResourceProduction;
import com.hrznstudio.titanium.annotation.Save;
import com.hrznstudio.titanium.component.energy.EnergyStorageComponent;
import com.hrznstudio.titanium.component.fluid.FluidTankComponent;
import com.hrznstudio.titanium.component.fluid.SidedFluidTankComponent;
import com.hrznstudio.titanium.component.inventory.SidedInventoryComponent;
import com.hrznstudio.titanium.util.RecipeUtil;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.Collection;

public class ResourcefulFurnaceTile extends IndustrialProcessingTile<ResourcefulFurnaceTile> {

    private int getPowerPerTick;

    @Save
    private SidedInventoryComponent<ResourcefulFurnaceTile> input;
    @Save
    private SidedInventoryComponent<ResourcefulFurnaceTile> output;
    @Save
    private SidedFluidTankComponent<ResourcefulFurnaceTile> tank;

    private FurnaceRecipe[] recipes;

    public ResourcefulFurnaceTile() {
        super(ModuleResourceProduction.RESOURCEFUL_FURNACE, 74, 22 + 18);
        addInventory(this.input = (SidedInventoryComponent<ResourcefulFurnaceTile>) new SidedInventoryComponent<ResourcefulFurnaceTile>("input", 44, 22, 3, 0).
                setColor(DyeColor.BLUE).
                setSlotLimit(1).
                setRange(1, 3).
                setOnSlotChanged((itemStack, integer) -> {
                    checkForRecipe(integer);
                }));
        addInventory(this.output = (SidedInventoryComponent<ResourcefulFurnaceTile>) new SidedInventoryComponent<ResourcefulFurnaceTile>("output", 110, 22, 3, 1).
                setColor(DyeColor.ORANGE).
                setInputFilter((itemStack, integer) -> false).
                setRange(1, 3));
        addTank(this.tank = (SidedFluidTankComponent<ResourcefulFurnaceTile>) new SidedFluidTankComponent<ResourcefulFurnaceTile>("essence", ResourcefulFurnaceConfig.maxEssenceTankSize, 132, 20, 2).
                setColor(DyeColor.LIME).
                setTankAction(FluidTankComponent.Action.DRAIN));
        this.recipes = new FurnaceRecipe[3];
        this.getPowerPerTick = ResourcefulFurnaceConfig.powerPerTick;
    }

    @Override
    public void setWorldAndPos(World p_226984_1_, BlockPos p_226984_2_) {
        super.setWorldAndPos(p_226984_1_, p_226984_2_);
        checkForRecipe(0);
        checkForRecipe(1);
        checkForRecipe(2);
    }

    private void checkForRecipe(int slot) {
        Collection<FurnaceRecipe> recipes = RecipeUtil.getCookingRecipes(this.world);
        this.recipes[slot] = recipes.stream().filter(furnaceRecipe -> furnaceRecipe.getIngredients().get(0).test(input.getStackInSlot(slot))).findAny().orElse(null);
    }

    @Override
    public boolean canIncrease() {
        for (FurnaceRecipe recipe : this.recipes) {
            if (recipe != null && ItemHandlerHelper.insertItem(output, recipe.getRecipeOutput().copy(), true).isEmpty())
                return true;
        }
        return false;
    }

    @Override
    public Runnable onFinish() {
        return () -> {
            for (int i = 0; i < this.recipes.length; i++) {
                FurnaceRecipe recipe = this.recipes[i];
                if (recipe != null && ItemHandlerHelper.insertItem(output, recipe.getRecipeOutput().copy(), true).isEmpty()) {
                    if (ItemHandlerHelper.insertItem(output, recipe.getRecipeOutput().copy(), true).isEmpty()) {
                        input.setStackInSlot(i, ItemStack.EMPTY);
                        ItemHandlerHelper.insertItem(output, recipe.getRecipeOutput().copy(), false);
                        tank.fill(new FluidStack(ModuleCore.ESSENCE.getSourceFluid(), (int) (recipe.getExperience() * 20)), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        };
    }

    @Override
    protected EnergyStorageComponent<ResourcefulFurnaceTile> createEnergyStorage() {
        return new EnergyStorageComponent<>(ResourcefulFurnaceConfig.maxStoredPower, 10, 20);
    }

    @Override
    protected int getTickPower() {
        return getPowerPerTick;
    }


    @Nonnull
    @Override
    public ResourcefulFurnaceTile getSelf() {
        return this;
    }
}

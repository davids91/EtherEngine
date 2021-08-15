package com.crystalline.aether.models.world;

import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.MiscUtils;
import com.crystalline.aether.services.world.World;

import java.nio.FloatBuffer;
import java.util.Random;

public class ElementalAspectStrategy extends RealityAspectStrategy{
    public static final int velocityMaxTicks = 3;
    private final int chunkSize;
    private Random rnd;
    private MiscUtils myMiscUtils;
    public ElementalAspectStrategy(int chunkSize_){
        chunkSize = chunkSize_;
        myMiscUtils = new MiscUtils();
        rnd = new Random();
    }

    public static boolean isMovable(int x, int y, int chunkSize, FloatBuffer elements, FloatBuffer scalars){
        return Material.movable(getElementEnum(x,y,chunkSize,elements), World.getUnit(x,y,chunkSize,scalars));
    }

    /*TODO: include forces into the logic ( e.g. a big force can force a switch ) */
    public static boolean aCanMoveB(int ax, int ay, int bx, int by, int chunkSize, FloatBuffer elements, FloatBuffer scalars){
        return(
            Material.discardable(ElementalAspectStrategy.getElementEnum(bx, by, chunkSize, elements), World.getUnit(bx, by, chunkSize, scalars))
            ||(
                (ElementalAspectStrategy.getWeight(ax,ay, chunkSize, elements, scalars) >= ElementalAspectStrategy.getWeight(bx, by, chunkSize, elements, scalars))
                && Material.movable(ElementalAspectStrategy.getElementEnum(ax,ay, chunkSize, elements), World.getUnit(ax,ay, chunkSize, scalars))
                && Material.movable(ElementalAspectStrategy.getElementEnum(bx,by, chunkSize, elements), World.getUnit(bx,by, chunkSize, scalars))
            )
        );
    }

    private float avgOfUnit(int x, int y, FloatBuffer elements, FloatBuffer scalars, Material.Elements type){
        float average_val = 0;
        float division = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(chunkSize, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(chunkSize, y + 2); ++ny) {
                if(ElementalAspectStrategy.getElementEnum(nx,ny, chunkSize, elements) == type){
                    average_val += World.getUnit(nx,ny,chunkSize,scalars);
                    division += 1;
                }
            }
        }
        if(0 < division)average_val /= division;
        return average_val;
    }

    private int numOfElements(int x, int y, FloatBuffer elements, Material.Elements type){
        int num = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(chunkSize, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(chunkSize, y + 2); ++ny) {
                if(ElementalAspectStrategy.getElementEnum(nx,ny, chunkSize, elements) == type){
                    ++num;
                }
            }
        }
        return num;
    }

    public float avgOfUnitsWithinDistance(int x, int y, FloatBuffer elements, FloatBuffer scalars){
        float average_val = World.getUnit(x,y, chunkSize, scalars);
        float division = 1;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(chunkSize, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(chunkSize, y + 2); ++ny) {
                if(
                        (x != nx) && (y != ny)
                                &&Material.isSameMat(
                                ElementalAspectStrategy.getElementEnum(x,y, chunkSize, elements), World.getUnit(x,y, chunkSize, scalars),
                                ElementalAspectStrategy.getElementEnum(nx,ny, chunkSize, elements), World.getUnit(nx,ny, chunkSize, scalars)
                        )
                ){
                    average_val += World.getUnit(nx,ny, chunkSize, scalars);
                    division += 1;
                }
            }
        }
        average_val /= division;
        return average_val;
    }

    /**
     * Defines the elemental phase based on the ethereal
     * @param inputs [0]: elements; [1]: ethereal
     * @param output the re-written elemental plane
     */
    public void defineByEtherealPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            ElementalAspectStrategy.setElement(x,y, chunkSize, output, EtherealAspectStrategy.getElementEnum(x,y, chunkSize, inputs[1]));
            RealityAspectStrategy.setPriority(x,y, chunkSize, output, RealityAspectStrategy.getPriority(x,y, chunkSize, inputs[0]));
        } }
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: elements
     * @param output elements buffer
     */
    public void switchElementsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            Material.Elements element = ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[1]);
            if(0 != RealityAspectStrategy.getOffsetCode(x,y,chunkSize, inputs[0])){
                int targetX = RealityAspectStrategy.getTargetX(x,y,chunkSize, inputs[0]);
                int targetY = RealityAspectStrategy.getTargetY(x,y,chunkSize, inputs[0]);
                int toApply = (int) RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]);
                if(
                        (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y)
                                &&(0 < toApply)
                                &&(targetX >= 0)&&(targetX < chunkSize)
                                &&(targetY >= 0)&&(targetY < chunkSize)
                ){
                    element = ElementalAspectStrategy.getElementEnum(targetX,targetY, chunkSize, inputs[1]);
                }
            }
            ElementalAspectStrategy.setElement(x,y, chunkSize, output, element);
            RealityAspectStrategy.setPriority(x,y, chunkSize, output, ElementalAspectStrategy.getPriority(x,y, chunkSize, inputs[1]));
            /*!Note: Priorities serve as an arbitration measure based on coordinates, so they should not be switched
             */
        }}
    }

    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: dynamics
     * @param output dynamics buffer
     */
    public void switchDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            int targetX = RealityAspectStrategy.getTargetX(x,y,chunkSize, inputs[0]);
            int targetY = RealityAspectStrategy.getTargetY(x,y,chunkSize, inputs[0]);
            int toApply = (int) RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]);
            float forceX = ElementalAspectStrategy.getForceX(x,y, chunkSize, inputs[1]);
            float forceY = ElementalAspectStrategy.getForceY(x,y, chunkSize, inputs[1]);
            int velocityTick = ElementalAspectStrategy.getVelocityTick(x,y, chunkSize, inputs[1]);
            float gravityCorrection = ElementalAspectStrategy.getGravityCorrection(x,y, chunkSize, inputs[1]);
            if(
                    (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y)
                            &&(0 < toApply)&&(0 != RealityAspectStrategy.getOffsetCode(x,y,chunkSize, inputs[0]))
                            &&(targetX >= 0)&&(targetX < chunkSize)
                            &&(targetY >= 0)&&(targetY < chunkSize)
            ){
                forceX = ElementalAspectStrategy.getForceX(targetX,targetY, chunkSize, inputs[1]);
                forceY = ElementalAspectStrategy.getForceY(targetX,targetY, chunkSize, inputs[1]);
                gravityCorrection = ElementalAspectStrategy.getGravityCorrection(targetX,targetY, chunkSize, inputs[1]);;
                velocityTick = ElementalAspectStrategy.getVelocityTick(targetX,targetY, chunkSize, inputs[1]);
            }
            ElementalAspectStrategy.setForceX(x,y, chunkSize, output, forceX);
            ElementalAspectStrategy.setForceY(x,y, chunkSize, output, forceY);
            ElementalAspectStrategy.setVelocityTick(x,y, chunkSize, output, velocityTick);
            ElementalAspectStrategy.setGravityCorrection(x,y, chunkSize, output, gravityCorrection);
        }}
    }

    public void processUnitsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < chunkSize; ++x) { for (int y = 0; y < chunkSize; ++y) { /* Calculate dilution */
            if(Material.movable(ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[1]))) {
                World.setUnit(x,y,chunkSize, output, avgOfUnitsWithinDistance(x,y,inputs[0], inputs[1]));
            }else{
                World.setUnit(x,y, chunkSize, output, World.getUnit(x,y, chunkSize, inputs[1]));
            }
        } }
    }

    /**
     * Provides a refined version of the current elemental aspect
     * @param inputs [0]: elements; [1]: ethereal; [2]: scalars
     * @param output elements
     */
    public void processTypesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = chunkSize - 1;x >= 0; --x)for(int y = chunkSize - 1 ; y >= 0; --y) {
            Material.Elements currentElement = EtherealAspectStrategy.getElementEnum(x,y,chunkSize,inputs[1]);
            float currentUnit = World.getUnit(x,y,chunkSize, inputs[2]);
            if(Material.Elements.Water == currentElement){ /* TODO: This will be ill-defined in a multi-threaded environment */
                if(avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Water) < avgOfUnit(x,y, inputs[0],inputs[2], Material.Elements.Fire)){
                    currentElement = Material.Elements.Air;
                }
            }

            if(Material.Elements.Air == currentElement) { /* TODO: make air catch fire! */
                if(
                        (numOfElements(x,y,inputs[0], Material.Elements.Air) < numOfElements(x,y,inputs[0], Material.Elements.Fire))
                                &&(0 == avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Water))
                ){
                    currentElement = Material.Elements.Fire;
                }
            }

            /* TODO: Store Flammability */
            /* TODO: Make fire springing out from Earth */
            if(Material.Elements.Fire == currentElement){
                /* TODO: Make lava cool off to earth by heat */
                if(avgOfUnit(x,y,inputs[0],inputs[2],Material.Elements.Water) > avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Fire)){
                    currentElement = Material.Elements.Earth;
                }
            }

            if(Material.Elements.Earth == currentElement){
                /* TODO: Make Earth keep track of heat instead of units */
                if((avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Earth) < avgOfUnit(x,y, inputs[0],inputs[2], Material.Elements.Fire))){
                    if( /* TODO: Make sand melt "into" glass */
                            Material.MechaProperties.Solid.ordinal() > Material.getState(Material.Elements.Earth, currentUnit).ordinal()
                                    || Material.MechaProperties.Plasma.ordinal() < Material.getState(Material.Elements.Fire, currentUnit).ordinal()
                    )currentElement = Material.Elements.Fire;
                }
            }
            ElementalAspectStrategy.setElement(x,y,chunkSize,output,currentElement);
            RealityAspectStrategy.setPriority(x,y, chunkSize, output, ElementalAspectStrategy.getPriority(x,y, chunkSize, inputs[0]));
        }
    }

    public void processTypeUnitsPhase(FloatBuffer[] inputs, FloatBuffer output) {
        for(int x = chunkSize - 1;x >= 0; --x) for(int y = chunkSize - 1 ; y >= 0; --y) {
            Material.Elements currentElement = ElementalAspectStrategy.getElementEnum(x,y,chunkSize,inputs[0]);
            float currentUnit = World.getUnit(x,y,chunkSize, inputs[1]);
            if(Material.Elements.Water == currentElement){
                if(y > (chunkSize * 0.9)){
                    currentUnit -= currentUnit * 0.02f;
                }
            }

            if(Material.Elements.Fire == currentElement){
                if(
                        (Material.MechaProperties.Plasma == Material.getState(currentElement, currentUnit))
                                && (currentUnit < avgOfUnit(x,y,inputs[0],inputs[1], Material.Elements.Fire))
                ){
                    currentUnit -= currentUnit * 0.1f;
                }else
                if(
                        (Material.MechaProperties.Plasma == Material.getState(currentElement, currentUnit))
                ){
                    currentUnit -= currentUnit * 0.05f;
                }
            }

            /* TODO: Make nearby fire consume compatible Earth */
            currentUnit = Math.max(0.1f,currentUnit);
            World.setUnit(x,y,chunkSize,output,currentUnit);
        }
    }

    /**
     * A function to propose force updates based on material properties
     * @param inputs: none
     * @param output the initialized proposed changes
     */
    public void initChangesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 1; x < chunkSize; ++x){ for(int y = 1; y < chunkSize; ++y){
            ElementalAspectStrategy.setOffsetCode(x,y, chunkSize, output, 0);
            ElementalAspectStrategy.setVelocityTick(x,y, chunkSize, output, 0);
            ElementalAspectStrategy.setToApply(x,y, chunkSize, output, 0);
        }}
    }

    /**
     * A function to propose force updates based on material properties
     * @param inputs: [0]: elements; [1]: dynamics; [2]: scalars; [3]: ethereal
     * @param output the resulting updated dynamics
     */
    public void proposeForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 1; x < chunkSize; ++x){ for(int y = 1; y < chunkSize; ++y){
            float forceX = 0.0f;
            float forceY = 0.0f;

            /*!Note: Adding gravity is handled in the post-process phase, not here, to handle collisions correctly */
            if(!Material.discardable(ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[0]),World.getUnit(x,y, chunkSize, inputs[2]))){
                forceX = ElementalAspectStrategy.getForceX(x,y, chunkSize, inputs[1]);
                forceY = ElementalAspectStrategy.getForceY(x,y, chunkSize, inputs[1]);
            }

            if(Material.Elements.Ether == ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[0])){
                for (int nx = (x - 2); nx < (x + 3); ++nx) for (int ny = (y - 2); ny < (y + 3); ++ny) {
                    if ( /* in the bounds of the chunk.. */
                        (0 <= nx)&&(chunkSize > nx)&&(0 <= ny)&&(chunkSize > ny)
                        &&( 1 < (Math.abs(x - nx) + Math.abs(y - ny)) ) /* ..after the not immediate neighbourhood.. */
                        &&(World.getUnit(x,y, chunkSize, inputs[2]) <= World.getUnit(x,y, chunkSize, inputs[2]))
                        &&(Material.Elements.Ether == ElementalAspectStrategy.getElementEnum(nx,ny, chunkSize, inputs[0]))
                    ){ /* ..Calculate forces from surplus ethers */
                        float aether_diff = Math.max(-10.5f, Math.min(10.5f,
                            ( EtherealAspectStrategy.aetherValueAt(x,y, chunkSize, inputs[3]) - EtherealAspectStrategy.aetherValueAt(nx,ny, chunkSize, inputs[3]))
                        ));
                        forceX += ((nx - x) * aether_diff);
                        forceY += ((ny - y) * aether_diff);
                    }
                }
            }

            if(Material.MechaProperties.Fluid == Material.getState(ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2]))){
                if(Material.isSameMat(
                    ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2]),
                    ElementalAspectStrategy.getElementEnum(x,y-1, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2])
                )){ /* TODO: define the water cell force behavior correctly: each water cell aims to be 1.5 cells from one another */
                    /* the cell is a liquid on top of another liquid, so it must move. */
                    if((0.0f < forceX)&&(6.0f > forceX))forceX *= 1.2f;
                    else{
                        forceX = rnd.nextInt(6) - 3;
                        forceY = 1.00f;
                    }
                }
            }else if(Material.MechaProperties.Plasma == Material.getState(getElementEnum(x,y, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2]))){
                forceX += rnd.nextInt(4) - 2;
            }/* TODO: Make gases loom, instead of staying still ( move about a bit maybe? )  */

            ElementalAspectStrategy.setForce(x,y, chunkSize, output,forceX,forceY);
            ElementalAspectStrategy.setGravityCorrection(x,y, chunkSize, output, 0); /* Gravity correction is not part of this phase */
            ElementalAspectStrategy.setVelocityTick(x,y, chunkSize, output, velocityMaxTicks);
        } }
    }

    /**
     * Provides proposed cell switches based on Forces
     * @param inputs [0]: previously proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output proposed switches, toApply set all 0
     */
    public void proposeChangesFromForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            int newVelocityTick = ElementalAspectStrategy.getVelocityTick(x,y, chunkSize, inputs[2]);
            int targetX = RealityAspectStrategy.getTargetX(x,y, chunkSize, inputs[0]);
            int targetY = RealityAspectStrategy.getTargetY(x,y, chunkSize, inputs[0]);
            int toApply = (int) RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]); /* a previously proposed change would overwrite a current loop change */

            if((0 == toApply)||(x == targetX && y == targetY)){ /* if no switch was arbitrated in the previously proposed changes */
                /* a target was not proposed previously for this cell, which would overwrite any switch proposed from forces */
                if(
                        !Material.discardable(ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[1]), World.getUnit(x,y, chunkSize, inputs[3]))
                                && (1 <= ElementalAspectStrategy.getForce(x,y, chunkSize, inputs[2]).len())
                ){
                    /* propose to change to the direction of the force */
                    if(1 < Math.abs(ElementalAspectStrategy.getForceX(x,y, chunkSize, inputs[2])))targetX = (int)(x + Math.max(-1, Math.min(ElementalAspectStrategy.getForceX(x,y, chunkSize, inputs[2]),1)));
                    if(1 < Math.abs(ElementalAspectStrategy.getForceY(x,y, chunkSize, inputs[2])))targetY = (int)(y + Math.max(-1, Math.min(ElementalAspectStrategy.getForceY(x,y, chunkSize, inputs[2]),1)));
                    targetX = Math.max(0, Math.min(chunkSize-1, targetX));
                    targetY = Math.max(0, Math.min(chunkSize-1, targetY));

                    /* calculate the final position of the intended target cell */
                    int targetFinalPosX = targetX;
                    int targetFinalPosY = targetY;
                    if(1 < Math.abs(ElementalAspectStrategy.getForceX(targetX,targetY, chunkSize, inputs[2])))
                        targetFinalPosX = (int)(targetX + Math.max(-1.1f, Math.min(ElementalAspectStrategy.getForceX(targetX, targetY, chunkSize, inputs[2]),1.1f)));
                    if(1 < Math.abs(ElementalAspectStrategy.getForceY(targetX,targetY, chunkSize, inputs[2])))
                        targetFinalPosY = (int)(targetY + Math.max(-1.1f, Math.min(ElementalAspectStrategy.getForceY(targetX,targetY, chunkSize, inputs[2]),1.1f)));

                    /* see if the two cells still intersect with forces included */
                    if(2 > MiscUtils.distance(x,y,targetFinalPosX,targetFinalPosY)){
                        if(
                            !((x == targetX) && (y == targetY))
                            &&( /* In case both is discardable, then no operations shall commence */
                                Material.discardable(getElementEnum(x,y, chunkSize, inputs[1]),World.getUnit(x,y, chunkSize, inputs[3]))
                                &&Material.discardable(getElementEnum(targetX,targetY, chunkSize, inputs[1]),World.getUnit(targetX,targetY, chunkSize, inputs[3]))
                            )
                        ){ /* both cells are discardable, so don't switch */
                            targetX = x;
                            targetY = y;
                        }else if (velocityMaxTicks > newVelocityTick){ /* propose a switch with the target only if the velocity ticks are at threshold */
                            ++newVelocityTick;
                            targetX = x;
                            targetY = y;
                        }
                    }else{ /* the two cells don't intersect by transition, so don't update the target */
                        targetX = x;
                        targetY = y;
                    }
                }
            }
            ElementalAspectStrategy.setVelocityTick(x,y, chunkSize, output, newVelocityTick);
            /* TODO: Try to have everything go TOP RIGHT direction: Bottom left corner will duplicate cells somehow... */
            ElementalAspectStrategy.setOffsetCode(x,y,chunkSize,output, RealityAspectStrategy.getOffsetCode((targetX - x), (targetY - y)));
            ElementalAspectStrategy.setToApply(x,y, chunkSize,output, 0);
        }}
    }

    /**
     * Decides which changes are to be applied, and which would not
     * @param inputs [0]: proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output the arbitrated changes, with the toApply part set
     */
    public void arbitrateChangesPhase(FloatBuffer[] inputs, FloatBuffer output){
        final int index_radius = 2;
        final int index_table_size = (index_radius * 2) + 1;
        int[][] priority = new int[index_table_size][index_table_size];
        int[][] changed = new int[index_table_size][index_table_size];
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            float offsetCode = 0;
            float toApply = 0;

            /* Initialize local data */
            int minIndexX = Math.max((x-index_radius), 0);
            int maxIndexX = Math.min((x+index_radius), chunkSize);
            int minIndexY = Math.max((y-index_radius), 0);
            int maxIndexY = Math.min((y+index_radius), chunkSize);
            for(int ix = minIndexX; ix < maxIndexX; ++ix){ for(int iy = minIndexY; iy < maxIndexY; ++iy) {
                int sx = ix - x + (index_radius);
                int sy = iy - y + (index_radius);
                priority[sx][sy] = (int)( /* The priority of the given cell consist of..  */
                        ElementalAspectStrategy.getForce(ix,iy, chunkSize, inputs[2]).len() /* ..the power of the force on it.. */
                                + ElementalAspectStrategy.getWeight(ix,iy, chunkSize, inputs[1], inputs[3]) /* .. and its weight */
                );
                changed[sx][sy] = 0;
            }}

            int highestPrioX;
            int highestPrioY;
            int highestTargetX;
            int highestTargetY;
            while(true){ /* Until all requests with priority above are found  */
                highestPrioX = -2;
                highestPrioY = -2;
                highestTargetX = -2;
                highestTargetY = -2;
                int localSourceX; /* The highest priority change in the local vicinity.. */
                int localSourceY; /* ..corresponding to highestPrio*, which is of global index scope */
                int localTargetOfCX;
                int localTargetOfCY;
                int highestPrioLocalX = -2;
                int highestPrioLocalY = -2;
                int highestPrioTargetLocalX = -2;
                int highestPrioTargetLocalY = -2;
                for(int ix = minIndexX; ix < maxIndexX; ++ix){ for(int iy = minIndexY; iy < maxIndexY; ++iy) {
                    int targetOfCX = RealityAspectStrategy.getTargetX(ix,iy,chunkSize,inputs[0]);
                    int targetOfCY = RealityAspectStrategy.getTargetY(ix,iy,chunkSize,inputs[0]);
                    localSourceX = ix - x + index_radius;
                    localSourceY = iy - y + index_radius;
                    localTargetOfCX = targetOfCX - x + index_radius;
                    localTargetOfCY = targetOfCY - y + index_radius;
                    if( /* The highest priority swap request is.. */
                            ( /* ..the one which isn't changed yet (only higher priority changes occurred prior to this loop )  */
                                    (0 == changed[localSourceX][localSourceY])
                                            &&( /* And either the target is out of bounds.. */
                                            (localTargetOfCX < 0)||(localTargetOfCX >= index_table_size)
                                                    ||(localTargetOfCY < 0)||(localTargetOfCY >= index_table_size)
                                                    ||(0 == changed[localTargetOfCX][localTargetOfCY]) /* ..or not changed yet */
                                    )
                            )&&( /* ..and of course the currently examined index has to has a higher target, then the previous highest one */
                                    ((-2 == highestPrioLocalX)||(-2 == highestPrioLocalY))
                                            ||(
                                            (priority[highestPrioLocalX][highestPrioLocalY] < priority[localSourceX][localSourceY])
                                                    ||(
                                                    (priority[highestPrioLocalX][highestPrioLocalY] == priority[localSourceX][localSourceY])
                                                            &&(
                                                            priority[highestPrioLocalX][highestPrioLocalY]
                                                                    < (priority[localSourceX][localSourceY] + ElementalAspectStrategy.getPriority(ix, iy, chunkSize, inputs[1]))
                                                    )
                                            )
                                    )
                            )
                    ){
                        highestPrioX = ix;
                        highestPrioY = iy;
                        highestPrioLocalX = localSourceX;
                        highestPrioLocalY = localSourceY;
                        highestTargetX = targetOfCX;
                        highestTargetY = targetOfCY;
                        highestPrioTargetLocalX = localTargetOfCX;
                        highestPrioTargetLocalY = localTargetOfCY;
                    }
                }}

                /* Simulate the highest priority change */
                if((-2 != highestPrioX)&&(-2 != highestPrioY)){
                    changed[highestPrioLocalX][highestPrioLocalY] = 1;
                    if(
                            (highestPrioTargetLocalX >= 0)&&(highestPrioTargetLocalX < index_table_size)
                                    &&(highestPrioTargetLocalY >= 0)&&(highestPrioTargetLocalY < index_table_size)
                    ){
                        changed[highestPrioTargetLocalX][highestPrioTargetLocalY] = 1;
                    }
                }

                /* If c was reached; or no changes are proposed; break! */
                if(
                        ((x == highestPrioX)&&(y == highestPrioY))
                                ||((x == highestTargetX)&&(y == highestTargetY))
                                ||((-2 == highestPrioX)&&(-2 == highestPrioY))
                                ||((-2 == highestTargetX)&&(-2 == highestTargetY))
                ){
                    if(
                            ((x == highestPrioX)&&(y == highestPrioY))
                                    ||((x == highestTargetX)&&(y == highestTargetY))
                    ){
                        if((x == highestPrioX)&&(y == highestPrioY)){
                            toApply = 2;
                            offsetCode = RealityAspectStrategy.getOffsetCode((highestTargetX - x), (highestTargetY - y));
                        }else{ /* if((x == highestTargetX)&&(y == highestTargetY)){ /* This is always true here.. */
                            toApply = 1;
                            offsetCode = RealityAspectStrategy.getOffsetCode((highestPrioX - x), (highestPrioY - y));
                        }
                    }
                    /*!Note: Only set the target if the current cell is actually the highest priority;
                     * because the swap will be decided for the target(other cell) based on this information
                     * */
                    break;
                }
            }

            /*!Note: At this point highestPrio* and highestTarget*
             * should contain either -2 or the highest priority switch request involving c
             * */
            RealityAspectStrategy.setOffsetCode(x,y, chunkSize, output, offsetCode);
            RealityAspectStrategy.setToApply(x,y, chunkSize,output, toApply);
        }}
    }

    /**
     * Applies the changes to forces proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output dynamics buffer updated with proper forces
     */
    public void applyChangesDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){ /* TODO: Define edges as connection point to other chunks */
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            int toApply = (int) RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]);
            int targetX = ElementalAspectStrategy.getTargetX(x,y,chunkSize, inputs[0]);
            int targetY = ElementalAspectStrategy.getTargetY(x,y,chunkSize, inputs[0]);
            float forceX = ElementalAspectStrategy.getForceX(x,y, chunkSize, inputs[2]);
            float forceY = ElementalAspectStrategy.getForceY(x,y, chunkSize, inputs[2]);
            float weight = ElementalAspectStrategy.getWeight(x,y, chunkSize, inputs[1], inputs[3]);
            float gravityCorrection = ElementalAspectStrategy.getWeight(x,y, chunkSize, inputs[1], inputs[3]);
            int EH = 0;

            if( /* Update the forces on a cell.. */
                    (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y) /* ..when it is inside bounds.. */
                            &&(0 < toApply)&&(0 != RealityAspectStrategy.getOffsetCode(x,y,chunkSize, inputs[0])) /* ..only if it wants to switch..  */
                            &&((targetX >= 0)&&(targetX < chunkSize)&&(targetY >= 0)&&(targetY < chunkSize)) /* ..but only if the target is also inside the bounds of the chunk */
            ){
                gravityCorrection = 0; /* Gravity is being added at forces update, so no need to re-add it at the end of the loop */
                if( ElementalAspectStrategy.aCanMoveB(x,y,targetX,targetY, chunkSize, inputs[1], inputs[3]) ){ /* The cells swap, decreasing forces on both *//* TODO: Also decrease the force based on the targets weight */
                    forceX += -forceX * 0.7f * ( Math.abs(weight) / Math.max(0.00001f, Math.max(Math.abs(weight),Math.abs(forceX))) );
                    forceY += -forceY * 0.7f * ( Math.abs(weight) / Math.max(0.00001f, Math.max(Math.abs(weight),Math.abs(forceY))) );
                    forceX += (myMiscUtils.getGravity(x,y).x * weight);
                    forceY += (myMiscUtils.getGravity(x,y).y * weight);
                    EH = 1;
                }else if(ElementalAspectStrategy.aCanMoveB(targetX,targetY,x,y, chunkSize, inputs[1], inputs[3])){ /* The cells collide, updating forces, but no swapping */
                    Vector2 u1 = ElementalAspectStrategy.getForce(x,y, chunkSize, inputs[2]).cpy().nor();
                    float m2 = ElementalAspectStrategy.getWeight(targetX, targetY, chunkSize, inputs[1], inputs[3]);
                    Vector2 u2 = ElementalAspectStrategy.getForce(targetX, targetY, chunkSize, inputs[2]).cpy().nor();
                    Vector2 result_speed = new Vector2();
                    result_speed.set( /*!Note: https://en.wikipedia.org/wiki/Elastic_collision#One-dimensional_Newtonian */
                            ((weight - m2)/(weight+m2)*u1.x) + (2*m2/(weight+m2))*u2.x,
                            ((weight - m2)/(weight+m2)*u1.y) + (2*m2/(weight+m2))*u2.y
                    );

                    /* F = m*a --> `a` is the delta v, which is the change in the velocity */
                    forceX = (weight * (result_speed.x - u1.x));
                    forceY = (weight * (result_speed.y - u1.y));
                    forceX += (myMiscUtils.getGravity(x,y).x * weight);
                    forceY += (myMiscUtils.getGravity(x,y).y * weight);
                    EH = 2;
                }
            }
            ElementalAspectStrategy.setForceX(x,y, chunkSize, output, forceX);
            ElementalAspectStrategy.setForceY(x,y, chunkSize, output, forceY);
            ElementalAspectStrategy.setVelocityTick(x,y, chunkSize, output, ElementalAspectStrategy.getVelocityTick(x,y, chunkSize, inputs[2]));
            ElementalAspectStrategy.setGravityCorrection(x,y, chunkSize, output, gravityCorrection);
        }}
    }

    /**
     * Decides whether the proposed changes are swaps or collisions
     * @param inputs [0]: proposed changes; [1]: elements; [2]:  scalars
     * @param output the proposed changes where toApply means swaps need to happen
     */
    public void arbitrateInteractionsPhase(FloatBuffer[] inputs, FloatBuffer output){
        /* Note: At this point the switches are supposed to be mutual: If a <> b, then every time b <> a  */
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            float toApply = RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]);
            int targetX = RealityAspectStrategy.getTargetX(x,y,chunkSize, inputs[0]);
            int targetY = RealityAspectStrategy.getTargetY(x,y,chunkSize, inputs[0]);
            if( /* Check for swaps */
                    (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y) /* ..when cell is inside bounds.. */
                            &&(0 < toApply)&&(0 != RealityAspectStrategy.getOffsetCode(x,y,chunkSize, inputs[0])) /* ..and it wants to switch..  */
                            &&((targetX >= 0)&&(targetX < chunkSize)&&(targetY >= 0)&&(targetY < chunkSize)) /* ..but only if the target is also inside the bounds of the chunk */
            ){
                if( /* this cell can not move its target */
                        ((2 == toApply)&&(!ElementalAspectStrategy.aCanMoveB(x,y,targetX,targetY, chunkSize, inputs[1], inputs[2])))
                                ||((1 == toApply)&&(!ElementalAspectStrategy.aCanMoveB(targetX,targetY, x,y, chunkSize, inputs[1], inputs[2])))
                                ||(
                                (!ElementalAspectStrategy.aCanMoveB(x,y,targetX,targetY, chunkSize, inputs[1], inputs[2]))
                                        &&(
                                        (x != RealityAspectStrategy.getTargetX(targetX,targetY, chunkSize, inputs[0]))
                                                ||(y != RealityAspectStrategy.getTargetY(targetX,targetY, chunkSize, inputs[0]))
                                )
                        )
                ){
                    toApply = 0;
                }
            }
            RealityAspectStrategy.setOffsetCode(x,y, chunkSize, output, RealityAspectStrategy.getOffsetCode(x,y, chunkSize, inputs[0]));
            RealityAspectStrategy.setToApply(x,y, chunkSize,output, toApply);
        }}
    }

    /**
     * Post-processing with the dynamics:basically corrects with the gravity based on GravityCorrection
     * @param inputs [0]: elements; [1]: dynamics; [2]: scalars
     * @param output the post-processed dynamics buffer
     */
    public void mechanicsPostProcessDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            float gravityCorrection = ElementalAspectStrategy.getGravityCorrection(x,y, chunkSize, inputs[1]);
            float forceX = ElementalAspectStrategy.getForceX(x,y, chunkSize, inputs[1]);
            float forceY = ElementalAspectStrategy.getForceY(x,y, chunkSize, inputs[1]);
            if(
                    (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y)
                            &&(0 < gravityCorrection)&&Material.movable(ElementalAspectStrategy.getElementEnum(x,y, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2]))
            ){
                forceX += gravityCorrection * myMiscUtils.getGravity(x,y).x;
                forceY += gravityCorrection * myMiscUtils.getGravity(x,y).y;
            }
            ElementalAspectStrategy.setForceX(x,y, chunkSize, output, forceX);
            ElementalAspectStrategy.setForceY(x,y, chunkSize, output, forceY);
            ElementalAspectStrategy.setVelocityTick(x,y, chunkSize, output, ElementalAspectStrategy.getVelocityTick(x,y, chunkSize, inputs[1]));
            ElementalAspectStrategy.setGravityCorrection(x,y, chunkSize, output, 0);
        }}
    }

    public static Material.Elements getElementEnum(int x, int y, int chunkSize, FloatBuffer buffer){
        return Material.Elements.get((int)getElement(x,y,chunkSize,buffer));
    }

    public static float getElement(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,0, buffer);
    }

    public static void setElement(int x, int y, int chunkSize, FloatBuffer buffer, Material.Elements element){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer,(float)element.ordinal());
    }

    private static final Vector2 tmpVec = new Vector2();
    public static Vector2 getForce(int x, int y, int chunkSize, FloatBuffer buffer){
        tmpVec.set(
                BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,0, buffer),
                BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,1, buffer)
        );
        return tmpVec;
    }
    public static float getForceX(int x, int y, int chunkSize, FloatBuffer buffer){
        return getForce(x,y, chunkSize, buffer).x;
    }
    public static float getForceY(int x, int y, int chunkSize, FloatBuffer buffer){
        return getForce(x,y, chunkSize, buffer).y;
    }
    public static void setForce(int x, int y, int chunkSize, FloatBuffer buffer, Vector2 value){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer, value.x);
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,buffer, value.y);
    }
    public static void setForce(int x, int y, int chunkSize, FloatBuffer buffer, float valueX, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer, valueX);
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,buffer, valueY);
    }
    public static void setForceX(int x, int y, int chunkSize, FloatBuffer buffer, float valueX){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer, valueX);
    }
    public static void setForceY(int x, int y, int chunkSize, FloatBuffer buffer, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,buffer, valueY);
    }
    public static void addToForce(int x, int y, int chunkSize, FloatBuffer buffer, float valueX, float valueY){
        addToForceX(x, y, chunkSize, buffer, valueX);
        addToForceY(x, y, chunkSize, buffer, valueY);
    }
    public static void addToForceX(int x, int y, int chunkSize, FloatBuffer buffer, float valueX){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,buffer, getForceX(x,y, chunkSize, buffer) + valueX);
    }
    public static void addToForceY(int x, int y, int chunkSize, FloatBuffer buffer, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,buffer, getForceY(x,y, chunkSize,  buffer) + valueY);
    }
    public static int getVelocityTick(int x, int y, int chunkSize, FloatBuffer buffer){
        return (int)BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,2,buffer);
    }
    public static void setVelocityTick(int x, int y, int chunkSize, FloatBuffer buffer, int value){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,2,buffer, value);
    }
    public static void increaseVelocityTick(int x, int y, int chunkSize, FloatBuffer buffer){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,2,buffer, getVelocityTick(x,y, chunkSize, buffer)+1);
    }
    public static float getGravityCorrection(int x, int y, int chunkSize, FloatBuffer buffer){
        return BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,3,buffer);
    }
    public static void setGravityCorrection(int x, int y, int chunkSize, FloatBuffer buffer, float value){
        BufferUtils.set(x,y, chunkSize, Config.bufferCellSize,3,buffer, value);
    }

    /* TODO: Weight to include pressure somehow? or at least the same materials on top */
    public static float getWeight(int x, int y, int chunkSize, FloatBuffer elements, FloatBuffer scalars){
        return (
                World.getUnit(x,y, chunkSize, scalars)
                        * Material.TYPE_SPECIFIC_GRAVITY
                        [(int)ElementalAspectStrategy.getElement(x,y, chunkSize, elements)]
                        [MiscUtils.indexIn(
                        Material.TYPE_UNIT_SELECTOR[(int)ElementalAspectStrategy.getElement(x,y, chunkSize, elements)],
                        World.getUnit(x,y, chunkSize, scalars)
                )]
        );
    }
}

package com.crystalline.aether.models.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.computation.Includer;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.MiscUtils;
import com.crystalline.aether.services.utils.StringUtils;
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
            Material.discardable(getElementEnum(bx, by, chunkSize, elements), World.getUnit(bx, by, chunkSize, scalars))
            ||(
                (getWeight(ax,ay, chunkSize, elements, scalars) >= getWeight(bx, by, chunkSize, elements, scalars))
                && Material.movable(getElementEnum(ax,ay, chunkSize, elements), World.getUnit(ax,ay, chunkSize, scalars))
                && Material.movable(getElementEnum(bx,by, chunkSize, elements), World.getUnit(bx,by, chunkSize, scalars))
            )
        );
    }

    /* TODO: move avgOf operations to a separate calculation run */
    private float avgOfUnit(int x, int y, FloatBuffer elements, FloatBuffer scalars, Material.Elements type){
        float average_val = 0;
        float division = 0;
        for (int nx = Math.max(0, (x - 1)); nx < Math.min(chunkSize, x + 2); ++nx) {
            for (int ny = Math.max(0, (y - 1)); ny < Math.min(chunkSize, y + 2); ++ny) {
                if(getElementEnum(nx,ny, chunkSize, elements) == type){
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
                if(getElementEnum(nx,ny, chunkSize, elements) == type){
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
                        getElementEnum(x,y, chunkSize, elements), World.getUnit(x,y, chunkSize, scalars),
                        getElementEnum(nx,ny, chunkSize, elements), World.getUnit(nx,ny, chunkSize, scalars)
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

    public static final String defineByEtherealPhaseKernel = buildKernel(StringUtils.readFileAsString(
        Gdx.files.internal("shaders/elmDefineByEtherealPhase.fshader")
    ), new Includer(baseIncluder));
    /**
     * Defines the elemental phase based on the ethereal
     * @param inputs [0]: elements; [1]: ethereal
     * @param output the re-written elemental plane
     */
    public void defineByEtherealPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            setElement(x,y, chunkSize, output, EtherealAspectStrategy.getElementEnum(x,y, chunkSize, inputs[1]));
            setPriority(x,y, chunkSize, output, getPriority(x,y, chunkSize, inputs[0]));
        } }
    }

    public static final String switchElementsPhaseKernel = buildKernel(StringUtils.readFileAsString(
        Gdx.files.internal("shaders/elmSwitchElementsPhase.fshader")
    ), new Includer(baseIncluder));
    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: elements
     * @param output elements buffer
     */
    public void switchElementsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            Material.Elements currentElement = getElementEnum(x,y, chunkSize, inputs[1]);
            if(
                (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y)
                &&(0 != RealityAspectStrategy.getOffsetCode(x,y,chunkSize, inputs[0]))
                &&(0 < RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]))
            ){
                int targetX = RealityAspectStrategy.getTargetX(x,y,chunkSize, inputs[0]);
                int targetY = RealityAspectStrategy.getTargetY(x,y,chunkSize, inputs[0]);
                if((targetX >= 0)&&(targetX < chunkSize)&&(targetY >= 0)&&(targetY < chunkSize)){
                    currentElement = getElementEnum(targetX,targetY, chunkSize, inputs[1]);
                }
            }
            setElement(x,y, chunkSize, output, currentElement);
            setPriority(x,y, chunkSize, output, getPriority(x,y, chunkSize, inputs[1]));
            /*!Note: Priorities serve as an arbitration measure based on coordinates, so they should not be switched
             */
        }}
    }

    public static final String switchForcesPhaseKernel = buildKernel(StringUtils.readFileAsString(
        Gdx.files.internal("shaders/elmSwitchForcesPhase.fshader")
    ), new Includer(baseIncluder));
    /**
     * Applies the changes proposed from the input proposal buffer
     * @param inputs [0]: proposed changes; [1]: forces
     * @param output dynamics buffer
     */
    public void switchForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            float forceX = getForceX(x,y, chunkSize, inputs[1]);
            float forceY = getForceY(x,y, chunkSize, inputs[1]);
            int velocityTick = getVelocityTick(x,y, chunkSize, inputs[1]);
            if(
                (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y)
                &&(0 < RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]))
                &&(0 != RealityAspectStrategy.getOffsetCode(x,y,chunkSize, inputs[0]))
            ){
                int targetX = RealityAspectStrategy.getTargetX(x,y,chunkSize, inputs[0]);
                int targetY = RealityAspectStrategy.getTargetY(x,y,chunkSize, inputs[0]);
                if(
                    (targetX >= 0)&&(targetX < chunkSize)
                    &&(targetY >= 0)&&(targetY < chunkSize)
                ){
                    forceX = getForceX(targetX,targetY, chunkSize, inputs[1]);
                    forceY = getForceY(targetX,targetY, chunkSize, inputs[1]);
                    velocityTick = getVelocityTick(targetX,targetY, chunkSize, inputs[1]);
                }
            }
            setForceX(x,y, chunkSize, output, forceX);
            setForceY(x,y, chunkSize, output, forceY);
            setVelocityTick(x,y, chunkSize, output, velocityTick);
        }}
    }

    public static final String processUnitsPhaseKernel = buildKernel(StringUtils.readFileAsString(
        Gdx.files.internal("shaders/elmProcessUnitsPhase.fshader")
    ), new Includer(baseIncluder));
    /**
     * Provides the number of units after a refinement step in the elemental phase
     * @param inputs [0]: elements; [1]: scalars
     * @param output the scalar units after the refinement step
     */
    public void processUnitsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0;x < chunkSize; ++x) { for (int y = 0; y < chunkSize; ++y) { /* Calculate dilution */
            World.setUnit(x,y, chunkSize, output, World.getUnit(x,y, chunkSize, inputs[1]));
        } }
    }

    public static final String processTypesPhaseKernel = buildKernel(StringUtils.readFileAsString(
            Gdx.files.internal("shaders/elmProcessTypesPhase.fshader")
    ), new Includer(baseIncluder));
    /**
     * Provides a refined version of the current elemental aspect
     * @param inputs [0]: elements; [1]: ethereal; [2]: scalars
     * @param output elements
     */
    public void processTypesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = chunkSize - 1;x >= 0; --x)for(int y = chunkSize - 1 ; y >= 0; --y) {
            Material.Elements currentElement = EtherealAspectStrategy.getElementEnum(x,y,chunkSize,inputs[1]);
            float currentUnit = World.getUnit(x,y,chunkSize, inputs[2]);

            if(Material.Elements.Water == currentElement){
                if(
                    avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Water)
                    < (avgOfUnit(x,y, inputs[0],inputs[2], Material.Elements.Fire) * 1.2f)
                ){
                    currentElement = Material.Elements.Air;
                }
            }

            /* TODO: Store Flammability */
            /* TODO: Make fire springing out from Earth */
            if(Material.Elements.Fire == currentElement){
                /* TODO: Make lava cool off to earth by heat */
                if(
                    avgOfUnit(x,y,inputs[0],inputs[2],Material.Elements.Water)
                    > avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Fire)
                ){
                    currentElement = Material.Elements.Earth;
                }
            }

//            if(Material.Elements.Air == currentElement) { /* TODO: make air catch fire! */
//                if(
//                    (
//                        avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Air)
//                        <= avgOfUnit(x,y,inputs[0] ,inputs[2],Material.Elements.Fire)
//                    ) && (0 == avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Water))
//                ){
//                    currentElement = Material.Elements.Fire;
//                }
//            }

//            if(Material.Elements.Earth == currentElement){
//                /* TODO: Make Earth keep track of heat instead of units */
//                if((avgOfUnit(x,y,inputs[0],inputs[2], Material.Elements.Earth) < avgOfUnit(x,y, inputs[0],inputs[2], Material.Elements.Fire))){
//                    if( /* TODO: Make sand melt "into" glass */
//                        Material.MechaProperties.Solid.ordinal() > Material.getState(Material.Elements.Earth, currentUnit).ordinal()
//                        || Material.MechaProperties.Plasma.ordinal() < Material.getState(Material.Elements.Fire, currentUnit).ordinal()
//                    )currentElement = Material.Elements.Fire;
//                }
//            }
            setElement(x,y,chunkSize,output,currentElement);
            setPriority(x,y, chunkSize, output, getPriority(x,y, chunkSize, inputs[0]));
        }
    }

    public static final String processTypesUnitsPhaseKernel = buildKernel(StringUtils.readFileAsString(
        Gdx.files.internal("shaders/elmProcessTypesUnitsPhase.fshader")
    ), new Includer(baseIncluder));
    /**
     * Calculates the unit values each cell shall have after this phase
     * @param inputs [0]: elements; [1]: scalars
     * @param output the refined unit values
     */
    public void processTypeUnitsPhase(FloatBuffer[] inputs, FloatBuffer output) {
        for(int x = chunkSize - 1;x >= 0; --x) for(int y = chunkSize - 1 ; y >= 0; --y) {
            Material.Elements currentElement = getElementEnum(x,y,chunkSize,inputs[0]);
            float currentUnit = World.getUnit(x,y,chunkSize, inputs[1]);
            /* TODO Create a pressure modifier to make water get from gas to fluid again */

            if(Material.Elements.Fire == currentElement){ /* TODO: Make fire disappear */
                if(Material.MechaProperties.Plasma == Material.getState(currentElement, currentUnit)){
                    if(currentUnit < avgOfUnit(x,y,inputs[0],inputs[1], Material.Elements.Fire)){
                        currentUnit -= currentUnit * 0.1f;
                    }else{
                        currentUnit -= currentUnit * 0.05f;
                    }
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
            setOffsetCode(x,y, chunkSize, output, 0);
            setVelocityTick(x,y, chunkSize, output, 0);
            setToApply(x,y, chunkSize, output, 0);
        }}
    }

    /**
     * A function to propose force updates based on material properties
     * @param inputs: [0]: elements; [1]: forces; [2]: scalars; [3]: ethereal
     * @param output the resulting updated dynamics
     */
    public void proposeForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 1; x < chunkSize; ++x){ for(int y = 1; y < chunkSize; ++y){
            float forceX = 0.0f;
            float forceY = 0.0f;

            /*!Note: Adding gravity is handled in the post-process phase, not here, to handle collisions correctly */
            if(!Material.discardable(getElementEnum(x,y, chunkSize, inputs[0]),World.getUnit(x,y, chunkSize, inputs[2]))){
                forceX = getForceX(x,y, chunkSize, inputs[1]);
                forceY = getForceY(x,y, chunkSize, inputs[1]);
            }

            if(Material.Elements.Ether == getElementEnum(x,y, chunkSize, inputs[0])){
                for (int nx = (x - 2); nx < (x + 3); ++nx) for (int ny = (y - 2); ny < (y + 3); ++ny) {
                    if ( /* in the bounds of the chunk.. */
                        (0 <= nx)&&(chunkSize > nx)&&(0 <= ny)&&(chunkSize > ny)
                        &&( 1 < (Math.abs(x - nx) + Math.abs(y - ny)) ) /* ..after the not immediate neighbourhood.. */
                        &&(World.getUnit(x,y, chunkSize, inputs[2]) <= World.getUnit(x,y, chunkSize, inputs[2]))
                        &&(Material.Elements.Ether == getElementEnum(nx,ny, chunkSize, inputs[0]))
                    ){ /* ..Calculate forces from surplus ethers */
                        float aether_diff = Math.max(-10.5f, Math.min(10.5f,
                            ( EtherealAspectStrategy.aetherValueAt(x,y, chunkSize, inputs[3]) - EtherealAspectStrategy.aetherValueAt(nx,ny, chunkSize, inputs[3]))
                        ));
                        forceX += ((nx - x) * aether_diff);
                        forceY += ((ny - y) * aether_diff);
                    }
                }
            }

            if(Material.MechaProperties.Fluid == Material.getState(getElementEnum(x,y, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2]))){
                if(Material.isSameMat(
                    getElementEnum(x,y, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2]),
                    getElementEnum(x,y-1, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2])
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

            setForce(x,y, chunkSize, output,forceX,forceY);
            setVelocityTick(x,y, chunkSize, output, velocityMaxTicks);
        } }
    }

    /**
     * Provides proposed cell switches based on Forces
     * @param inputs [0]: previously proposed changes; [1]: elements; [2]: dynamics; [3]: scalars
     * @param output proposed switches, toApply set all 0
     */
    public void proposeChangesFromForcesPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            int newVelocityTick = getVelocityTick(x,y, chunkSize, inputs[2]);
            int targetX = RealityAspectStrategy.getTargetX(x,y, chunkSize, inputs[0]);
            int targetY = RealityAspectStrategy.getTargetY(x,y, chunkSize, inputs[0]);
            int toApply = (int) RealityAspectStrategy.getToApply(x,y, chunkSize, inputs[0]); /* a previously proposed change would overwrite a current loop change */

            if((0 == toApply)||(x == targetX && y == targetY)){ /* if no switch was arbitrated in the previously proposed changes */
                /* a target was not proposed previously for this cell, which would overwrite any switch proposed from forces */
                if(
                    !Material.discardable(getElementEnum(x,y, chunkSize, inputs[1]), World.getUnit(x,y, chunkSize, inputs[3]))
                    && (1 <= getForce(x,y, chunkSize, inputs[2]).len())
                ){
                    /* propose to change to the direction of the force */
                    if(1 < Math.abs(getForceX(x,y, chunkSize, inputs[2])))targetX = (int)(x + Math.max(-1, Math.min(getForceX(x,y, chunkSize, inputs[2]),1)));
                    if(1 < Math.abs(getForceY(x,y, chunkSize, inputs[2])))targetY = (int)(y + Math.max(-1, Math.min(getForceY(x,y, chunkSize, inputs[2]),1)));
                    targetX = Math.max(0, Math.min(chunkSize-1, targetX));
                    targetY = Math.max(0, Math.min(chunkSize-1, targetY));

                    /* calculate the final position of the intended target cell */
                    int targetFinalPosX = targetX;
                    int targetFinalPosY = targetY;
                    if(1 < Math.abs(getForceX(targetX,targetY, chunkSize, inputs[2])))
                        targetFinalPosX = (int)(targetX + Math.max(-1.1f, Math.min(getForceX(targetX, targetY, chunkSize, inputs[2]),1.1f)));
                    if(1 < Math.abs(getForceY(targetX,targetY, chunkSize, inputs[2])))
                        targetFinalPosY = (int)(targetY + Math.max(-1.1f, Math.min(getForceY(targetX,targetY, chunkSize, inputs[2]),1.1f)));

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
            setVelocityTick(x,y, chunkSize, output, newVelocityTick);
            /* TODO: Try to have everything go TOP RIGHT direction: Bottom left corner will duplicate cells somehow... */
            setOffsetCode(x,y,chunkSize,output, RealityAspectStrategy.getOffsetCode((targetX - x), (targetY - y)));
            setToApply(x,y, chunkSize,output, 0);
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
                    getForce(ix,iy, chunkSize, inputs[2]).len() /* ..the power of the force on it.. */
                    + getWeight(ix,iy, chunkSize, inputs[1], inputs[3]) /* .. and its weight */
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
                                        < (priority[localSourceX][localSourceY] + getPriority(ix, iy, chunkSize, inputs[1]))
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
            setOffsetCode(x,y, chunkSize, output, offsetCode);
            setToApply(x,y, chunkSize,output, toApply);
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
            int targetX = getTargetX(x,y,chunkSize, inputs[0]);
            int targetY = getTargetY(x,y,chunkSize, inputs[0]);
            float forceX = getForceX(x,y, chunkSize, inputs[2]);
            float forceY = getForceY(x,y, chunkSize, inputs[2]);
            float weight = getWeight(x,y, chunkSize, inputs[1], inputs[3]);

            if( /* Update the forces on a cell.. */
                (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y) /* ..when it is inside bounds.. */
                &&(0 < toApply)&&(0 != RealityAspectStrategy.getOffsetCode(x,y,chunkSize, inputs[0])) /* ..only if it wants to switch..  */
                &&((targetX >= 0)&&(targetX < chunkSize)&&(targetY >= 0)&&(targetY < chunkSize)) /* ..but only if the target is also inside the bounds of the chunk */
            ){
                if( aCanMoveB(x,y,targetX,targetY, chunkSize, inputs[1], inputs[3]) ){ /* The cells swap, decreasing forces on both *//* TODO: Also decrease the force based on the targets weight */
                    forceX += -forceX * 0.7f * ( Math.abs(weight) / Math.max(0.00001f, Math.max(Math.abs(weight),Math.abs(forceX))) );
                    forceY += -forceY * 0.7f * ( Math.abs(weight) / Math.max(0.00001f, Math.max(Math.abs(weight),Math.abs(forceY))) );
                    forceX += (myMiscUtils.getGravity(x,y).x * weight);
                    forceY += (myMiscUtils.getGravity(x,y).y * weight);
                }else if(aCanMoveB(targetX,targetY,x,y, chunkSize, inputs[1], inputs[3])){ /* The cells collide, updating forces, but no swapping */
                    Vector2 u1 = getForce(x,y, chunkSize, inputs[2]).cpy().nor();
                    float m2 = getWeight(targetX, targetY, chunkSize, inputs[1], inputs[3]);
                    Vector2 u2 = getForce(targetX, targetY, chunkSize, inputs[2]).cpy().nor();
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
                }
            }
            setForceX(x,y, chunkSize, output, forceX);
            setForceY(x,y, chunkSize, output, forceY);
            setVelocityTick(x,y, chunkSize, output, getVelocityTick(x,y, chunkSize, inputs[2]));
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
                    ((2 == toApply)&&(!aCanMoveB(x,y,targetX,targetY, chunkSize, inputs[1], inputs[2])))
                        ||((1 == toApply)&&(!aCanMoveB(targetX,targetY, x,y, chunkSize, inputs[1], inputs[2])))
                        ||(
                        (!aCanMoveB(x,y,targetX,targetY, chunkSize, inputs[1], inputs[2]))
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
     * @param inputs [0]: elements; [1]: forces; [2]: scalars; [3]: Proposed changes
     * @param output the post-processed dynamics buffer
     */
    public void mechanicsPostProcessDynamicsPhase(FloatBuffer[] inputs, FloatBuffer output){
        for(int x = 0; x < chunkSize; ++x){ for(int y = 0; y < chunkSize; ++y){
            float gravityCorrection = (
                getToApply(x,y, chunkSize, inputs[3]) * getWeight(x,y, chunkSize, inputs[0], inputs[2])
            );
            float forceX = getForceX(x,y, chunkSize, inputs[1]);
            float forceY = getForceY(x,y, chunkSize, inputs[1]);
            if(
                (0 < x)&&(chunkSize-1 > x)&&(0 < y)&&(chunkSize-1 > y)
                &&(0 < gravityCorrection)&&Material.movable(getElementEnum(x,y, chunkSize, inputs[0]), World.getUnit(x,y, chunkSize, inputs[2]))
            ){
                forceX += gravityCorrection * myMiscUtils.getGravity(x,y).x;
                forceY += gravityCorrection * myMiscUtils.getGravity(x,y).y;
            }
            setForceX(x,y, chunkSize, output, forceX);
            setForceY(x,y, chunkSize, output, forceY);
            setVelocityTick(x,y, chunkSize, output, getVelocityTick(x,y, chunkSize, inputs[1]));
        }}
    }

    /* Buffer Structures
     */
    /** TODO: Maybe prio doesn't need to be kept... It can be just calculated from coordinates
     * A texture image representing the elemental properties of reality
     * - R: block type --> Material.Elements
     * - G:
     * - B:priority --> A unique number to decide arbitration while switching cells
     */
    public static Material.Elements getElementEnum(int x, int y, int chunkSize, FloatBuffer elements){
        return Material.Elements.get((int)getElement(x,y,chunkSize,elements));
    }

    public static float getElement(int x, int y, int chunkSize, FloatBuffer elements){
        return BufferUtils.get(x,y,chunkSize, Config.bufferCellSize,0, elements);
    }

    public static void setElement(int x, int y, int chunkSize, FloatBuffer elements, Material.Elements element){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,elements,(float)element.ordinal());
    }

    public static void setPriority(int x, int y, int chunkSize, FloatBuffer buffer, float prio){
        BufferUtils.set(x,y,chunkSize, Config.bufferCellSize,2, buffer, prio);
    }

    public static float getPriority(int x, int y, int chunkSize, FloatBuffer elements){
        return BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,2, elements);
    }

    /** TODO: Move away velocity tick
     * A texture image representing the dynamism of a cell
     * - R: x of the force vector active on the block
     * - G: y of the force vector active on the block
     * - B: the velocity tick of the cell ( 0 means the cell would move )
     */
    private static final Vector2 tmpVec = new Vector2();
    public static Vector2 getForce(int x, int y, int chunkSize, FloatBuffer forces){
        tmpVec.set(
            BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,0, forces),
            BufferUtils.get(x,y,chunkSize,Config.bufferCellSize,1, forces)
        );
        return tmpVec;
    }
    public static float getForceX(int x, int y, int chunkSize, FloatBuffer forces){
        return getForce(x,y, chunkSize, forces).x;
    }
    public static float getForceY(int x, int y, int chunkSize, FloatBuffer forces){
        return getForce(x,y, chunkSize, forces).y;
    }
    public static void setForce(int x, int y, int chunkSize, FloatBuffer forces, Vector2 value){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,forces, value.x);
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,forces, value.y);
    }
    public static void setForce(int x, int y, int chunkSize, FloatBuffer forces, float valueX, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,forces, valueX);
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,forces, valueY);
    }
    public static void setForceX(int x, int y, int chunkSize, FloatBuffer forces, float valueX){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,forces, valueX);
    }
    public static void setForceY(int x, int y, int chunkSize, FloatBuffer forces, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,forces, valueY);
    }
    public static void addToForce(int x, int y, int chunkSize, FloatBuffer forces, float valueX, float valueY){
        addToForceX(x, y, chunkSize, forces, valueX);
        addToForceY(x, y, chunkSize, forces, valueY);
    }
    public static void addToForceX(int x, int y, int chunkSize, FloatBuffer forces, float valueX){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,0,forces, getForceX(x,y, chunkSize, forces) + valueX);
    }
    public static void addToForceY(int x, int y, int chunkSize, FloatBuffer forces, float valueY){
        BufferUtils.set(x,y,chunkSize,Config.bufferCellSize,1,forces, getForceY(x,y, chunkSize,  forces) + valueY);
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

    /* TODO: Weight to include pressure somehow? or at least the same materials on top */
    public static float getWeight(int x, int y, int chunkSize, FloatBuffer elements, FloatBuffer scalars){
        return (
                World.getUnit(x,y, chunkSize, scalars)
                        * Material.TYPE_SPECIFIC_GRAVITY
                        [(int)getElement(x,y, chunkSize, elements)]
                        [MiscUtils.indexIn(
                        Material.TYPE_UNIT_SELECTOR[(int)getElement(x,y, chunkSize, elements)],
                        World.getUnit(x,y, chunkSize, scalars)
                )]
        );
    }
}

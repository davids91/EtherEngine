/* =================== MATERIAL_LIBRARY =================== */

const float PHI = 1.6;//1803398875f;
const float world_ratioEther = 1.0;
const float world_ratioEarth = (PHI);
const float world_ratioWater = (PHI * PHI);
const float world_ratioAir =   (PHI * PHI * PHI);
const float world_ratioFire =  (PHI * PHI * PHI * PHI);

const float world_stateNegligible   = 0;
const float world_stateGas          = 1;
const float world_stateFluid        = 2;
const float world_statePlasma       = 3;
const float world_stateGranular     = 4;
const float world_stateSolid        = 5;
const float world_stateCrystal      = 6;
const float world_stateHard         = 7;
const float world_stateSuperhard    = 8;
const float world_stateUltrahard    = 9;
const float world_stateMorningWood  = 10;

const float NETHER_RATIOS[6] = float[6](
  world_ratioEther,
  world_ratioEarth, world_ratioWater,
  world_ratioAir, world_ratioFire,
  0.0
);

const float[6][6] TYPE_UNIT_SELECTOR = {
/* Ether */   float[6](0,  50, 500,   5000,   50000,    500000),
/* Earth */   float[6](0,  10, 15,    70,     700,      1000),
/* Water */   float[6](0,  50, 100,   1000,   10000,    100000),
/* Air*/      float[6](0,  10, 1000,  10000,  100000,   10000000),
/* Fire */    float[6](10, 50, 100,   1000,   10000,    100000),
/* Nothing */ float[6](0,  0,  0,     0,      0,        0)
};

const float[6][6] TYPE_SPECIFIC_STATE = {
  /* Ether */   float[6](
    world_stateGas,       world_stateGranular,
    world_stateGranular,  world_stateGranular,
    world_stateGranular,  world_stateGranular
  ),
  /* Earth */   float[6](
    world_stateGranular,  world_stateGranular,
    world_stateSolid,     world_stateCrystal,
    world_stateHard,      world_stateSuperhard
  ),
  /* Water */   float[6](
    world_stateFluid,   world_stateGas,
    world_stateFluid,   world_stateFluid,
    world_stateFluid,   world_stateFluid
  ),
  /* Air*/      float[6](
    world_stateNegligible,  world_stateNegligible,
    world_stateNegligible,  world_stateNegligible,
    world_stateNegligible,  world_stateNegligible
  ),
  /* Fire */    float[6](
    world_statePlasma,      world_stateFluid,
    world_stateUltrahard,   world_stateUltrahard,
    world_stateUltrahard,   world_stateUltrahard
  ),
  /* Nothing */ float[6](
    world_stateNegligible,  world_stateNegligible,
    world_stateNegligible,  world_stateNegligible,
    world_stateNegligible,  world_stateNegligible
  )
};

const float world_indexEther = 0;
const float world_indexEarth = 1;
const float world_indexWater = 2;
const float world_indexAir   = 3;
const float world_indexFire  = 4;

float world_RatioOf(float element){
  return NETHER_RATIOS[int(element)];
}

int world_indexIn(float[6] table, float value){
  int index = table.length()-1;
  while((index > 0)&&(table[index] >= value))--index;
  return index;
}

float world_getState(float element, float unit){
  int index = world_indexIn(TYPE_UNIT_SELECTOR[int(element)], unit);
  return TYPE_SPECIFIC_STATE[int(element)][index];
}

bool world_isCellMovable(float element, float unit){
  float state = world_getState(element, unit);
  return (
    (world_stateNegligible < state)
    &&(world_stateSolid > state)
  );
}

bool world_isCellDiscardable(float element, float unit){
  return (world_stateNegligible == world_getState(element, unit));
}

bool world_isSameMat(float elementA, float unitA, float elementB, float unitB){
  return(
    (elementA == elementB)
    &&(
      world_indexIn(TYPE_UNIT_SELECTOR[int(elementA)], unitA)
      == world_indexIn(TYPE_UNIT_SELECTOR[int(elementA)], unitA)
    )
  );
}
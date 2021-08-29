/* =================== MATERIAL_LIBRARY =================== */

const float PHI = 1.6;//1803398875f;
const float world_ratioEther = 1.0;
const float world_ratioEarth = (PHI);
const float world_ratioWater = (PHI * PHI);
const float world_ratioAir = (PHI * PHI * PHI);
const float world_ratioFire = (PHI * PHI * PHI * PHI);

const float netherRatios[6] = float[6](
  world_ratioEther,
  world_ratioEarth, world_ratioWater, world_ratioAir, world_ratioFire,
  0.0
);

float world_RatioOf(float element){
  return netherRatios[int(element)];
}
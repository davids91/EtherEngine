/* =================== MATERIAL_LIBRARY =================== */

const float PHI = 1.61803398875f;
const float world_ratioAir = (PHI * PHI * PHI);
const float netherRatios[6] = float[6](
/* Ether, Earth, Water,       Air,            Fire,                    Nothing */
   1.0,   PHI,   (PHI * PHI), world_ratioAir, (PHI * PHI * PHI * PHI), 0.0
);

float world_RatioOf(float element){
  return netherRatios[int(element)];
}
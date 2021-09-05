/* =================== WORLD_LIBRARY =================== */

const float velocityMaxTicks = 3;

float world_getUnit(vec2 position, sampler2D scalars){
  return texture(scalars, position).r;
}

void world_setUnit(float value){
  gl_FragColor.r = value;
}
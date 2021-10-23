/* =================== WORLD_LIBRARY =================== */

const float velocityMaxTicks = 3;
const vec2 world_gravity = vec2(0,-1.81);

float world_getUnit(vec2 position, sampler2D scalars){
  return texture(scalars, position).r;
}

void world_setUnit(float value){
  gl_FragColor.r = value;
}
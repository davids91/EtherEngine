/* =================== WORLD_LIBRARY =================== */

float world_getUnit(vec2 position, sampler2D scalars){
  return texture(scalars, position).r;
}

void world_setUnit(inout vec4 previous_color, float value){
  previous_color.r = value;
}
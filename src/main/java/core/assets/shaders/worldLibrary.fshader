/* =================== WORLD_LIBRARY =================== */

float world_getUnit(vec2 position, sampler2D scalars){
  return texture(scalars, position).r;
}

vec4 world_setUnit(inout vec4 previous_color, float value){
  vec4 new_color = previous_color;
  new_color.r = value;
  return new_color;
}
/* =================== WORLD_LIBRARY =================== */

float world_getUnit(vec2 position, sampler2D scalars){
  return texture2D(scalars, position).r;
}

vec4 world_setUnit(vec4 previous_color, float value){
  vec4 new_color = previous_color;
  new_color.r = value;
  return new_color;
}
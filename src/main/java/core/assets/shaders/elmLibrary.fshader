/* =================== ELM_LIBRARY =================== */

float elm_getElement(vec2 position, sampler2D elements){
  return texture(elements, position).r;
}

void elm_SetElement(float value){
  gl_FragColor.r = value;
}

float elm_getPriority(vec2 position, sampler2D elements){
  return texture(elements, position).b;
}

void elm_setPriority(float value){
  gl_FragColor.b = value;
}

vec2 elm_getForce(vec2 position, sampler2D forces){
  return texture(forces, position).xy;
}

void elm_setForce(vec2 value){
  gl_FragColor.r = value.x;
  gl_FragColor.g = value.y;
}

float elm_getVelocityTick(vec2 position, sampler2D forces){
  return texture(forces, position).b;
}

void elm_setVelocityTick(float value){
  gl_FragColor.b = value;
}

float elm_getAvgElementUnit(vec2 position, sampler2D elements, sampler2D scalars, float elementType){
  float divisor = 0;
  float value = 0;
  for(float x = position.x - unitCoordinate; x <= position.x + unitCoordinate; x += unitCoordinate){
    for(float y = position.y - unitCoordinate; y <= position.y + unitCoordinate; y += unitCoordinate){
      vec2 n = vec2(x,y);
      if(coords_insideEdges(n)){
        if(elementType == elm_getElement(n, elements)){
          value += world_getUnit(n, scalars);
          divisor += 1;
        }
      }
    }
  }
  return value / divisor;
}

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

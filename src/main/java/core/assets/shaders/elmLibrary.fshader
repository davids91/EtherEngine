/* =================== ELM_LIBRARY =================== */

float elm_getElement(vec2 position, sampler2D elements){
  return texture2D(elements, position).r;
}
  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* elements */
  layout(binding=2)uniform sampler2D inputs2; /* scalars */

  <COORDINATES_LIBRARY>
  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float currentElement = eth_getElement(currentPosition.xy, inputs1);
    float currentUnit = world_getUnit(currentPosition.xy, inputs2);

    if(world_indexFire == currentElement){
      if(world_statePlasma == world_getState(currentElement, currentUnit)){
        if(currentUnit <= elm_getAvgElementUnit(currentPosition.xy, inputs1, inputs2, world_indexFire)){
          currentUnit *= currentUnit * 0.1f;
        }else{
          currentUnit -= currentUnit * 0.05f;
        }
      }
    }

    currentUnit = max(0.1, currentUnit);
    world_setUnit(currentUnit);
  }
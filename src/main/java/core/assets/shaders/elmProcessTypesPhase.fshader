  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* elements */
  layout(binding=2)uniform sampler2D inputs2; /* ethereal */
  layout(binding=2)uniform sampler2D inputs3; /* scalars */

  <COORDINATES_LIBRARY>
  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;
    float currentElement = eth_getElement(currentPosition.xy, inputs2);
    float currentUnit = world_getUnit(currentPosition.xy, inputs3);

    if(world_indexWater == currentElement){
      if(
        elm_getAvgElementUnit(currentPosition.xy, inputs1, inputs3, world_indexWater)
        < (elm_getAvgElementUnit(currentPosition.xy, inputs1, inputs3, world_indexFire) * 1.2)
      ){
        currentElement = world_indexAir;
      }
    }

    if(world_indexFire == currentElement){
      if(
        elm_getAvgElementUnit(currentPosition.xy, inputs1, inputs3, world_indexWater)
        > elm_getAvgElementUnit(currentPosition.xy, inputs1, inputs3, world_indexFire)
      ){
        currentElement = world_indexEarth;
      }
    }

    if(world_indexAir == currentElement){ }
    if(world_indexEarth == currentElement){ }

    elm_SetElement(currentElement);
    elm_setPriority(elm_getPriority(currentPosition.xy, inputs1));
  }
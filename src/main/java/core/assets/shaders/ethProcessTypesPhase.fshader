  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* etherValues */
  layout(binding=2)uniform sampler2D inputs2; /* elements */
  layout(binding=3)uniform sampler2D inputs3; /* scalars */

  <COORDINATES_LIBRARY>
  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float oldRatio = ( /* Ratio shall include a mixture of both Elemental, both Ethereal  targets */
     (0.8 * eth_getRatio(currentPosition.xy, inputs1))
     + (0.2 * world_RatioOf(elm_getElement(currentPosition.xy, inputs2)))
    );
    float etherUnit = eth_getUnit(currentPosition.xy, inputs1);
    float worldUnit = world_getUnit(currentPosition.xy, inputs3);
    float aeVal = eth_getAether(currentPosition.xy, inputs1);
    float neVal = eth_getNether(currentPosition.xy, inputs1);
    float newAe = (
      ( ((aeVal * aetherWeightInUnits) + neVal) * worldUnit )
      /((etherUnit * aetherWeightInUnits) + (etherUnit * oldRatio))
    );
    float newNe = newAe * oldRatio;
    eth_SetAether(newAe);
    eth_SetNether(newNe);
  }
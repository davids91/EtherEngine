  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* etherValues */
  layout(binding=2)uniform sampler2D inputs2; /* elements */
  layout(binding=3)uniform sampler2D inputs3; /* scalars */

  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    vec3 currentPosition = vec3(gl_FragCoord.x/chunkSize, gl_FragCoord.y/chunkSize, gl_FragCoord.z/chunkSize);
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;
    float oldRatio = eth_getRatio(currentPosition.xy, inputs1);
    float etherUnit = eth_getUnit(currentPosition.xy, inputs1);
    float worldUnit = world_getUnit(currentPosition.xy, inputs3);
    float aeVal = eth_getAether(currentPosition.xy, inputs1);
    float neVal = eth_getNether(currentPosition.xy, inputs1);
    float newAe = (
      ( ((aeVal * aetherWeightInUnits) + neVal) * worldUnit )
      /((etherUnit * aetherWeightInUnits) + (etherUnit * oldRatio))
    );
    float newNe = newAe * oldRatio;
    eth_SetAether(gl_FragColor, newAe);
    eth_SetNether(gl_FragColor, newNe);
  }
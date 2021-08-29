  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* releasedEther */
  layout(binding=2)uniform sampler2D inputs2; /* etherValues */

  <COORDINATES_LIBRARY>
  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float newAetherValue = (
      max(0.01,
        eth_getAether(currentPosition.xy, inputs2) - eth_getReleasedAether(currentPosition.xy, inputs1)
        + (eth_getAvgReleasedAether(currentPosition.xy, inputs1) * 0.9)
      )
    );
    float newNetherValue = (
      max(0.01,
        eth_getNether(currentPosition.xy, inputs2) - eth_getReleasedNether(currentPosition.xy, inputs1)
        + (eth_getAvgReleasedNether(currentPosition.xy, inputs1) * 0.9)
      )
    );
    eth_SetAether(newAetherValue);
    eth_SetNether(newNetherValue);
  }
  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* etherValues */

  <COORDINATES_LIBRARY>
  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float currentRatio = eth_getRatio(currentPosition.xy, inputs1);
    float releasedAe = 0;
    float releasedNe = 0;
    if(0.5 < abs(currentRatio - world_ratioEther)){
      float aeVal = eth_getAether(currentPosition.xy, inputs1);
      float neVal = eth_getNether(currentPosition.xy, inputs1);
      releasedAe = aeVal - eth_getMinAether(currentPosition.xy, inputs1);
      releasedNe = neVal - eth_getMaxNether(currentPosition.xy, inputs1);
      if(
        ( neVal >= (eth_getMaxNether(currentPosition.xy, inputs1) + (aeVal * etherReleaseThreshold)) )
        ||( aeVal >= (eth_getMinAether(currentPosition.xy, inputs1) + (neVal * etherReleaseThreshold)) )
      ){
        if(releasedNe >= releasedAe){
          releasedAe = 0;
          releasedNe = releasedNe / 9.0;
        }else{
          releasedAe = releasedAe / 9.0;
          releasedNe = 0;
        }
      }else{
        releasedAe = 0;
        releasedNe = 0;
      }
    } /* if( 0.5 < Math.abs(currentRatio - ether_ratio) ) */
    eth_SetReleasedAether(releasedAe);
    eth_SetReleasedNether(releasedNe);
  }
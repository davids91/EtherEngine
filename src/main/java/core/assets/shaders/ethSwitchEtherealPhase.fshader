  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* proposedChanges */
  layout(binding=2)uniform sampler2D inputs2; /* etherValues */

  <COORDINATES_LIBRARY>
  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float aeVal = eth_getAether(currentPosition.xy, inputs2);
    float neVal = eth_getNether(currentPosition.xy, inputs2);
    if(
      (coords_insideInnerBounds(currentPosition.xy))
      &&(0 != coords_getOffsetCode(currentPosition.xy, inputs1))
      &&(0 < coords_getToApply(currentPosition.xy, inputs1))
    ){
      vec2 target = vec2(
        coords_getTargetX(currentPosition.xy, inputs1),
        coords_getTargetY(currentPosition.xy, inputs1)
      );
      if( coords_insideEdges(target) ){
        aeVal = eth_getAether(target, inputs2);
        neVal = eth_getNether(target, inputs2);
      }
    }

    eth_SetAether(aeVal);
    eth_SetNether(neVal);
  }
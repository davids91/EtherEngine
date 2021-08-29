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
    vec3 currentPosition = vec3(gl_FragCoord.x/chunkSize, gl_FragCoord.y/chunkSize, gl_FragCoord.z/chunkSize);
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float aeVal = eth_getAether(currentPosition.xy, inputs2);
    float neVal = eth_getNether(currentPosition.xy, inputs2);
    if(
      (coords_insideInnerBounds(currentPosition.xy, chunkSize))
      &&(0 != coords_getOffsetCode(currentPosition.xy, inputs1))
      &&(0 < coords_getToApply(currentPosition.xy, inputs1))
    ){
      vec2 target = vec2(
        coords_getTargetX(currentPosition.xy, inputs1, chunkSize),
        coords_getTargetY(currentPosition.xy, inputs1, chunkSize)
      );
      if( coords_insideEdges(target, chunkSize) ){
        aeVal = eth_getAether(target, inputs2);
        neVal = eth_getNether(target, inputs2);
      }
      gl_FragColor.g = coords_getTargetX(currentPosition.xy, inputs1, chunkSize);
    }

    eth_SetAether(gl_FragColor, aeVal);
    eth_SetNether(gl_FragColor, neVal);
  }
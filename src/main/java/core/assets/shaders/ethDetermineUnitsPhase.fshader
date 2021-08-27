  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* etherValues */

  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>

  void main(void){
    vec3 currentPosition = vec3(gl_FragCoord.x/chunkSize, gl_FragCoord.y/chunkSize, gl_FragCoord.z/chunkSize);
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    world_setUnit(gl_FragColor, eth_getUnit(currentPosition.xy, inputs1));
  }
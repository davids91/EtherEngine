  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* elements */
  layout(binding=2)uniform sampler2D inputs2; /* scalars */

  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    vec3 currentPosition = vec3(gl_FragCoord.x/chunkSize, gl_FragCoord.y/chunkSize, gl_FragCoord.z/chunkSize);
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float currentUnits = world_getUnit(currentPosition.xy, inputs2);
    float currentElement = elm_getElement(currentPosition.xy, inputs1);
    float newAether = (2.0 * currentUnits) / (1.0 + world_RatioOf(currentElement));
    if(0 < currentUnits){
      eth_SetAether(gl_FragColor, newAether );
      eth_SetNether(gl_FragColor, (newAether * world_RatioOf(currentElement)) );
    }else{
      eth_SetAether(gl_FragColor, 1);
      eth_SetNether(gl_FragColor, world_ratioAir);
    }
  }
  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  uniform float chunkSize;
  layout(binding=1)uniform sampler2D inputs1; /* proposed changes */
  layout(binding=2)uniform sampler2D inputs2; /* elements */

  <COORDINATES_LIBRARY>
  <MATERIAL_LIBRARY>
  <WORLD_LIBRARY>
  <ETH_LIBRARY>
  <ELM_LIBRARY>

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float currentElement = elm_getElement(currentPosition.xy, inputs2);
    if(
        (coords_insideInnerBounds(currentPosition.xy))
        &&(0 != coords_getOffsetCode(currentPosition.xy, inputs1))
        &&(0 < coords_getToApply(currentPosition.xy, inputs1))
    ){
      vec2 target = vec2(
        coords_getTargetX(currentPosition.xy, inputs1, chunkSize),
        coords_getTargetY(currentPosition.xy, inputs1, chunkSize)
      );
      if( coords_insideEdges(target) ){
        currentElement = elm_getElement(target, inputs2);
      }
    }
    elm_SetElement(currentElement);
    elm_setPriority(elm_getPriority(currentPosition.xy, inputs2));
  }
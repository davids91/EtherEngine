#version 440
out vec4 gl_FragColor;
in vec4 gl_FragCoord;  /* Going from 0 to image size */
varying vec2 v_texCoords;

uniform float chunkSize;
layout(binding=1)uniform sampler2D inputs1; /* elements */
layout(binding=2)uniform sampler2D inputs2; /* forces */
layout(binding=3)uniform sampler2D inputs3; /* scalars */
layout(binding=4)uniform sampler2D inputs4; /* proposed changes */

<COORDINATES_LIBRARY>
<MATERIAL_LIBRARY>
<WORLD_LIBRARY>
<ELM_LIBRARY>

void main(void){
  gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

  float gravityCorrection = (
    elm_getToApply(currentPosition.xy, inputs4) * elm_getWeight(currentPosition.xy, inputs1, inputs3)
  );
  vec2 force = elm_getForce(currentPosition.xy, inputs2);

  if(
    (coords_insideInnerBounds(currentPosition.xy))
    &&(0 < gravityCorrection)&&world_isCellMovable(elm_getElement(currentPosition.xy, inputs1), world_getUnit(currentPosition.xy, inputs3))
  ){
    gravityCorrection = min(1, gravityCorrection);
    force += world_gravity * gravityCorrection;
  }

  elm_setForce(force);
}
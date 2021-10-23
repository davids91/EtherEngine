#version 440
out vec4 gl_FragColor;
in vec4 gl_FragCoord;  /* Going from 0 to image size */
varying vec2 v_texCoords;

uniform float chunkSize;
layout(binding=1)uniform sampler2D inputs1; /* proposed changes */
layout(binding=2)uniform sampler2D inputs2; /* elements */
layout(binding=3)uniform sampler2D inputs3; /* scalars */

<COORDINATES_LIBRARY>
<MATERIAL_LIBRARY>
<WORLD_LIBRARY>
<ELM_LIBRARY>

void main(void){
  gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

  float toApply = elm_getToApply(currentPosition.xy, inputs1);
  int offsetCode = coords_getOffsetCode(currentPosition.xy, inputs1);
  float velocityTick = elm_getVelocityTick(currentPosition.xy, inputs1);

  int targetX = coords_getIntXFromOffsetCode(int(gl_FragCoord.x), offsetCode);
  int targetY = coords_getIntYFromOffsetCode(int(gl_FragCoord.y), offsetCode);
  vec2 target = vec2( ((targetX + 0.5) / chunkSize), ((targetY + 0.5) / chunkSize) );
  vec2 positionOfT = vec2((targetX+0.5)/chunkSize, (targetY+0.5)/chunkSize);
  int offsetCodeOfT = coords_getOffsetCode(positionOfT, inputs1);
  int targetTX = coords_getIntXFromOffsetCode(targetX, offsetCodeOfT);
  int targetTY = coords_getIntYFromOffsetCode(targetY, offsetCodeOfT);
  if( /* Check for swaps */
    coords_insideInnerBounds(currentPosition.xy)
    &&(0 < coords_getToApply(currentPosition.xy, inputs1))&&(0 != coords_getOffsetCode(currentPosition.xy, inputs1))
    &&(coords_insideEdges(target))
    &&((int(gl_FragCoord.x) == targetTX) && (int(gl_FragCoord.y) == targetTY)) /* and the target is mutual --> no conflicts are found */
  ){
    if(
      ((2 == toApply)&&(!elm_ACanMoveB(currentPosition.xy, target, inputs2, inputs3)))
      ||((1 == toApply)&&(!elm_ACanMoveB(target, currentPosition.xy, inputs2, inputs3)))
      ||(
        (!elm_ACanMoveB(currentPosition.xy, target, inputs2, inputs3))
        &&((int(gl_FragCoord.x) != targetTX)||(int(gl_FragCoord.y) != targetTY))
      )
    ){
      toApply = 0;
    }
  }

  coords_setOffsetCode(float(offsetCode));
  elm_setToApply(toApply);
  elm_setVelocityTick(velocityTick);
}
#version 440
out vec4 gl_FragColor;
in vec4 gl_FragCoord;  /* Going from 0 to image size */
varying vec2 v_texCoords;

uniform float chunkSize;
layout(binding=1)uniform sampler2D inputs1; /* previously proposed changes */
layout(binding=2)uniform sampler2D inputs2; /* elements */
layout(binding=3)uniform sampler2D inputs3; /* forces */
layout(binding=4)uniform sampler2D inputs4; /* scalars */

<COORDINATES_LIBRARY>
<MATERIAL_LIBRARY>
<WORLD_LIBRARY>
<ETH_LIBRARY>
<ELM_LIBRARY>

const int indexRadius = 1;//2;
const int indexTableSize = (indexRadius * 2) + 1;

void main(void){
  gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

  float priority[indexTableSize][indexTableSize];
  bool changed[indexTableSize][indexTableSize];
  int offsetCode = 0;
  float toApply = 0;
  float velocityTick = elm_getVelocityTick(currentPosition.xy, inputs1);

  int minIndexX = max(int(gl_FragCoord.x) - indexRadius, 0);
  int maxIndexX = min(int(gl_FragCoord.x) + indexRadius, int(chunkSize));
  int minIndexY = max(int(gl_FragCoord.y) - indexRadius, 0);
  int maxIndexY = min(int(gl_FragCoord.y) + indexRadius, int(chunkSize));

  for(int ix = minIndexX; ix <= maxIndexX; ++ix){ for(int iy = minIndexY; iy <= maxIndexY; ++iy) {
    int sx = int(ix - gl_FragCoord.x + (indexRadius));
    int sy = int(iy - gl_FragCoord.y + (indexRadius)); /* TODO: Include Velocity tick into arbitration logic */
    priority[sx][sy] = ( /* The priority of the given cell consist of..  */
        length(elm_getForce(currentPosition.xy, inputs3)) /* ..the power of the force on it.. */
        + elm_getWeight(currentPosition.xy, inputs2, inputs4) /* .. and its weight */
    );
    changed[sx][sy] = false;
  }}

  vec2 highestPrio = vec2(-2,-2);
  vec2 highestTarget = vec2(-2,-2);

  while(true){
    highestPrio = vec2(-2,-2);
    highestTarget = vec2(-2,-2);
    int localSourceX; /* The highest priority change in the local vicinity.. */
    int localSourceY; /* ..corresponding to highestPrio*, which is of global index scope */
    int localTargetOfCX;
    int localTargetOfCY;
    int highestPrioLocalX = -2;
    int highestPrioLocalY = -2;
    int highestPrioTargetLocalX = -2;
    int highestPrioTargetLocalY = -2;
    for(int ix = minIndexX; ix <= maxIndexX; ++ix){ for(int iy = minIndexY; iy <= maxIndexY; ++iy) {
      vec2 positionOfC = vec2((ix+0.5)/chunkSize, (iy+0.5)/chunkSize);
      int offsetCodeOfC = coords_getOffsetCode(positionOfC, inputs1);
      int targetOfCX = coords_getIntXFromOffsetCode(ix,offsetCodeOfC);
      int targetOfCY = coords_getIntYFromOffsetCode(iy,offsetCodeOfC);
      localSourceX = ix - int(gl_FragCoord.x) + indexRadius;
      localSourceY = iy - int(gl_FragCoord.y) + indexRadius;
      localTargetOfCX = targetOfCX - int(gl_FragCoord.x) + indexRadius;
      localTargetOfCY = targetOfCY - int(gl_FragCoord.y) + indexRadius;
      if( /* The highest priority swap request is.. */
        ( /* ..the one which isn't changed yet (only higher priority changes occurred prior to this loop )  */
          (!changed[localSourceX][localSourceY])
          &&( /* And either the target is out of bounds.. */
            (localTargetOfCX < 0)||(localTargetOfCX >= indexTableSize)
            ||(localTargetOfCY < 0)||(localTargetOfCY >= indexTableSize)
            ||(!changed[localTargetOfCX][localTargetOfCY]) /* ..or not changed yet */
          )
        )&&( /* ..and of course the currently examined index has to have a higher prio target, then the previous highest one */
          ((-2 == highestPrioLocalX)||(-2 == highestPrioLocalY))
          ||(
            (priority[highestPrioLocalX][highestPrioLocalY] < priority[localSourceX][localSourceY])
            ||(
              (priority[highestPrioLocalX][highestPrioLocalY] == priority[localSourceX][localSourceY])
              &&(
                priority[highestPrioLocalX][highestPrioLocalY]
                < (priority[localSourceX][localSourceY] + elm_getPriority(positionOfC, inputs2))
              )
            )
          )
        )
      ){
        /*!Note: Sampling needs to be done from the middle of the pixel; This is why there's a 0.5 offset */
        highestPrio = vec2(
          (ix+0.5)/chunkSize,
          (iy+0.5)/chunkSize
        );
        highestTarget = vec2(
          ((targetOfCX + 0.5) / chunkSize),
          ((targetOfCY + 0.5) / chunkSize)
        );
        highestPrioLocalX = localSourceX;
        highestPrioLocalY = localSourceY;
        highestPrioTargetLocalX = localTargetOfCX;
        highestPrioTargetLocalY = localTargetOfCY;
      }
    }}

    /* If c was reached; or no changes are proposed; break! */
    if(
        (highestPrio == currentPosition.xy)
        ||(highestTarget == currentPosition.xy)
        ||((-2 == highestPrio.x)&&(-2 == highestPrio.y))
        ||((-2 == highestTarget.x)&&(-2 == highestTarget.y))
    ){
      if(highestPrio == currentPosition.xy){
        toApply = 2;
        offsetCode = coords_getOffsetCodeFromOffsetVector(vec2(
          round(highestTarget.x * chunkSize - gl_FragCoord.x),
          round(highestTarget.y * chunkSize - gl_FragCoord.y)
        ));
      }else if(highestTarget == currentPosition.xy){
        toApply = 1;
        offsetCode = coords_getOffsetCodeFromOffsetVector(vec2(
          round(highestPrio.x * chunkSize - gl_FragCoord.x),
          round(highestPrio.y * chunkSize - gl_FragCoord.y)
        ));
      }
      /*!Note: Only set the target if the current cell is actually the highest priority;
       * because the swap will be decided for the target(other cell) based on this information
       * */
      break;
    }

    /* Loop continues.. Simulate the highest priority swap */
    if((-2 != highestPrioLocalX)&&(-2 != highestPrioLocalY)){
        changed[highestPrioLocalX][highestPrioLocalY] = true;
        if(
            (highestPrioTargetLocalX >= 0)&&(highestPrioTargetLocalX < indexTableSize)
            &&(highestPrioTargetLocalY >= 0)&&(highestPrioTargetLocalY < indexTableSize)
        ){
            changed[highestPrioTargetLocalX][highestPrioTargetLocalY] = true;
        }
    }
  }

  coords_setOffsetCode(float(offsetCode));
  elm_setToApply(toApply);
  elm_setVelocityTick(velocityTick);
}
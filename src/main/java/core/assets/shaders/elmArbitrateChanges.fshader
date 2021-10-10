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

const int indexRadius = 2;
const int indexTableSize = (indexRadius * 2) + 1;

void main(void){
  gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

  float priority[indexTableSize][indexTableSize];
  bool changed[indexTableSize][indexTableSize];
  int offsetCode = 0;
  float toApply = 0;
  float velocityTick = elm_getVelocityTick(currentPosition.xy, inputs1);

  int minIndexX = max(int(gl_FragCoord.x) - indexRadius, 0);
  int maxIndexX = min(int(gl_FragCoord.x) + indexRadius, int(chunkSize-1));
  int minIndexY = max(int(gl_FragCoord.y) - indexRadius, 0);
  int maxIndexY = min(int(gl_FragCoord.y) + indexRadius, int(chunkSize-1));
  int offsetCodeOfC = coords_getOffsetCode(currentPosition.xy, inputs1);
  int targetOfCX = coords_getIntXFromOffsetCode(int(gl_FragCoord.x),offsetCodeOfC);
  int targetOfCY = coords_getIntYFromOffsetCode(int(gl_FragCoord.y),offsetCodeOfC);
  for(int ix = 0; ix < indexTableSize; ++ix){ for(int iy = 0; iy < indexTableSize; ++iy) {
    changed[ix][iy] = true;
  }}

  priority[indexRadius][indexRadius] = elm_getDynamicPrio(currentPosition.xy, inputs2, inputs3, inputs4);
  changed[indexRadius][indexRadius] = false;
  for(int ix = minIndexX; ix <= maxIndexX; ++ix){ for(int iy = minIndexY; iy <= maxIndexY; ++iy) {
    vec2 positionOfT = vec2((ix+0.5)/chunkSize, (iy+0.5)/chunkSize);
    int offsetCodeOfT = coords_getOffsetCode(positionOfT, inputs1);
    int targetOfTX = coords_getIntXFromOffsetCode(ix,offsetCodeOfT);
    int targetOfTY = coords_getIntYFromOffsetCode(iy,offsetCodeOfT);
    if((ix != int(gl_FragCoord.x))||(iy != int(gl_FragCoord.y))){
      int sx = int(ix - int(gl_FragCoord.x) + (indexRadius));
      int sy = int(iy - int(gl_FragCoord.y) + (indexRadius)); /* TODO: Include Velocity tick into arbitration logic */
      priority[sx][sy] = elm_getDynamicPrio(positionOfT, inputs2, inputs3, inputs4);
      if(priority[sx][sy] < priority[indexRadius][indexRadius]) { /* if priority is lower than c mark it changed, because it's irrelevant to the calculation.. */
        if( (targetOfCX != ix) || (targetOfCY != iy) ){ /* ..but only if C is not targeting it */
            changed[sx][sy] = true;
        }else{
            changed[sx][sy] = false;
        }
      }else if(priority[sx][sy] > priority[indexRadius][indexRadius]){ /* if priority is higher than C.. */
          vec2 positionOfTT = vec2((targetOfTX+0.5)/chunkSize, (targetOfTY+0.5)/chunkSize);
          int offsetCodeOfTT = coords_getOffsetCode(positionOfTT, inputs1);
          int targetOfTTX = coords_getIntXFromOffsetCode(targetOfTX,offsetCodeOfTT);
          int targetOfTTY = coords_getIntYFromOffsetCode(targetOfTY,offsetCodeOfTT);
          if(
            ((ix != targetOfCX) || (iy != targetOfCY)) /* ..but only if it's the target of C */

            &&((targetOfTX != int(gl_FragCoord.x)) || (targetOfTY != int(gl_FragCoord.y))) /* ..and target is not c, then mark it changed.. */
            &&((targetOfTX != targetOfCX) || (targetOfTY != targetOfCY)) /* ..but only if it's not targeting the target of C */

            &&((targetOfTTX != int(gl_FragCoord.x)) || (targetOfTTY != int(gl_FragCoord.y))) /* The same goes for the target of the target of the target */
            &&((targetOfTTX != targetOfCX) || (targetOfTTY != targetOfCY))
          ){
              changed[sx][sy] = true;
          }else{
              changed[sx][sy] = false;
          }
      }else changed[sx][sy] = false;
    }
  }}

  vec2 highestPrio = vec2(-2,-2);
  vec2 highestTarget = vec2(-2,-2);
  int loopCounter = 0;
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
      offsetCodeOfC = coords_getOffsetCode(positionOfC, inputs1);
      targetOfCX = coords_getIntXFromOffsetCode(ix,offsetCodeOfC);
      targetOfCY = coords_getIntYFromOffsetCode(iy,offsetCodeOfC);
      localSourceX = ix - int(gl_FragCoord.x) + indexRadius;
      localSourceY = iy - int(gl_FragCoord.y) + indexRadius;
      localTargetOfCX = targetOfCX - int(gl_FragCoord.x) + indexRadius;
      localTargetOfCY = targetOfCY - int(gl_FragCoord.y) + indexRadius;
      if(changed[localSourceX][localSourceY]) continue;

      if( /* The highest priority swap request is.. */
        (!changed[localSourceX][localSourceY]) /* ..the one which isn't changed yet (only higher priority changes occurred prior to this loop )  */
        &&( /* ..and of course the currently examined index has to have a higher prio target, then the previous highest one */
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
      ((-2 == highestPrio.x)&&(-2 == highestPrio.y))
      ||((-2 == highestTarget.x)&&(-2 == highestTarget.y))
      ||(
        (
          (currentPosition.xy == highestPrio)
          ||(currentPosition.xy == highestTarget)
        )
        &&(!changed[highestPrioLocalX][highestPrioLocalY])
        &&(!changed[highestPrioTargetLocalX][highestPrioTargetLocalY])
      )
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
    if(loopCounter < 25)++loopCounter;
    else break; /* TODO: Find out why there is a chance of an infinite cycle... */
  }

  coords_setOffsetCode(float(offsetCode));
  elm_setToApply(toApply);
  elm_setVelocityTick(velocityTick);
}
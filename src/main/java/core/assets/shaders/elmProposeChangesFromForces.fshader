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

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

    float currentElement = elm_getElement(currentPosition.xy, inputs2);
    float currentUnit = world_getUnit(currentPosition.xy, inputs4);
    float velocityTick = elm_getVelocityTick(currentPosition.xy, inputs1);
    vec2 target = vec2(
      coords_getTargetX(currentPosition.xy, inputs1, chunkSize),
      coords_getTargetY(currentPosition.xy, inputs1, chunkSize)
    );
    vec2 force = elm_getForce(currentPosition.xy, inputs3);

    if(
      (0 == elm_getToApply(currentPosition.xy, inputs1))
      &&(!world_isCellDiscardable(currentElement, currentUnit))
      &&(1 <= length(force))
    ){
      if(1 < abs(force.x)){
        target.x = (max(-unitCoordinate, min(unitCoordinate, force.x)));
        target.x = currentPosition.x + (target.x / chunkSize);
      }
      if(1 < abs(force.y)){
        target.y = (max(-unitCoordinate, min(unitCoordinate, force.y)));
        target.y = currentPosition.y + (target.y / chunkSize);
      }
      target.x = max(0,min(1, target.x));
      target.y = max(0,min(1, target.y));

      vec2 targetFinalPos = target;
      vec2 forceOfTarget = elm_getForce(target.xy, inputs3);
      if(1 < abs(forceOfTarget.x))
        targetFinalPos.x = target.x + max((-1.1 / chunkSize), min((1.1 / chunkSize), forceOfTarget.x));
      if(1 < abs(forceOfTarget.y))
        targetFinalPos.y = target.y + max((-1.1 / chunkSize), min((1.1 / chunkSize), forceOfTarget.y));

      if((2*unitCoordinate) > length(currentPosition.xy - targetFinalPos)){
        float targetElement = elm_getElement(currentPosition.xy, inputs2);
        float targetUnit = world_getUnit(currentPosition.xy, inputs4);
        if(
          (currentPosition.xy != target)
          &&(
            world_isCellDiscardable(currentElement, currentUnit)
            &&world_isCellDiscardable(targetElement, targetUnit)
          )
        ){
          target = currentPosition.xy;
        }else if(velocityMaxTicks > velocityTick){
          velocityTick += 1.0;
          target = currentPosition.xy;
        }
      }else{
        target = currentPosition.xy;
      }
    }

    elm_setVelocityTick(velocityTick);
    coords_setOffsetCode(coords_getOffsetCodeFromOffsetVector(target - currentPosition.xy));
    elm_setToApply(0);
  }
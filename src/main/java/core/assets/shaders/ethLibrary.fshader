/* =================== ETH_LIBRARY =================== */
const float etherReleaseThreshold = 0.1;

float eth_getAether(vec2 position, sampler2D etherValues){
  return texture(etherValues, position).b;
}

float eth_getNether(vec2 position, sampler2D etherValues){
  return texture(etherValues, position).r;
}

void eth_SetAether(float value){
  gl_FragColor.b = value;
}

void eth_SetNether(float value){
  gl_FragColor.r = value;
}

float eth_getReleasedAether(vec2 position, sampler2D etherValues){
  return texture(etherValues, position).b;
}

float eth_getReleasedNether(vec2 position, sampler2D etherValues){
  return texture(etherValues, position).r;
}

float eth_getAvgReleasedAether(vec2 position, sampler2D etherValues){
  float divisor = 0;
  float value = 0;
  for(float x = position.x - unitCoordinate; x <= position.x + unitCoordinate; x += unitCoordinate){
    for(float y = position.y - unitCoordinate; y <= position.y + unitCoordinate; y += unitCoordinate){
      vec2 n = vec2(x,y);
      if(coords_insideEdges(n)){
        value += eth_getReleasedAether(n, etherValues);
        divisor += 1;
      }
    }
  }
  return value / divisor;
}

float eth_getAvgReleasedNether(vec2 position, sampler2D etherValues){
  float divisor = 0;
  float value = 0;
  for(float x = position.x - unitCoordinate; x <= position.x + unitCoordinate; x += unitCoordinate){
    for(float y = position.y - unitCoordinate; y <= position.y + unitCoordinate; y += unitCoordinate){
      vec2 n = vec2(x,y);
      if(coords_insideEdges(n)){
        value += eth_getReleasedNether(n, etherValues);
        divisor += 1;
      }
    }
  }
  return value / divisor;
}

void eth_SetReleasedAether(float value){
  gl_FragColor.b = value;
}

void eth_SetReleasedNether(float value){
  gl_FragColor.r = value;
}

const float aetherWeightInUnits = 4;
float eth_getUnit(vec2 position, sampler2D etherValues){
  float aeVal = eth_getAether(position, etherValues);
  float neVal = eth_getNether(position, etherValues);
  return ( ((aeVal * aetherWeightInUnits) + neVal) / (aetherWeightInUnits + 1) );
}

float eth_getRatio(vec2 position, sampler2D etherValues){
  float aeVal = eth_getAether(position, etherValues);
  float neVal = eth_getNether(position, etherValues);
  if(0 < aeVal) return (neVal / aeVal);
    else return 0;
}

float eth_getMaxNether(vec2 position, sampler2D etherValues){
  return eth_getAether(position, etherValues) * world_ratioFire;
}

float eth_getMinAether(vec2 position, sampler2D etherValues){
  return eth_getNether(position, etherValues) / world_ratioEarth;
}

float eth_getElement(vec2 position, sampler2D etherValues){
  float ratio = eth_getRatio(position, etherValues);
  if(eth_getUnit(position, etherValues) <= world_ratioFire){
    return world_indexAir;
  }else if(0.05 > abs(ratio - world_ratioEther)){
    return world_indexEther;
  }else if(ratio <= ((world_ratioEarth + world_ratioWater)/2.0)){
    return world_indexEarth;
  }else if(ratio <= ((world_ratioWater + world_ratioAir)/2.0)){
    return world_indexWater;
  }else if(ratio <= ((world_ratioAir + world_ratioFire)/2.0)){
    return world_indexAir;
  }else{
    return world_indexFire;
  }
}
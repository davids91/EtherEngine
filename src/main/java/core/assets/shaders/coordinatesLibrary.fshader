/* =================== COORDINATES_LIBRARY =================== */
const vec3 currentPosition = gl_FragCoord.xyz / chunkSize;
const float unitCoordinate = 1/chunkSize;

int coords_getOffsetCode(vec2 position, sampler2D proposedChanges){
  return int(texture(proposedChanges, position).r);
}

void coords_setOffsetCode(float value){
  gl_FragColor.r = value;
}

float coords_getToApply(vec2 position, sampler2D proposedChanges){
  return(texture(proposedChanges, position).g);
}

int coords_getOffsetCodeFromOffsetVector(vec2 v){
   if((v.x < 0)&&(v.y < 0)) return 1;
   if((v.x == 0)&&(v.y < 0)) return 2;
   if((v.x > 0)&&(v.y < 0)) return 3;
   if((v.x > 0)&&(v.y == 0)) return 4;
   if((v.x > 0)/*&&(v.y > 0)*/) return 5;
   if((v.x == 0)&&(v.y > 0)) return 6;
   if((v.x < 0)&&(v.y > 0)) return 7;
   if((v.x < 0)/*&&(v.y == 0)*/) return 8;
   return 0;
}

float coords_getXFromOffsetCode(float x, int code){
    switch(code){
        case 1: case 8: case 7: return (x-unitCoordinate);
        case 2: case 0: case 6: return (x);
        case 3: case 4: case 5: return (x+unitCoordinate);
    }
    return x;
}

float coords_getYFromOffsetCode(float y, int code){
    switch(code){
        case 7: case 6: case 5: return (y+unitCoordinate);
        case 8: case 0: case 4: return (y);
        case 1: case 2: case 3: return (y-unitCoordinate);
    }
    return y;
}

int coords_getIntXFromOffsetCode(int x, int code){
    switch(code){
        case 1: case 8: case 7: return (x-1);
        case 2: case 0: case 6: return (x);
        case 3: case 4: case 5: return (x+1);
    }
    return x;
}

int coords_getIntYFromOffsetCode(int y, int code){
    switch(code){
        case 7: case 6: case 5: return (y+1);
        case 8: case 0: case 4: return (y);
        case 1: case 2: case 3: return (y-1);
    }
    return y;
}

float coords_getTargetX(vec2 position, sampler2D proposedChanges){
  int offsetCode = coords_getOffsetCode(position, proposedChanges);
  return coords_getXFromOffsetCode(position.x, offsetCode);
}

float coords_getTargetY(vec2 position, sampler2D proposedChanges){
  int offsetCode = coords_getOffsetCode(position, proposedChanges);
  return coords_getYFromOffsetCode(position.y, offsetCode);
}

vec2 coords_getTarget(vec2 position, sampler2D proposedChanges){
  return vec2(
    coords_getTargetX(position, proposedChanges),
    coords_getTargetY(position, proposedChanges)
  );
}

bool coords_insideInnerBounds(vec2 position){
  return (
    (unitCoordinate < position.x)&&(1-unitCoordinate > position.x)
    &&(unitCoordinate < position.y)&&(1-unitCoordinate > position.y)
  );
}

bool coords_insideEdges(vec2 position){
  return ( (0 <= position.x)&&(1 > position.x)&&(0 <= position.y)&&(1 > position.y) );
}
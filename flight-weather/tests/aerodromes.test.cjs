const assert=require('node:assert/strict');
global.Engine=require('../app/src/main/assets/engine.js');
const A=require('../app/src/main/assets/aerodromes.js');
for(const type of ['metar','taf']){
 assert.equal(A.nearest({lat:61.342,lon:73.422},type).icao,'USRR');
 assert.equal(A.nearest({lat:66.074,lon:76.5},type).icao,'USMU');
 for(const point of [{lat:65.79,lon:87.96},{lat:68.1,lon:33.1},{lat:69.5,lon:161.4}]){
  const nearest=A.nearest(point,type);assert(nearest.types.includes(type.toUpperCase()));
  for(const s of A.stations.filter(s=>s.types.includes(type.toUpperCase())))assert(nearest.distance<=Engine.routePoints(point,s).distance+1e-8);
 }
}
console.log('ICAO: ближайшие станции выбираются по расстоянию и доступности METAR/TAF');

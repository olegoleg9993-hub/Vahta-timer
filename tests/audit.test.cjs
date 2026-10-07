const assert=require('node:assert/strict'),E=require('../app/src/main/assets/engine.js');
const make=()=>({...E.assess({visibility:[5000],wind_speed_10m:[8],wind_gusts_10m:[10],precipitation:[.3],temperature_2m:[5],weather_code:[0],wind_direction_10m:[90],cloud_cover_low:[50],is_day:[1]},0),cloud:{known:true,value:400,clear:false,source:'GFS'}});
// Removing an input must not lower the upper bound, and must mark it incomplete.
for(const key of ['visibility','wind','gust','rain','temp','code','direction','low','day','cloud']){const a=Array.from({length:5},make),full=E.indexDetails(a,E.defaults);if(key==='cloud')a[2].cloud=null;else a[2].x[key]=null;const partial=E.indexDetails(a,E.defaults);assert(partial.partial,key);assert(partial.score>=full.score,key);}
let r=make();r.x.wind=-1;assert(E.indexDetails([r],E.defaults).partial);r=make();r.cloud.value=-10;assert(E.indexDetails([r],E.defaults).partial);r=make();r.x.temp=273;assert(E.indexDetails([r],E.defaults).partial);
assert.equal(E.indexDetails([E.assess({},-1)],E.defaults).score,null);
for(const v of [0,1,5,12,15,19,20,100]){const p=E.severity(v,20,12);assert(p>=0&&p<=1);}
const t=Date.parse('2026-10-07T00:00Z'),taf={icaoId:'TEST',validTimeFrom:t/1000,validTimeTo:t/1000+7200,fcsts:[{timeFrom:t/1000,timeTo:t/1000+3600,clouds:[{cover:'OVC',base:200}]},{timeFrom:t/1000+3600,timeTo:t/1000+7200,fcstChange:'FM',wspd:5,clouds:[]}]};assert.equal(E.ceilingOf(E.tafAt(taf,t+3600000).variants[0]).known,false);
const data={hourly:{time:['2026-10-07T00:00','2026-10-07T03:00'],visibility:[5000,5000],wind_speed_10m:[5,5]}};assert.equal(E.sampleModel(data,t+3600000).x.visibility,null);
assert.equal(E.assess({visibility:[1000]},0).g.visibility,2);assert.equal(E.assess({visibility:[3000]},0).g.visibility,1);
// Deterministic property sweep: index remains finite in 0..100 and decreases with stronger wind.
for(let v=0;v<=20;v+=.25){r=make();r.x.wind=v;let value=E.weatherIndex([r],E.defaults);assert(Number.isInteger(value)&&value>=0&&value<=100);const next={...r,x:{...r.x,wind:v+.25}};assert(E.weatherIndex([next],E.defaults)<=value);}
console.log('Аудит: верхняя граница при пропусках, физические диапазоны, границы порогов, FM TAF, пропуск срока, монотонность и 0..100 — пройдены');

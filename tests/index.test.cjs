const assert=require('node:assert/strict'),E=require('../app/src/main/assets/engine.js');
function point(wind=5,ceiling=1000,code=0){const r=E.assess({visibility:[10000],wind_speed_10m:[wind],wind_gusts_10m:[8],precipitation:[.2],temperature_2m:[5],weather_code:[code],wind_direction_10m:[90],cloud_cover_low:[100],is_day:[1]},0);return E.withCloud(r,{known:true,value:ceiling,clear:false,source:'GFS',approximate:true},E.defaults);}
const a=point();assert.equal(E.weatherIndex([a],E.defaults),63); // 100 * (1-5/30) * (1-8/45) * (1-.08)
const scores=[1,3,5,8,12].map(v=>E.weatherIndex([point(v)],E.defaults));assert.equal(new Set(scores).size,5);assert(scores.every((v,i)=>i===0||v<scores[i-1]));
assert.equal(E.weatherIndex([point(5,1000,71)],E.defaults),E.weatherIndex([a],E.defaults));assert.equal(E.weatherIndex([point(5,1000,45)],E.defaults),E.weatherIndex([a],E.defaults));assert.equal(E.weatherIndex([point(5,1000,95)],E.defaults),0);
assert.equal(E.weatherIndex([point(20)],E.defaults),0);assert.equal(E.weatherIndex([point(5,100)],E.defaults),0);
const results=Array.from({length:5},()=>point());results[2].cloud=null;assert.equal(E.weatherIndex(results,E.defaults),null);assert.deepEqual(E.indexDetails(results,E.defaults).missing[0],{name:'Высота облаков / облачный потолок',points:[2]});
for(const key of ['visibility','wind','gust','rain','temp','code','direction','day','low']){const r=point();r.x[key]=null;assert.equal(E.weatherIndex([r],E.defaults),null,key);}
const mixed=[point(2,1000),point(3,500),point(4,400),point(5,300),point(6,250)];assert.equal(E.weatherIndex(mixed,E.defaults),Math.round(Math.min(...E.indexDetails(mixed,E.defaults).points.map(x=>x.raw))));
assert.equal(E.weatherIndex([],E.defaults),null);assert(!point().reasons.some(x=>x.includes('неизвестна')));assert(E.weatherIndex([a],E.defaults)>E.weatherIndex([point(5,350)],E.defaults));
console.log('Индекс v0.4: плавность, формула, все обязательные поля, пропуск облаков в одной точке, причины нуля, худшая точка — пройдены');

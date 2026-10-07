const assert=require('node:assert/strict'),fs=require('node:fs'),E=require('../app/src/main/assets/engine.js'),F=require('../app/src/main/assets/fallback.js');
const point={lat:61.342,lon:73.422},csv=fs.readFileSync(__dirname+'/fixtures/gfs-surface.csv','utf8');
for(const missing of ['NaN','','null','1e38','-1']){const rows=F.parse(csv.replace('8.0E-6',missing),'surface',point);assert.equal(rows[0].x.rain,null,'missing rain '+missing);}
const h={visibility:[-10],wind_speed_10m:[1e38],wind_gusts_10m:[null],temperature_2m:[273],precipitation:['0'],weather_code:[12],is_day:[.5]};const bad=E.assess(h,0);for(const k of ['visibility','wind','gust','temp','rain','code','day'])assert.equal(bad.x[k],null,k);
const ms=Date.parse('2026-10-07T00:00Z'),groups={surface:[{ms,x:{visibility:9000,gust:6,rain:0}}],temp:[{ms,x:{temp:2}}],wind:[{ms,x:{wind:4,direction:90}}]};const merged=F.merge(bad,groups,ms,E.defaults);assert.equal(merged.x.visibility,9000);assert.equal(merged.x.wind,4);assert.equal(merged.x.rain,0);assert.equal(merged.sources.visibility,'~ GFS');assert.equal(E.coverage([]).available,0);
for(const [raw,miles] of [['1/2',.5],['1 1/2',1.5],['P6',6],['6+',6],[6,6]])assert.equal(E.visibilityMeters(raw),miles*1609.344);
for(const raw of ['1foo','-1','1/0','',null,Infinity])assert.equal(E.visibilityMeters(raw),null);
const taf={icaoId:'TEST',validTimeFrom:ms/1000,validTimeTo:ms/1000+3600,fcsts:[{timeFrom:ms/1000,timeTo:ms/1000+3600,visib:'1/2',wspd:20,wgst:null,clouds:[{cover:'OVC',base:500}]}]};const augmented=E.augment(merged,taf,ms,E.defaults);assert.equal(augmented.x.visibility,.5*1609.344);assert.equal(augmented.sources.visibility,'TAF TEST');assert.equal(augmented.sources.wind,'TAF TEST');assert.equal(merged.sources.visibility,'~ GFS');assert.equal(augmented.x.gust,6);assert.equal(E.ceilingOf({vertVis:-100}).known,false);
// Property sweeps cover all scoring inputs, not merely one example.
for(const [key,lo,hi,step] of [['wind',0,20,.2],['gust',0,25,.25],['rain',0,3,.03],['visibility',0,10000,100],['cloud',0,1200,10]]){let previous=null;for(let v=lo;v<=hi;v+=step){const r=E.withCloud(E.assess({visibility:[10000],wind_speed_10m:[0],wind_gusts_10m:[0],precipitation:[0]},0),{known:true,value:1000,clear:false},E.defaults);if(key==='cloud')r.cloud.value=v;else r.x[key]=v;const score=E.indexDetails([r],E.defaults).score;assert(Number.isInteger(score)&&score>=0&&score<=100);if(previous!==null)assert(['visibility','cloud'].includes(key)?score>=previous:score<=previous,key);previous=score;}}
console.log('v16 audit: missing rain, corrupt primary/fallback replacement, coverage, TAF fractions/provenance/invalid heights, all-factor monotonicity passed');

assert.equal(E.severity(4,20,12),0);assert.equal(E.severity(.03,3,1),0);assert.equal(E.severity(434.5,150,300,true),0);assert.equal(E.severity(16,20,12),.5);assert.equal(E.severity(225,150,300,true),.5);
const corrupt={hourly:{time:['2026-10-07T00:00','2026-10-07T01:00'],temperature_2m:[-200,200]}};assert.equal(E.sampleModel(corrupt,ms+1800000).x.temp,null);

assert.throws(()=>F.parse(csv.replace('unit="kg.m-2.s-1"','unit="mm"'),'surface',point),/единицы/);
assert.equal(E.sampleModel({utc_offset_seconds:3600,hourly:{time:['2026-10-07T00:00'],visibility:[9000]}},ms).x.visibility,null);
assert.equal(E.sampleModel({hourly_units:{visibility:'km'},hourly:{time:['2026-10-07T00:00'],visibility:[9]}},ms).x.visibility,null);

assert.equal(E.tafAt({...taf,validTimeTo:null},ms),null);assert.equal(E.tafAt({...taf,validTimeFrom:undefined},ms),null);assert.equal(E.tafAt({...taf,fcsts:[{...taf.fcsts[0],timeTo:ms/1000+1}]},ms+2000),null);

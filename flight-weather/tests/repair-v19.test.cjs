const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const E=require('../app/src/main/assets/engine.js'),F=require('../app/src/main/assets/fallback.js');
const hour=3600000,at=Date.parse('2026-10-08T00:00Z');
test('a low next forecast must not become three hours of constant low ceiling/visibility',()=>{
 const rows=[{ms:at,value:900},{ms:at+3*hour,value:10}];
 assert(Math.abs(E.cloudAt(rows,at+hour).value-603.333333)<.0001);
 assert.equal(E.cloudAt(rows,at).value,900);assert.equal(E.cloudAt(rows,at+3*hour).value,10);
 const visibility=F.sample([{ms:at,x:{visibility:24000}},{ms:at+3*hour,x:{visibility:100}}],at+hour);
 assert(Math.abs(visibility.visibility-16033.333333)<.0001);
});
test('no ceiling is categorical; transitions and gaps must not manufacture numeric heights',()=>{
 assert.equal(E.cloudAt([{ms:at,value:10},{ms:at+3*hour,clear:true}],at+hour),null);
 assert.equal(E.cloudAt([{ms:at,value:10},{ms:at+3*hour,value:null}],at+hour),null);
 const csv=fs.readFileSync(__dirname+'/fixtures/route-20261008/cloud1.txt','utf8');
 const corrupted=csv.replace('84.47097,74.61938','50,74.61938');
 assert.equal(E.parseCloudCsv(corrupted)[1].value,null);
 assert.equal(E.parseCloudCsv(corrupted)[1].reason,'belowTerrain');
});
test('recorded route: real GFS near-surface ceiling is retained, but disagreement is visible',()=>{
 const rows=E.parseCloudCsv(fs.readFileSync(__dirname+'/fixtures/route-20261008/cloud1.txt','utf8'));
 const c=E.cloudAt(rows,at+3*hour);assert(Math.abs(c.value-9.85159)<.001);
 const r=E.assess({visibility:[32660],wind_speed_10m:[2],wind_gusts_10m:[3],precipitation:[0],temperature_2m:[1],weather_code:[3],is_day:[1],cloud_cover_low:[100],wind_direction_10m:[90]},0,E.defaults);
 r.sources={visibility:'Open-Meteo'};
 const out=E.withCloud(r,c,E.defaults);assert.equal(out.cloud.disagreement,true);
 assert.equal(out.x.visibility,32660);assert.equal(E.weatherIndex([out],E.defaults),0);
 const taf={...r,cloud:{known:true,value:600,clear:false,source:'TAF'}};
 assert.equal(E.withCloud(taf,c,E.defaults).cloud.value,600);
 assert.equal(E.withCloud(taf,c,E.defaults).cloud.source,'TAF');
});

const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const E=require('../app/src/main/assets/engine.js'),A=require('../app/src/main/assets/atmosphere.js'),stations=require('../app/src/main/assets/stations.js');
const file=name=>fs.readFileSync(__dirname+'/fixtures/atmosphere-20261009/'+name,'utf8'),ms=Date.parse('2026-10-09T06:00:00Z');
const route=E.routePoints({lat:65.797,lon:87.968},{lat:65.9,lon:78.2});
test('verified Russian SYNOP reports: first row, cloud intervals, humidity and instrumental layer',()=>{
 const csv=file('synop.csv'),t=A.parseSynop(csv,'23472')[0],u=A.parseSynop(csv,'23453')[0],k=A.parseSynop(csv,'23465')[0];
 assert.equal(t.ms,ms);assert.deepEqual(t.base,[600,1000]);assert.equal(t.temp,4.6);assert.equal(t.dew,-3.1);assert.equal(t.visibility,20000);assert.equal(t.wind,3);assert(t.humidity>57&&t.humidity<59);
 assert.deepEqual(u.base,[1000,1500]);assert.equal(u.ceiling,1080);assert.equal(u.visibility,50000);assert.equal(k.ceiling,150);assert.equal(k.visibility,2000);assert.equal(k.fog,false);assert.equal(k.weather,10);
 assert.equal(A.parseSynop(csv,'99999').length,0);
});
test('nearest observation stations match actual route; no arbitrary distant airport substitution',()=>{
 const chosen=A.nearby(route.points,stations);assert(chosen.some(s=>s.code==='23472'&&s.points.some(p=>p.index===0&&p.distance<5)));assert(chosen.some(s=>s.code==='23453'&&s.points.some(p=>p.index===4&&p.distance<20)));assert(chosen.some(s=>s.code==='23465'&&s.points.some(p=>p.index===2&&p.distance<60)));
 assert(!chosen.some(s=>s.code==='23358'));
});
test('h9 alone is not clear sky; missing wind is not calm; fog uses SYNOP codes',()=>{
 const raw=(cover,wind,h='9',ww='45')=>`23472,2026,10,09,06,00,AAXX 09061 23472 41${h}70 ${cover}10${wind} 10046 21031 7${ww}00==`;
 assert.equal(A.parseSynop(raw('8','99'),'23472')[0].clear,false);assert.equal(A.parseSynop(raw('8','99'),'23472')[0].wind,null);
 assert.equal(A.parseSynop(raw('0','03'),'23472')[0].clear,true);assert.equal(A.parseSynop(raw('8','03'),'23472')[0].fog,true);
 assert.equal(A.parseSynop(raw('8','03','/'),'23472')[0].base,null);
});
test('observation conflict applies only close in space and time, never tomorrow',()=>{
 const obs=A.parseSynop(file('synop.csv'),'23472')[0],cloud={known:true,value:10,clear:false};
 assert(A.comparison(obs,cloud,ms,ms,1));assert.equal(A.comparison(obs,cloud,ms+24*3600000,ms,1),null);assert.equal(A.comparison(obs,cloud,ms,ms,39),null);assert.equal(A.comparison(obs,cloud,ms,ms+4*3600000,1),null);
});
test('all 30 recorded pressure responses parse with actual units and grid coordinates',()=>{
 for(let i=0;i<5;i++)for(const pressure of [975,950,925])for(const group of ['air','water']){const rows=A.parseProfile(file(`verified-point${i}-${pressure}-${group}.csv`),group,route.points[i]);assert.equal(rows.length,13);assert(Object.values(rows[0].x).every(Number.isFinite));}
});
test('icing needs subzero liquid water; RH or minus temperature alone is insufficient',()=>{
 const groups={};for(const p of [975,950,925])groups[p]={air:A.parseProfile(file(`verified-point0-${p}-air.csv`),'air',route.points[0]),water:A.parseProfile(file(`verified-point0-${p}-water.csv`),'water',route.points[0])};
 const result=A.icing(groups,ms,38);assert.equal(result.signal,false);assert(result.usable.length>0);assert(result.levels.every(l=>l.agl===l.height-38));
 const synthetic={950:{air:[{ms,x:{temp:-5,humidity:98,height:538}}],water:[{ms,x:{liquid:.0001,ice:0}}]}};
 assert.equal(A.icing(synthetic,ms,38).signal,true);synthetic[950].water[0].x.liquid=0;assert.equal(A.icing(synthetic,ms,38).signal,false);
 assert.equal(A.icing(groups,ms,null).complete,false);assert.equal(A.icing(groups,ms+48*3600000,38).complete,false);
 assert.throws(()=>A.parseProfile(file('verified-point0-950-air.csv').replace('unit="K"','unit="C"'),'air',route.points[0]));
});

(function(root){
'use strict';
const defaults={visRed:1000,visYellow:3000,windRed:20,windYellow:12,gustRed:25,gustYellow:18,rainRed:3,rainYellow:1,cloudRed:150,cloudYellow:300};
function valid(n){return typeof n==='number'&&Number.isFinite(n);}
function grade(v,red,yellow,reverse){if(!valid(v))return 3;return reverse?(v<red?2:v<yellow?1:0):(v>=red?2:v>=yellow?1:0);}
function at(h,key,i){const v=h&&h[key]&&h[key][i];return valid(v)?v:null;}
function assess(h,i,t){t=Object.assign({},defaults,t);const x={visibility:at(h,'visibility',i),wind:at(h,'wind_speed_10m',i),gust:at(h,'wind_gusts_10m',i),low:at(h,'cloud_cover_low',i),rain:at(h,'precipitation',i),temp:at(h,'temperature_2m',i),code:at(h,'weather_code',i),direction:at(h,'wind_direction_10m',i),day:at(h,'is_day',i)};
 const g={visibility:grade(x.visibility,t.visRed,t.visYellow,true),wind:grade(x.wind,t.windRed,t.windYellow),gust:grade(x.gust,t.gustRed,t.gustYellow),rain:grade(x.rain,t.rainRed,t.rainYellow),low:valid(x.low)?(x.low>=80?1:0):3};
 const reasons=[];let level=0;const missing=Object.values(g).includes(3)||x.code===null||x.day===null;
 Object.values(g).filter(v=>v!==3).forEach(v=>level=Math.max(level,v));
 if(g.visibility>0&&g.visibility<3)reasons.push('Сниженная видимость');
 if(g.wind>0&&g.wind<3)reasons.push('Сильный ветер');
 if(g.gust>0&&g.gust<3)reasons.push('Порывы ветра');
 if(g.rain>0&&g.rain<3)reasons.push('Интенсивные осадки');
 if(g.low===1)reasons.push('Значительная облачность нижнего яруса');
 if([95,96,99].includes(x.code)){level=2;reasons.push('Прогноз грозы');}
 if([56,57,66,67].includes(x.code)){level=2;reasons.push('Прогноз переохлаждённых осадков');}
 if([45,48].includes(x.code)){level=Math.max(level,1);reasons.push('Прогноз тумана');}
 if([71,73,75,77,85,86].includes(x.code)){level=Math.max(level,1);reasons.push('Прогноз снега');}
 if(missing){reasons.push('Часть погодных данных отсутствует');if(level<2)level=3;}
 return {level,x,g,reasons,missing};
}
function routePoints(a,b){const rad=Math.PI/180;const unit=p=>[Math.cos(p.lat*rad)*Math.cos(p.lon*rad),Math.cos(p.lat*rad)*Math.sin(p.lon*rad),Math.sin(p.lat*rad)];const u=unit(a),v=unit(b);const angle=Math.acos(Math.max(-1,Math.min(1,u.reduce((s,x,i)=>s+x*v[i],0))));if(angle>Math.PI-.01)throw Error('Выберите более короткий маршрут');const points=[];for(let i=0;i<5;i++){let t=i/4,xyz;if(angle<1e-7)xyz=u;else{let p=Math.sin((1-t)*angle)/Math.sin(angle),q=Math.sin(t*angle)/Math.sin(angle);xyz=u.map((x,j)=>p*x+q*v[j]);}points.push({lat:Math.atan2(xyz[2],Math.hypot(xyz[0],xyz[1]))/rad,lon:Math.atan2(xyz[1],xyz[0])/rad});}return {points,distance:angle*6371};}
function combine(results){const severe=results.some(r=>r.level===2),missing=results.some(r=>r.missing);return {level:severe?2:missing?3:Math.max(...results.map(r=>r.level)),missing};}
function weatherName(c){if(c===null)return 'Нет данных';if(c===0)return 'Ясно';if(c<=3)return 'Облачно';if([45,48].includes(c))return 'Туман';if([56,57,66,67].includes(c))return 'Переохлаждённые осадки';if(c>=95)return 'Гроза';if([71,73,75,77,85,86].includes(c))return 'Снег';if(c>=51&&c<=82)return 'Дождь / морось';return 'Осадки';}

function sampleModel(data,ms,t){
 const h=data?.hourly||{},times=h.time||[],stamp=times.map(s=>Date.parse(s+'Z'));
 const lower=stamp.findIndex(x=>x===ms);if(lower>=0)return assess(h,lower,t);
 let hi=stamp.findIndex(x=>x>ms);if(hi<1)return assess({},-1,t);
 const a=hi-1,b=hi,out={};
 for(const key of ['visibility','wind_speed_10m','wind_gusts_10m','cloud_cover_low','precipitation','temperature_2m','weather_code','is_day','wind_direction_10m']){
  const va=at(h,key,a),vb=at(h,key,b);let value=null;
  if(va!==null&&vb!==null){
   if(['visibility','is_day'].includes(key))value=Math.min(va,vb);
   else if(key==='weather_code'){
    const hazard=c=>[95,96,99,56,57,66,67].includes(c)?2:[45,48,71,73,75,77,85,86].includes(c)?1:0;
    value=hazard(va)>=hazard(vb)?va:vb;
   } else if(key==='temperature_2m')value=va+(vb-va)*(ms-stamp[a])/(stamp[b]-stamp[a]);
   else if(key==='wind_direction_10m'){const diff=((vb-va+540)%360)-180;value=(va+diff*(ms-stamp[a])/(stamp[b]-stamp[a])+360)%360;}
   else value=Math.max(va,vb);
  }out[key]=[value];
 }
 return assess(out,0,t);
}
function severity(v,red,yellow,reverse=false){
 if(!valid(v))return null;
 if(reverse){if(v<=red)return 1;if(v<yellow)return .4+.6*(yellow-v)/(yellow-red);return Math.max(0,.4*(2*yellow-v)/yellow);}
 if(v>=red)return 1;if(v>yellow)return .4+.6*(v-yellow)/(red-yellow);return Math.max(0,.4*v/yellow);
}
function mergeForecast(base,update){const out={...base};for(const[k,v]of Object.entries(update))if(v!==null&&v!==undefined&&v!==''&&(k!=='clouds'||Array.isArray(v)&&v.length))out[k]=v;return out;}
function tafAt(taf,ms){
 const sec=ms/1000;if(!taf||sec<taf.validTimeFrom||sec>=taf.validTimeTo)return null;
 const fcsts=Array.isArray(taf.fcsts)?taf.fcsts:[],normal=fcsts.filter(g=>!g.fcstChange||['FM','BECMG'].includes(g.fcstChange)).sort((a,b)=>a.timeFrom-b.timeFrom);
 let previous={},conditions=null,variants=[],transition=false;
 for(const g of normal){if(g.timeFrom>sec)break;const next=mergeForecast(previous,g);
  if(g.fcstChange==='BECMG'&&valid(g.timeBec)&&sec<g.timeBec){variants=[previous,next];transition=true;conditions=next;break;}
  previous=next;conditions=next;
 }
 if(!conditions)return null;if(!variants.length)variants=[conditions];
 const tempo=fcsts.filter(g=>g.fcstChange&&!['FM','BECMG'].includes(g.fcstChange)&&sec>=g.timeFrom&&sec<g.timeTo);
 const originals=variants.slice();tempo.forEach(g=>originals.forEach(b=>variants.push(mergeForecast(b,g))));
 return {variants,transition,temporary:tempo.length>0,raw:taf.rawTAF,icao:taf.icaoId};
}
function ceilingOf(group){
 if(valid(group.vertVis))return {known:true,value:group.vertVis*.3048,clear:false};
 const clouds=group.clouds||[],significant=clouds.filter(c=>['BKN','OVC','VV'].includes(c.cover)&&valid(c.base));
 if(significant.length)return {known:true,value:Math.min(...significant.map(c=>c.base))*.3048,clear:false};
 const clear=clouds.length>0&&clouds.every(c=>['FEW','SCT','SKC','CLR','NSC','CAVOK'].includes(c.cover));
 return {known:clear,value:null,clear};
}
function augment(result,taf,ms,t,aircraft){
 const r={...result,x:{...result.x},g:{...result.g},reasons:result.reasons.slice(),cloud:null,aviation:null,temperatureOutside:false};
 const forecast=tafAt(taf,ms);
 if(forecast){
  r.aviation=forecast;
  let clouds=forecast.variants.map(ceilingOf);if(clouds.every(c=>c.known)){const values=clouds.map(c=>c.value).filter(valid);r.cloud={known:true,value:values.length?Math.min(...values):null,clear:!values.length,source:'TAF'};}
  const toMeters=v=>{const n=typeof v==='number'?v:parseFloat(v);return Number.isFinite(n)?n*1609.344:null;};
  const vis=forecast.variants.map(g=>toMeters(g.visib)).filter(valid),winds=forecast.variants.map(g=>valid(g.wspd)?g.wspd*.514444:null).filter(valid),gusts=forecast.variants.map(g=>valid(g.wgst)?g.wgst*.514444:null).filter(valid);
  if(vis.length)r.x.visibility=result.x.visibility===null?Math.min(...vis):Math.min(result.x.visibility,...vis);
  if(winds.length)r.x.wind=result.x.wind===null?Math.max(...winds):Math.max(result.x.wind,...winds);
  if(gusts.length)r.x.gust=result.x.gust===null?Math.max(...gusts):Math.max(result.x.gust,...gusts);
  r.g.visibility=grade(r.x.visibility,t.visRed,t.visYellow,true);r.g.wind=grade(r.x.wind,t.windRed,t.windYellow);r.g.gust=grade(r.x.gust,t.gustRed,t.gustYellow);
  if(r.cloud){r.g.cloud=r.cloud.clear?0:grade(r.cloud.value,t.cloudRed,t.cloudYellow,true);if(r.g.cloud>0)r.reasons.push('Низкая облачность по TAF '+forecast.icao);}
  if(r.g.visibility>0&&r.g.visibility<3&&!r.reasons.includes('Сниженная видимость'))r.reasons.push('Сниженная видимость по TAF '+forecast.icao);
  if(r.g.wind>0&&r.g.wind<3&&!r.reasons.includes('Сильный ветер'))r.reasons.push('Сильный ветер по TAF '+forecast.icao);
  if(r.g.gust>0&&r.g.gust<3&&!r.reasons.includes('Порывы ветра'))r.reasons.push('Порывы ветра по TAF '+forecast.icao);
  for(const g of forecast.variants){const wx=g.wxString||'';if(/TS|FZRA|FZDZ/.test(wx)){r.level=2;r.reasons.push('Опасные явления по TAF '+forecast.icao);}else if(/FG|SN/.test(wx)){r.level=Math.max(r.level===3?0:r.level,1);r.reasons.push('Туман / снег по TAF '+forecast.icao);}}
  if(forecast.temporary||forecast.transition)r.reasons.push('TAF содержит временные условия или переход; учтён худший вариант');
 }
 const grades=Object.values(r.g).filter(g=>g!==3);r.missing=['visibility','wind','gust','rain','low'].some(k=>r.g[k]===3)||r.x.code===null||r.x.day===null;
 r.level=Math.max(r.level===3?0:r.level,...grades);
 if(aircraft&&valid(aircraft.tempMin)&&valid(aircraft.tempMax)&&valid(r.x.temp)&&(r.x.temp<aircraft.tempMin||r.x.temp>aircraft.tempMax)){r.level=2;r.temperatureOutside=true;r.reasons.push('Температура вне справочного диапазона выбранной модели');}
 if(r.missing&&r.level<2)r.level=3;
 return r;
}
function indexDetails(results,t){
 t={...defaults,...t};const required=[['visibility','Видимость'],['wind','Ветер'],['gust','Порывы'],['rain','Осадки'],['code','Явления погоды'],['temp','Температура'],['direction','Направление ветра'],['day','День / ночь'],['low','Облачность нижнего яруса']];
 const missing=[];required.forEach(([key,name])=>{const points=results.map((r,i)=>valid(r.x[key])?null:i).filter(i=>i!==null);if(points.length)missing.push({name,points});});
 const points=results.map((r,i)=>r.cloud?.known&&(r.cloud.clear||valid(r.cloud.value))?null:i).filter(i=>i!==null);if(points.length)missing.push({name:'Высота облаков / облачный потолок',points});
 if(!results.length)missing.push({name:'Погодный прогноз',points:[]});
 if(missing.length)return {score:null,missing,points:[]};
 const breakdown=results.map(r=>{
  const factors=[{name:'Видимость',value:r.x.visibility,unit:'м',penalty:severity(r.x.visibility,t.visRed,t.visYellow,true)},
   {name:'Ветер',value:r.x.wind,unit:'м/с',penalty:severity(r.x.wind,t.windRed,t.windYellow)},
   {name:'Порывы',value:r.x.gust,unit:'м/с',penalty:severity(r.x.gust,t.gustRed,t.gustYellow)},
   {name:'Осадки',value:r.x.rain,unit:'мм/ч',penalty:severity(r.x.rain,t.rainRed,t.rainYellow)},
   {name:'Облачный потолок',value:r.cloud.clear?null:r.cloud.value,unit:'м',clear:r.cloud.clear,source:r.cloud.source,penalty:r.cloud.clear?0:severity(r.cloud.value,t.cloudRed,t.cloudYellow,true)}];
  const adverse=[95,96,99,56,57,66,67].includes(r.x.code)||r.temperatureOutside||(r.aviation?.variants||[]).some(g=>/TS|FZRA|FZDZ/.test(g.wxString||''));
  if(adverse)factors.push({name:r.temperatureOutside?'Температура вне справочного диапазона':'Гроза / переохлаждённые осадки',penalty:1});
  // Product is a chosen weather scoring rule, not multiplication of event probabilities.
  const raw=100*factors.reduce((acc,f)=>acc*(1-f.penalty),1);return {raw,score:Math.round(raw),factors};
 });return {score:Math.round(Math.min(...breakdown.map(x=>x.raw))),missing:[],points:breakdown};
}
function weatherIndex(results,t){return indexDetails(results,t).score;}
function coverage(results){
 const fields=[['visibility','Видимость'],['wind','Ветер'],['gust','Порывы'],['direction','Направление ветра'],['low','Облачность нижнего яруса'],['rain','Осадки'],['temp','Температура'],['code','Явления погоды'],['day','День / ночь']];
 const list=fields.map(([key,name])=>({name,status:results.every(r=>valid(r.x[key]))?'есть':'нет'}));
 const cloudCount=results.filter(r=>r.cloud?.known).length;
 list.push({name:'Высота облаков / вертикальная видимость',status:cloudCount===results.length?'есть':cloudCount?'частично':'нет'},{name:'Обледенение на маршруте',status:'нет'},{name:'Турбулентность',status:'нет'});
 return {list,available:list.filter(x=>x.status==='есть').length,total:list.length,partial:cloudCount>0&&cloudCount<results.length};
}
// NCSS CSV contains geopotential heights relative to sea level (GFS UPP CLDZ).
// Subtract model terrain at the same grid cell to approximate height above ground.
function parseCloudCsv(csv){
 const lines=String(csv).trim().split(/\r?\n/),header=lines.shift()?.split(',')||[],index=key=>header.findIndex(h=>h.startsWith(key+'['));
 const ci=index('Geopotential_height_cloud_ceiling'),si=index('Geopotential_height_surface'),li=index('latitude'),oi=index('longitude');
 if(ci<0||si<0||li<0||oi<0)throw Error('Источник высоты облаков вернул другой формат.');
 const rows=[];for(const line of lines){const f=line.split(','),ms=Date.parse(f[0]),value=i=>f[i]?.trim()?Number(f[i]):NaN,c=value(ci),terrain=value(si),lat=value(li),lon=value(oi);if(!Number.isFinite(ms)||!valid(lat)||!valid(lon))continue;const height=valid(c)&&valid(terrain)&&c>=-500&&c<19000&&Math.abs(terrain)<10000?Math.max(0,c-terrain):null;rows.push({ms,value:height,lat,lon:lon>180?lon-360:lon});}
 if(!rows.length)throw Error('В источнике нет часов прогноза облаков.');return rows.sort((a,b)=>a.ms-b.ms);
}
function cloudAt(rows,ms){if(!Array.isArray(rows)||!rows.length||ms<rows[0].ms||ms>rows[rows.length-1].ms)return null;const exact=rows.find(r=>r.ms===ms);if(exact)return valid(exact.value)?{known:true,value:exact.value,clear:false,source:'GFS',approximate:true}:null;const hi=rows.findIndex(r=>r.ms>ms);if(hi<1||rows[hi].ms-rows[hi-1].ms>3*3600000||!valid(rows[hi].value)||!valid(rows[hi-1].value))return null;return {known:true,value:Math.min(rows[hi].value,rows[hi-1].value),clear:false,source:'GFS',approximate:true};}
function withCloud(r,cloud,t){if(!cloud)return r;const out={...r,g:{...r.g},reasons:r.reasons.slice()};if(r.cloud?.known){out.cloud={...cloud,value:r.cloud.clear?cloud.value:Math.min(r.cloud.value,cloud.value),source:'TAF + GFS'};}else out.cloud={...cloud};out.g.cloud=grade(out.cloud.value,t.cloudRed,t.cloudYellow,true);if(out.g.cloud>0)out.reasons.push('Низкие облака по '+out.cloud.source);out.reasons=out.reasons.map(x=>x==='Значительная облачность нижнего яруса'?'Много облаков нижнего яруса':x);out.level=Math.max(out.level===3?0:out.level,out.g.cloud);if(out.missing&&out.level<2)out.level=3;return out;}
function compass(deg){if(!valid(deg))return '—';const dirs=['С','СВ','В','ЮВ','Ю','ЮЗ','З','СЗ'];return dirs[Math.round(deg/45)%8]+' '+Math.round(deg)+'°';}
const api={defaults,valid,assess,routePoints,combine,weatherName,sampleModel,tafAt,ceilingOf,augment,weatherIndex,indexDetails,coverage,compass,severity,parseCloudCsv,cloudAt,withCloud};root.Engine=api;if(typeof module!=='undefined')module.exports=api;
})(typeof window!=='undefined'?window:globalThis);

(function(root){
'use strict';
const defaults={visRed:1000,visYellow:3000,windRed:20,windYellow:12,gustRed:25,gustYellow:18,rainRed:3,rainYellow:1};
function valid(n){return typeof n==='number'&&Number.isFinite(n);}
function grade(v,red,yellow,reverse){if(!valid(v))return 3;return reverse?(v<red?2:v<yellow?1:0):(v>=red?2:v>=yellow?1:0);}
function at(h,key,i){const v=h&&h[key]&&h[key][i];return valid(v)?v:null;}
function assess(h,i,t){t=Object.assign({},defaults,t);const x={visibility:at(h,'visibility',i),wind:at(h,'wind_speed_10m',i),gust:at(h,'wind_gusts_10m',i),low:at(h,'cloud_cover_low',i),rain:at(h,'precipitation',i),temp:at(h,'temperature_2m',i),code:at(h,'weather_code',i),day:at(h,'is_day',i)};
 const g={visibility:grade(x.visibility,t.visRed,t.visYellow,true),wind:grade(x.wind,t.windRed,t.windYellow),gust:grade(x.gust,t.gustRed,t.gustYellow),rain:grade(x.rain,t.rainRed,t.rainYellow),low:valid(x.low)?(x.low>=80?1:0):3};
 const reasons=[];let level=0;const missing=Object.values(g).includes(3)||x.code===null||x.day===null;
 Object.values(g).filter(v=>v!==3).forEach(v=>level=Math.max(level,v));
 if(g.visibility>0&&g.visibility<3)reasons.push('Сниженная видимость');
 if(g.wind>0&&g.wind<3)reasons.push('Сильный ветер');
 if(g.gust>0&&g.gust<3)reasons.push('Порывы ветра');
 if(g.rain>0&&g.rain<3)reasons.push('Интенсивные осадки');
 if(g.low===1)reasons.push('Много облаков нижнего яруса; их высота неизвестна');
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
const api={defaults,valid,assess,routePoints,combine,weatherName};root.Engine=api;if(typeof module!=='undefined')module.exports=api;
})(typeof window!=='undefined'?window:globalThis);

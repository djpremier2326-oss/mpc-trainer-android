/* Append-only bankroll ledger. Money is stored in kopecks; odds have six decimals. */
(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.BettingCore=api})(typeof globalThis!=='undefined'?globalThis:this,function(){
'use strict';
const MAX=1000000000000,SCALE=1000000;
function fail(message){throw Error(message)}
function money(value,zero=true){if(!Number.isSafeInteger(value)||value<0||value>MAX||!zero&&value===0)fail('Укажи корректную сумму.');return value}
function date(value){if(typeof value!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(value)||!Number.isFinite(Date.parse(value+'T00:00:00Z'))||new Date(value+'T00:00:00Z').toISOString().slice(0,10)!==value)fail('Некорректная дата.');return value}
function time(value){if(typeof value!=='string'||!Number.isFinite(Date.parse(value)))fail('Некорректное время операции.');return value}
function parseAmount(value){const s=String(value).replace(/\s/g,'').replace(',','.');if(!/^\d+(\.\d{1,2})?$/.test(s))fail('Введи сумму в рублях, не больше двух знаков после запятой.');const parts=s.split('.'),cents=BigInt(parts[0])*100n+BigInt((parts[1]||'').padEnd(2,'0'));if(cents>BigInt(MAX))fail('Слишком большая сумма.');return Number(cents)}
function parseOdds(value){const s=String(value).trim().replace(',','.');if(!/^\d+(\.\d{1,6})?$/.test(s))fail('Введи фактический коэффициент, например 1,85.');const parts=s.split('.'),units=BigInt(parts[0])*1000000n+BigInt((parts[1]||'').padEnd(6,'0'));if(units<1000000n||units>10000000000n)fail('Коэффициент должен быть от 1 до 10000.');return Number(units)}
function stake(bank){return bank<=0?0:Math.round(bank/1000)*100}
function profit(amount,status,odds){if(status==='skip'||status==='return')return 0;if(status==='loss')return -amount;const result=(BigInt(amount)*BigInt(odds-SCALE)+500000n)/1000000n;if(result>BigInt(MAX))fail('Результат превышает допустимый размер банка.');return Number(result)}
function read(book){
 if(!book||book.version!==1||!Array.isArray(book.entries)||book.entries.length>50000)fail('Не удалось прочитать историю ставок.');money(book.initial);time(book.created);
 let bank=book.initial;const days=[],operations=[];
 for(const entry of book.entries){
  if(!entry||typeof entry!=='object')fail('Некорректная операция.');time(entry.at);let day=days[days.length-1];
  if(entry.type==='open'){
   date(entry.date);if(day&&(!day.closed||entry.date<=day.date))fail('Сначала заверши предыдущий день.');if(stake(bank)<100)fail('Для расчёта ставки в 1 ₽ нужен банк не меньше 5 ₽.');
   day={date:entry.date,opening:bank,stake:stake(bank),night:null,day:null,closed:false,profit:0};days.push(day);
  }else if(entry.type==='result'){
   if(!day||day.closed||!['night','day'].includes(entry.slot)||day[entry.slot]||entry.slot==='day'&&!day.night)fail('Результат этой ставки уже зафиксирован или нарушен порядок ставок.');
   if(!['win','loss','skip','return'].includes(entry.status))fail('Выбери результат ставки.');
   if(entry.odds!==null&&(!Number.isSafeInteger(entry.odds)||entry.odds<SCALE||entry.odds>10000000000))fail('Некорректный коэффициент.');
   if(entry.status==='win'&&entry.odds===null||entry.status==='return'&&entry.odds!==SCALE||entry.status==='skip'&&entry.odds!==null)fail('Проверь коэффициент и результат.');
   const delta=profit(day.stake,entry.status,entry.odds);bank+=delta;day.profit+=delta;day[entry.slot]={status:entry.status,odds:entry.odds,stake:day.stake,profit:delta,bank,at:entry.at};
   if(day.night&&day.day){day.closed=true;day.closing=bank;}
  }else if(['deposit','withdraw','correction'].includes(entry.type)){
   money(entry.amount,entry.type==='correction');const before=bank;if(entry.type==='deposit')bank+=entry.amount;else if(entry.type==='withdraw'){if(entry.amount>bank)fail('Сумма снятия больше текущего банка.');bank-=entry.amount;}else bank=entry.amount;
   operations.push({...entry,before,bank,change:bank-before});
  }else fail('Неизвестная операция.');
  if(!Number.isSafeInteger(bank)||Math.abs(bank)>MAX)fail('Банк выходит за допустимый диапазон.');
 }
 const current=days[days.length-1]||null;return {bank,days,operations,current,nextStake:current?.closed?stake(bank):null,profit:days.reduce((sum,day)=>sum+day.profit,0)};
}
function append(book,entry){const next={...book,entries:[...book.entries,entry]};read(next);return next}
function create(initial,day,at=new Date().toISOString()){money(initial);const book={version:1,initial,created:time(at),entries:[]};return open(book,day,at)}
function open(book,day,at=new Date().toISOString()){read(book);return append(book,{type:'open',date:date(day),at:time(at)})}
function settle(book,slot,status,odds,at=new Date().toISOString()){read(book);if(status==='skip')odds=null;if(status==='return')odds=SCALE;return append(book,{type:'result',slot,status,odds:odds??null,at:time(at)})}
function operation(book,type,amount,at=new Date().toISOString()){read(book);return append(book,{type,amount,at:time(at)})}
function valid(book){try{return read(book).days.length>0}catch{return false}}
return {MAX,SCALE,parseAmount,parseOdds,stake,profit,read,create,open,settle,operation,valid};
});

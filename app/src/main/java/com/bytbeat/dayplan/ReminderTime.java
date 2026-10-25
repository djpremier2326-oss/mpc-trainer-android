package com.bytbeat.dayplan;
import java.time.*;
import java.util.Set;
public final class ReminderTime {
    public static final class Occurrence {
        public final long eventMillis,triggerMillis;
        Occurrence(ZonedDateTime event,int lead){eventMillis=event.toInstant().toEpochMilli();triggerMillis=event.minusMinutes(lead).toInstant().toEpochMilli();}
    }
    public static Occurrence next(LocalDate start,LocalTime time,String repeat,Set<Integer> weekdays,int lead,ZoneId zone,long after){
        LocalDate today=Instant.ofEpochMilli(after).atZone(zone).toLocalDate();
        if("none".equals(repeat)){Occurrence o=new Occurrence(start.atTime(time).atZone(zone),lead);return o.triggerMillis>after?o:null;}
        if("daily".equals(repeat)||"weekly".equals(repeat)){
            LocalDate d=today.isAfter(start)?today:start;
            for(int i=0;i<16;i++,d=d.plusDays(1)){
                if("weekly".equals(repeat)&&!weekdays.contains(d.getDayOfWeek().getValue()))continue;
                Occurrence o=new Occurrence(d.atTime(time).atZone(zone),lead);if(o.triggerMillis>after)return o;
            }
        }else if("monthly".equals(repeat)){
            YearMonth ym=YearMonth.from(today.isAfter(start)?today:start);
            for(int i=0;i<25;i++,ym=ym.plusMonths(1)){
                LocalDate d=ym.atDay(Math.min(start.getDayOfMonth(),ym.lengthOfMonth()));if(d.isBefore(start))continue;
                Occurrence o=new Occurrence(d.atTime(time).atZone(zone),lead);if(o.triggerMillis>after)return o;
            }
        }else if("yearly".equals(repeat)){
            int year=Math.max(start.getYear(),today.getYear());
            for(int i=0;i<9;i++,year++){
                YearMonth ym=YearMonth.of(year,start.getMonthValue());LocalDate d=ym.atDay(Math.min(start.getDayOfMonth(),ym.lengthOfMonth()));if(d.isBefore(start))continue;
                Occurrence o=new Occurrence(d.atTime(time).atZone(zone),lead);if(o.triggerMillis>after)return o;
            }
        }
        return null;
    }
}

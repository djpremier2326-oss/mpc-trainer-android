package com.bytbeat.dayplan;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.*;
import java.util.*;
public class ReminderTimeTest {
 static final ZoneId Z=ZoneId.of("Europe/Moscow");
 long at(String d){return ZonedDateTime.parse(d).toInstant().toEpochMilli();}
 ReminderTime.Occurrence next(String date,String time,String repeat,int lead,long now,Integer...days){return ReminderTime.next(LocalDate.parse(date),LocalTime.parse(time),repeat,new HashSet<>(Arrays.asList(days)),lead,Z,now);}
 @Test public void exactSingleAndPast(){long now=at("2026-10-25T14:00:00+03:00");ReminderTime.Occurrence o=next("2026-10-25","15:00","none",10,now);assertEquals(at("2026-10-25T14:50:00+03:00"),o.triggerMillis);assertNull(next("2026-10-25","13:00","none",0,now));}
 @Test public void warningCrossesMidnight(){ReminderTime.Occurrence o=next("2026-10-26","00:10","none",30,at("2026-10-25T23:00:00+03:00"));assertEquals(at("2026-10-25T23:40:00+03:00"),o.triggerMillis);}
 @Test public void weeklySelectedDays(){ReminderTime.Occurrence o=next("2026-10-25","15:00","weekly",0,at("2026-10-25T16:00:00+03:00"),2,4);assertEquals(at("2026-10-27T15:00:00+03:00"),o.eventMillis);}
 @Test public void monthlyClampsAndReturnsTo31(){ReminderTime.Occurrence o=next("2026-01-31","09:00","monthly",0,at("2026-02-01T00:00:00+03:00"));assertEquals(at("2026-02-28T09:00:00+03:00"),o.eventMillis);o=next("2026-01-31","09:00","monthly",0,o.triggerMillis+1);assertEquals(at("2026-03-31T09:00:00+03:00"),o.eventMillis);}
 @Test public void birthdayLeapYear(){ReminderTime.Occurrence o=next("2024-02-29","09:00","yearly",0,at("2026-01-01T00:00:00+03:00"));assertEquals(at("2026-02-28T09:00:00+03:00"),o.eventMillis);o=next("2024-02-29","09:00","yearly",0,at("2028-01-01T00:00:00+03:00"));assertEquals(at("2028-02-29T09:00:00+03:00"),o.eventMillis);}
 @Test public void dailyLeadDoesNotSkipNextDay(){ReminderTime.Occurrence o=next("2026-10-25","10:00","daily",1440,at("2026-10-25T09:59:59+03:00"));assertEquals(at("2026-10-25T10:00:00+03:00"),o.triggerMillis);ReminderTime.Occurrence n=next("2026-10-25","10:00","daily",1440,o.triggerMillis+1000);assertEquals(at("2026-10-26T10:00:00+03:00"),n.triggerMillis);}
 @Test public void startDateNotBeforeAnchor(){ReminderTime.Occurrence o=next("2027-01-15","15:00","weekly",0,at("2026-10-25T09:00:00+03:00"),1);assertEquals(at("2027-01-18T15:00:00+03:00"),o.eventMillis);}
 @Test public void timezoneAndDst(){ZoneId berlin=ZoneId.of("Europe/Berlin");ReminderTime.Occurrence o=ReminderTime.next(LocalDate.parse("2026-03-29"),LocalTime.parse("02:30"),"none",Collections.emptySet(),0,berlin,Instant.parse("2026-03-28T00:00:00Z").toEpochMilli());assertEquals(Instant.parse("2026-03-29T01:30:00Z").toEpochMilli(),o.triggerMillis);}
 @Test public void restoreAfterTimeChangeKeepsNextFuture(){ReminderTime.Occurrence o=next("2026-10-01","15:00","daily",10,at("2026-10-25T16:00:00+03:00"));assertEquals(at("2026-10-26T14:50:00+03:00"),o.triggerMillis);}
}

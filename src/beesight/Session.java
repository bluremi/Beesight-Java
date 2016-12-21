package beesight;

import java.util.Date;
import java.util.Calendar;

/**
 * 
 * @author Phil Brosgol <phil.brosgol at gmail.com>
 */
public class Session {
    long timestamp; //UNIX timestamp
    int minutes;
    String comment;
    
    public void setTimestamp(long ts){
        timestamp = ts;
    }
    public void setMinutes(int mins){
        minutes = mins;
    }
    public void setComment(String cmt){
        comment = cmt;
    }
    
    public long getTimestamp(){
        return timestamp;
    }
    public int getMinutes(){
        return minutes;
    }
    public String getComment(){
        return comment;
    }
    public Date getDate(){
        return new Date(timestamp*1000);
    }
    
    public String getTime(){
    // returns the session time in 24HR format (with seconds) e.g. 14:55:30
        String time = "";
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp*1000);
        String hour  = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
        String minute = String.format("%02d", cal.get(Calendar.MINUTE));
        String second = String.format("%02d", cal.get(Calendar.SECOND));
        time = hour + ":" + minute + ":" + second;
        return time;
    }
}

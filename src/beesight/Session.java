package beesight;

import java.util.Date;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * 
 * @author Phil Brosgol <phil.brosgol at gmail.com>
 */
public class Session {
    private String time;
    private String duration;
    private long id;
    private String preset;
    
    private Session(){
    }
    
    public long getId(){
        return id;
    }
    
    public String getComment(){
        
        return "\"" + time + ", " + duration + ", " + preset + "\"";
    }
    
    public double getMinutes(){
    // returns the number of minutes, based on the "duration" which has sample format 25:04
    // if it's over an hour long it has format 1:25:04, etc
        double minutes = 0;
        String[] nums = duration.split(":");
        if (nums.length > 2) {
            //it's over an hour long
            minutes += Integer.parseInt(nums[0]) * 60;
            minutes += Integer.parseInt(nums[1]);
            minutes += Double.parseDouble(nums[2]) / 60;
        } else {
            minutes += Integer.parseInt(nums[0]);
            minutes += Double.parseDouble(nums[1]) / 60;
        }
    
        return Math.floor(minutes * 100) / 100;
    }
        
    public long getTimestamp(){
    // returns a UNIX timestamp based on the time string, sample format: Jan 01 2017 11:03 PM
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat(
                "MMM dd yyyy hh:mm a");
        try {
            cal.setTime(format.parse(time));
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return (cal.getTimeInMillis() / 1000);
    }
    
    public String getTime(){
    // returns the session time in 24HR format (with seconds) e.g. 14:55:30
        String time = "";
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getTimestamp()*1000);
        String hour  = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
        String minute = String.format("%02d", cal.get(Calendar.MINUTE));
        String second = String.format("%02d", cal.get(Calendar.SECOND));
        time = hour + ":" + minute + ":" + second;
        return time;
    }
}

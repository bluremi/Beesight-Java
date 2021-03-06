
package beesight;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;


/**
 * Version 2.0 (2 January 2017)
 * @author Phil Brosgol <phil.brosgol at gmail.com>
 */
public class Beesight {
    private static Logger logger = Logger.getLogger("beesight.Beesight");
        
    String insightUsername;
    String insightPassword;
    String bmUsername;
    String authToken;
    String goalName;
    String logLevel;
    int numSessions;
    ArrayList<Session> sessionList;
    
    File sessionDataFile = new File("sessionData.csv");
    String INSIGHT_LOGIN_URL = "https://insighttimer.com/user_session";
    String INSIGHT_SESSIONS_URL = "https://insighttimer.com/sessions/all?p1=1&l1=";
    String BM_BASE_URL = "https://www.beeminder.com/api/v1/";
    
    public static void main(String[] args) {
        Beesight bs = new Beesight();
        //logging
        FileHandler fh;
        try {
            fh = new FileHandler("beesight.log", true);
            logger.addHandler(fh);
            MyFormatter formatter = new MyFormatter();
            fh.setFormatter(formatter);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        logger.info("Reading configuration file");
        bs.readConfig();
        bs.setLogLevel();
        bs.go();
    }
    
    public void go(){
        boolean sessionDataExists = sessionDataFile.exists();
        
        //get session data from insighttimer.com and add it to the sessionList<>
        logger.info("Getting insighttimer session data");
        try {
            getInsightSessionData();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not parse insight session data", ex);
            System.exit(0);
        }
        logger.log(Level.INFO, "Parsed " + sessionList.size() + " sessions");
        
        //now remove any sessions from the sessionList that are older than the first (latest)
        //session in the sessionDataFile
        if (sessionDataExists) {
            removeOldSessions();
        } else {
            //create a blank file
            try {
            sessionDataFile.createNewFile();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error creating new sessionData.csv file", ex);
                System.exit(0);
            }
        }
        if (sessionList.isEmpty()) {
            logger.info("No new sessions found, terminating");
            System.exit(0);
        } else {
            logger.info("Of the data parsed, " + sessionList.size() + " were new sessions");
        }
        /*  Desired behavior is to not post any data to beeminder on the first run, to avoid
            duplicating all the data that the user has been (presumably) already uploaded
            manually.
            If no sessionData file exists, that means this is the first run. Only sessions discovered after the first run are posted.
            Users desiring to post all data just need to create a file with just "0" (zero)
            on the first line.
        */
        int responseCode = 0;
        if (sessionDataExists) {
            try {
                responseCode = postSessionData();
                //make sure posting was successful
                if (responseCode != 200) {
                    logger.log(Level.SEVERE, "Received error from Beeminder server, code " + responseCode);
                    System.exit(0);
                } else {
                    logger.log(Level.INFO, "Posted session data to beeminder");
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to post session data to beeminder", ex);
            }
        }
        //put the new sessions into the sessionData file
        try {
            logger.log(Level.INFO, "Updating " + sessionDataFile.getName() + " file with newest sessions");
            updateSessionDataFile();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to update sessionData file: ", ex);
        }
    }
    
    public void readConfig(){
    // initialize the variables by reading from the config.properties file
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("config.properties");
            prop.load(input);
            
            insightUsername = prop.getProperty("Insight_Username");
            insightPassword = prop.getProperty("Insight_Password");
            bmUsername = prop.getProperty("Beeminder_Username");
            authToken = prop.getProperty("Beeminder_Auth_Token");
            goalName = prop.getProperty("Beeminder_Goal_Name");
            numSessions = Integer.parseInt(prop.getProperty("Number_of_Sessions_to_Sync"));
            logLevel = prop.getProperty("Log_Level");
            
            input.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error reading config file", ex);
            //System.exit(0);
        }
    }
    
    public UrlEncodedFormEntity getInsightLoginEntity() {
        UrlEncodedFormEntity entity = null;
        List nameValuePairs = new ArrayList(1);
        nameValuePairs.add(new BasicNameValuePair("user_session[email]", insightUsername));
        nameValuePairs.add(new BasicNameValuePair("user_session[password]", insightPassword));
        try {
            entity = new UrlEncodedFormEntity(nameValuePairs);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return entity;
    }
    
    public void getInsightSessionData() throws UnsupportedEncodingException, IOException {
    //returns an ArrayList with the latest sessions from InsightTimer.com
        sessionList = new ArrayList<>();
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(INSIGHT_LOGIN_URL);
        //POST the credentials as a set of name/value pairs
        post.setEntity(getInsightLoginEntity());
        HttpResponse response = client.execute(post);
        int code = response.getStatusLine().getStatusCode();
        //make sure we logged in without any errors
        if (code != 302) {
            logger.log(Level.SEVERE, "Error logging into InsightTimer.com, please check your credentials");
            System.exit(0);
        }
        EntityUtils.consume(response.getEntity());
        
        //Now that we have authenticated, we can request the session data
        HttpGet request = new HttpGet(INSIGHT_SESSIONS_URL + Integer.toString(numSessions));
        response = client.execute(request);
        InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
        Gson gson = new Gson();
        sessionList = gson.fromJson(reader, Response.class).getSessions();
        reader.close();
        EntityUtils.consume(response.getEntity());
    }
    
    public void removeOldSessions(){
    // removes any duplicate sessions from sessionList by comparing IDs in the sessionData file
        ArrayList<Long> idList = new ArrayList<>();
        boolean needsRemoval = false;
        // parse the sessionDataFile for IDs and add them to the list
        try {
            BufferedReader reader = new BufferedReader(new FileReader(sessionDataFile));
            String line = "";
            int index = 1;
            while (((line = reader.readLine()) != null) && index <= numSessions) {
                idList.add(Long.parseLong(line.split(",")[0]));
                index++;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error reading sessionData file", ex);
            System.exit(0);
        }
        // perform ID comparison and remove if a match is found
        
        
        
    
    
    
        ArrayList<Long> timeStamps = new ArrayList<>();
        long latestSessionTS = 0;
        long diff;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(sessionDataFile));
            String line = "";
            int index = 1;
            //get UNIX timestamp of the last x sessions in the sessionData file, where x = numSessions
            while (((line = reader.readLine()) != null) && index <= numSessions) {
                timeStamps.add(Long.parseLong(line.split(",")[0]));
                index++;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error reading sessionData file", ex);
            System.exit(0);
        }
        //perform comparison and remove if necessary
        //go backwards through the arrayList so taht removing an item doesn't wreck the indexing
        for (int i = sessionList.size() -1; i >= 0; i--) {
            for (long id : idList) {
                if (id == sessionList.get(i).getId()) {
                    needsRemoval = true;
                }
            }
            if (needsRemoval) {
                logger.log(Level.FINE, "Excluding this session, already recorded: " + sessionList.get(i).getComment());
                sessionList.remove(i);
                needsRemoval = false;
            }
        }
    }
    
    public String createSessionUrl() {
    // returns the POST url containing all of the session data to be posted to beeminder    
        String url = BM_BASE_URL;
        
        url += "users/" + bmUsername + "/goals/" + goalName + "/";
        url += "datapoints/create_all.json";
        logger.log(Level.FINE, "Generated session URL: " + url);
        return url;
    }
    
    public UrlEncodedFormEntity getDatapoints() {
    //returns the parameters for the postSessionData POST query to beeminder    
        List<BasicNameValuePair> urlParams = new ArrayList<BasicNameValuePair>();
        UrlEncodedFormEntity entity = null;
        //generate the JSON containing all the data points
        String dataPointsJSON = "[";
        int index = 0;
        for (Session s : sessionList) {
            //add a comma before every datapoint except the first
            if (index > 0) {
                dataPointsJSON += ",";
            }
            index++;
            dataPointsJSON += "{\"timestamp\":" + s.getTimestamp() + ",";
            dataPointsJSON += "\"value\":" + s.getMinutes() + ",";
            dataPointsJSON += "\"comment\":\"Beesight script entry " + s.getTime() + "\"}";
        }
        dataPointsJSON += "]";
        //create the name/value pairs
        urlParams.add(new BasicNameValuePair("datapoints", dataPointsJSON));
        urlParams.add(new BasicNameValuePair("auth_token", authToken));
        try {
            entity = new UrlEncodedFormEntity(urlParams);
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, "Error during URLEcode of session data", ex);
            System.exit(0);
        }
        return entity;
    }
    
    public int postSessionData() throws UnsupportedEncodingException, IOException {
    //posts all of the sessions in sessionList to beeminder.com using the REST API
    //returns HTTP response code
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(createSessionUrl());
        //POST the datapoints as a set of name/value pairs
        post.setEntity(getDatapoints());
        HttpResponse response = client.execute(post);
        int code = response.getStatusLine().getStatusCode();
        EntityUtils.consume(response.getEntity());
        return code;
    }
    
    public void updateSessionDataFile() throws IOException {
    //adds the latest sessions to the TOP of the file (prepends)
        String oldText = "";
        String line;
        long id;
        double minutes;
        String comment;
        BufferedReader reader = new BufferedReader(new FileReader(sessionDataFile));
        //copy the entire file contents to memory
        while ((line = reader.readLine()) != null) {
            oldText += line + "\n";
        }
        reader.close();
        //prepend the new sessions to the file contents
        for (int i = (sessionList.size() -1); i >= 0; i--) {
            id = sessionList.get(i).getId();
            minutes = sessionList.get(i).getMinutes();
            comment = sessionList.get(i).getComment();
            line = id + "," + minutes + ",\"" + comment + "\"\n";
            oldText = line + oldText;
        }
        //write back to file
        BufferedWriter writer = new BufferedWriter(new FileWriter(sessionDataFile));
        writer.write(oldText);
        writer.close();
        logger.log(Level.INFO, "Finished updating sessionData file. Process completed successfully.");
    }
    
    public void setLogLevel(){
        try {
            switch (logLevel) {
                case "INFO":    logger.setLevel(Level.INFO);
                                break;
                case "FINE":    logger.setLevel(Level.FINE);
                                break;
                case "SEVERE":   logger.setLevel(Level.SEVERE);
                                break;
                default:        logger.setLevel(Level.INFO);
                                break;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error setting log level, check config file. Valid values are FINE, INFO, SEVERE", ex);
            System.exit(0);
        }
    }
    
    public static class MyFormatter extends SimpleFormatter{
        boolean firstLine = true;
        @Override
        public String format(LogRecord record) {
            LocalDateTime now = LocalDateTime.now();
            StringBuilder sb = new StringBuilder();
            //print a blank line if it's the first time the formatter is called
            if (firstLine) {
                sb.append(System.lineSeparator());
                firstLine = false;
            }
            sb.append(now + " " + record.getLevel() + ": " + record.getMessage() + "\r\n");
            if (null != record.getThrown()) {
                Throwable t = record.getThrown();
                PrintWriter pw = null;
                try {
                    StringWriter sw = new StringWriter();
                    pw = new PrintWriter(sw);
                    t.printStackTrace(pw);
                    sb.append(sw.toString());
                } finally {
                    if (pw != null) {
                        try {
                            pw.close();
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
            }
            return sb.toString();
        }
    }
} //close Beesight class


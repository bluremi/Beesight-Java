=====================================
Beesight (Java) Version 2.0
=====================================

OVERVIEW
This is a java script that fetches meditation session data from InsightTimer.com
and posts the individual sessions (in minutes) to Beeminder.com.
It can be run multiple times a day and will not post duplicates.


INSTALL INSTRUCTIONS
1) Make sure that Java is installed on your system and the class path
   is configured correctly.
2) Extract folder contents to your "Documents" folder. If you extract
   it to "Program Files" it may not run due to operating system 
   restrictions.
3) Open "config.properties" and add the required information, e.g.:
		Insight_Username=frank@gmail.com
		Insight_Password=password123
		Beeminder_Username=franky
		Beeminder_Auth_Token=RJK7H6bnkf3sdyfb7FMp2
		Beeminder_Goal_Name=Meditate
		Number_of_Sessions_to_Sync=3
		Log_Level=INFO
   (NOTE: Possible log levels are FINE, INFO, SEVERE in order of detail)
   After you log in to Beeminder, visit https://www.beeminder.com/api/v1/auth_token.json 
   to get your personal auth token.
4) Update run_beesight.bat with the path to the Beesight directory and save.
5) To run the script, just double-click on run_beesight.bat. Ideally you would
   set it up as a scheduled task, to run every few hours or whatever interval
   works for you.

   
USAGE INSTRUCTIONS
*** The first time you run the script, it will not post any new sessions.
	It will download data from InsightTimer.com and save it to a sesion file.
	This is to prevent it from posting a bunch of sessions that you have
	already added manually.

*** The next time it runs, it will see that sessionData.csv is present and
	it will post any new sessions that it finds. It determines that a
	session is new if the timestamp is older than the last session recorded.
	
***	To force it to re-post the latest sessions, just delete those sessions
	(and any newer ones) from the sessionData.csv file.
	
*** To upload a bunch of old sessions, create a new sessionData.csv file
	and put "0" (the number zero without quotes) on the first line.
	
CHANGE LOG
2017-01-02 Version 2.0
- Now fetches insight session data via JSON. All timestamps should now be accurate
- Now records session data with greater precision (seconds)
- Removed GMT offset setting from the configuration file (no longer needed)
- sessionData.csv file format is now different, reconciles based on sessionID rather
	than timestamp
2017-12-10 Version 1.0
- Initial version
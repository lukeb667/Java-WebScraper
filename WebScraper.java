/* Luke Becker
 * Date: 4/21/2024
 * Purpose: Scrapes sourcecode of sites for URLs and email addresses, and collects them for the user. This program allows the user to jump from site to site in the order that they're found
 * Collaborators: <StackOverflow for the cls() function, ChatGPT for some help with understanding regex strings>
 *
 * ADT Justification:
 * There were several possible ADT’s for solving the problems facing us in the final project. We chose to decide between them on two main grounds: Usability, and efficiency. 
 *   Given that this web crawler is run in java, and does not need to grab large portions of the internet, it’s more important to make the program easy to work with than it 
 *   is to make it blindingly fast. With these assumptions, we designed the program for optimal convenience first, followed by speed.
 * The most notable part is our method of holding urls we haven’t visited yet. We don’t need to access these again once we use them, so any ADT can work well. The queue was 
 *   just because there’s something satisfying about exploring links in the order that you find them. Aside from the aesthetics, a queue is certainly more efficient than a list.
 *   The other two necessary data structures were for holding the URL’s we have visited and the emails we have gathered. We aren’t doing anything other than storing these in this 
 *   project. However, the most likely extension of this project would be doing something using those data points, which means we would like to be able to rapidly search through 
 *   them. Thus, we used a list for both of them in the name of usability.
 */

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;

public class WebScraper {
    protected static Pattern containsURL   = Pattern.compile(".*https?://[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(/[^/]*)*(\\.[a-zA-Z0-9]+)?.*");  // Regular expression to check if a string CONTAINS a URL
    protected static Pattern containsEmail = Pattern.compile(".*[a-zA-Z0-9](\\.?[a-zA-Z0-9]+)*@[a-zA-Z0-9]+\\.[a-zA-Z0-9]+.*");          // Regular expression to check if a string CONTAINS an email 
    protected static Pattern isURL         = Pattern.compile("https?://[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(/[^/]*)*(\\.[a-zA-Z0-9]+)?");  // Regular expression to check if a string IS a URL
    protected static Pattern isEmail       = Pattern.compile("[a-zA-Z0-9](\\.?[a-zA-Z0-9]+)*@[a-zA-Z0-9]+\\.[a-zA-Z0-9]+");          // Regular expression to check if a string IS an emai;
    
    protected static ArrayDeque<String> urlQueue      = new ArrayDeque<String>();
    protected static ArrayList<String> visitedURLs    = new ArrayList<String>();
    protected static ArrayList<String> emailsGathered = new ArrayList<String>();
    

    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);
        if (!(getFirstURL(keyboard) == null)) { // This will be null if the user entered the exit code
            scrapeLoop(keyboard); // If the user didn't choose to exit the program, initialize the main scraping loop 
            CLIviewDataOnEXIT(keyboard); } }
    
    //
    // SCRAPING FUNCTIONS
    // >>>

    public static String getFirstURL(Scanner keyboard) {
        // Get an initial URL from the user.
        // This is a unique function to ensure that the first website entered is readable
        String URLString = getURLStringfromInput(keyboard, "Input a valid URL to begin scraping, or input EXIT to terminate the program... ", "Error: invalid URL!", "exit");
        if (!(URLString == null)) {
            cls();
            if (readWebpage(URLString))  {return URLString; } // Return the URL string if the webpage was read successfully 
            else                         {System.out.println("Error reading webpage. Try a different URL..."); return getFirstURL(keyboard);} } // Recursive call if webpage read failed
        else                             {return null;} // If the user entered the exit code, return null
    }

    public static void scrapeLoop(Scanner keyboard) {
        // This function loops and directs the rest of the program based on user input. This is called after the user inputs a first link and the program has data to operate on.
        
        String suggestedNext = urlQueue.peek(); // Peek the first link in the queue to suggest to the user
        String nextErrorString = "None"; // If the queue is empty, display this string to the user 
        if (suggestedNext == null) { suggestedNext = nextErrorString; }

        System.out.println( // Display options to the user
            "\nPlease choose from the following (input number):" +
            "\n  1. Next: " + suggestedNext + 
            "\n  2. Input URL manually" +
            "\n  3. View Data" +
            "\n  4. Exit" );

        String userInput = keyboard.next().strip().toLowerCase();

        if (suggestedNext == nextErrorString && userInput.equals("1")) {userInput = "2";} // Handle empty queue
        if      (userInput.equals("1")) { readWebpage(urlQueue.pop()); } // Read the suggested next link 
        else if (userInput.equals("2")) { CLImanualURL(keyboard); } // Manually input a URL
        else if (userInput.equals("3")) { CLIviewData(keyboard); } // Open data CLI
        else if (userInput.equals("4")) { System.out.println("Retrieving Data..."); } // Quit
        else                                     { cls(); System.out.println("Error: invalid input..."); } // If input is unhandled 

        if (!userInput.equals("4")) {scrapeLoop(keyboard); }  // Recursive call if user did not select EXIT 
        }


    public static boolean readWebpage(String URLString) {
        if (isValidURL(URLString)) { 
            String domain = URI.create(URLString).getHost(); // Gets the domain of the URL
            String path   = URI.create(URLString).getPath(); // Gets the path/subdomain of the URL 

            Scanner webpageScanner = new Scanner(getStreamFromString(URLString)); // Open the webpage for reading 
            while (webpageScanner.hasNext()) {
                
                String currentToken = webpageScanner.next(); 
                // Check for URLs
                if (currentToken.contains("href=\"")) { // If there is an html reference tag in the line
                    try                 {
                        String link = formatLink(currentToken.substring(currentToken.indexOf("href=\"")+6, currentToken.indexOf("\"", currentToken.indexOf("href=\"")+6)), domain, path); // Try to format the link. Will return null if the string cannot be formatted 
                        if (!(link == null) && !(urlQueue.contains(link)) && (!(visitedURLs.contains(link))) ) {urlQueue.add(link);} } // If the URL hasn't already been found, add it to the queue 
                    catch (StringIndexOutOfBoundsException e) {} } // If the String does not contain the expected delimiters, we don't care and just pass it by; it won't contain a link
                
                // Check for emails 
                if (containsEmail.matcher(currentToken).matches()) {
                    String emailString = getEmailFromString(currentToken);
                    if (!(emailString == null)) {emailsGathered.add(emailString);} } }

            webpageScanner.close(); visitedURLs.add(URLString); cls(); System.out.println(">>> Operation Successful..."); return true;}
        else {cls(); System.out.println(">>> Operation Unsuccessful..."); return false;} } // Returns false if the link looks good, but is unreadable for some reason. This can include connection issues 
    
    //
    // CLI FUNCTIONS 
    // >>>

    public static void CLImanualURL(Scanner keyboard) { 
        // Prompt the user to input a url manually. If the URL is an exit code, the 'getURLStringfromInput' function will return null and this function will return.
        // The 'getURLStringfromInput' function handles bad user input 
        cls();
        String URLString = getURLStringfromInput(keyboard, "Input a valid URL, or input BACK to return to the previous menu... ", "Error: invalid URL!", "back");
        if      (URLString == null)     {cls();} // Return if null
        else if (isValidURL(URLString)) {readWebpage(URLString); } // If the URL is good, attempt to read the webpage
        else                            {System.out.println("Error: URL unreadable!");} } // If the URL is valid but unreadable 

    public static void CLIviewData(Scanner keyboard) {
        // Display data options to the user. This function will prompt for input and display according to the user's selection. 
        cls();
        String userInput = "";
        while (!userInput.equals("back")) {

            // Display basic data 
            System.out.println("---- --- -- -");
            System.out.println("URLs  identified: " + urlQueue.size());
            System.out.println("Emails  gathered: " + emailsGathered.size());
            System.out.println("Websites visited: " + visitedURLs.size());
            System.out.println("---- --- -- -");

            // Display input options 
            System.out.println("Please select a dataset to view (input number), or input BACK to view the previous menu:" +
                                "\n  1. Detected URLs" + 
                                "\n  2. Detected Email Addresses" + 
                                "\n  3. Visited Websites" );

            // Get and check user input 
            userInput = keyboard.next().toLowerCase().strip();
            if (userInput.equals("1")) { 
                // Print detected URLs
                cls();
                System.out.println("\n");
                Iterator<String> iter = urlQueue.iterator();
                int i = 1;
                while (iter.hasNext()) {
                    System.out.println(i++ + ". " + iter.next()); 
                    if (i%1000 == 0) {
                        // Pause after 1000 items to ensure the console allows them to be viewed
                        System.out.println("Input any value to continue...");
                        keyboard.next();
                        System.out.println("\n"); } }
                System.out.println("Input any value to finish...");
                System.out.println(keyboard.next()); break;}
            
            else if (userInput.equals("2")) { 
                // Print emails gathered
                cls();
                System.out.println("\n");
                Iterator<String> iter = emailsGathered.iterator();
                int i = 1;
                while (iter.hasNext()) {
                    System.out.println(i++ + ". " + iter.next()); 
                    if (i%1000 == 0) {
                        // Pause after 1000 items to ensure the console allows them to be viewed
                        System.out.println("Input any value to continue...");
                        keyboard.next();
                        System.out.println("\n"); } }
                System.out.println("Input any value to finish...");
                System.out.println(keyboard.next()); break;}

            else if (userInput.equals("3")) { 
                // Print sites visited
                cls();
                System.out.println("\n");
                Iterator<String> iter = visitedURLs.iterator();
                int i = 1;
                while (iter.hasNext()) {
                    System.out.println(i++ + ". " + iter.next()); 
                    if (i%1000 == 0) {
                        // Pause after 1000 items to ensure the console allows them to be viewed
                        System.out.println("Input any value to continue...");
                        keyboard.next();
                        System.out.println("\n"); } }
                System.out.println("Input any value to finish...");
                System.out.println(keyboard.next()); break;} 
            else {cls(); System.out.println("Error: invalid input...");} } 
        cls(); }

        public static void CLIviewDataOnEXIT(Scanner keyboard) {
            // Display data options to the user on exit. This function will prompt for input and display according to the user's selection. 
            cls();
            String userInput = "";
            while (!userInput.equals("4")) {
    
                // Display basic data 
                System.out.println("Data Collected: ");
                System.out.println("URLs  identified: " + urlQueue.size());
                System.out.println("Emails  gathered: " + emailsGathered.size());
                System.out.println("Websites visited: " + visitedURLs.size());
                System.out.println("---- --- -- -");
    
                // Display input options 
                System.out.println("Please select a dataset to view (input number), or input 4 to terminate the program:" +
                                    "\n  1. Detected URLs" + 
                                    "\n  2. Detected Email Addresses" + 
                                    "\n  3. Visited Websites" +
                                    "\n  4. Exit" );
    
                // Get and check user input 
                userInput = keyboard.next().toLowerCase().strip();
                if (userInput.equals("1")) { 
                    // Print detected URLs
                    cls();
                    System.out.println("\n");
                    Iterator<String> iter = urlQueue.iterator();
                    int i = 1;
                    while (iter.hasNext()) {
                        System.out.println(i++ + ". " + iter.next()); 
                        if (i%1000 == 0) {
                            // Pause after 1000 items to ensure the console allows them to be viewed
                            System.out.println("Input any value to continue...");
                            keyboard.next();
                            System.out.println("\n"); } }
                    System.out.println("Input any value to finish...");
                    System.out.println(keyboard.next()); cls();} 
                
                else if (userInput.equals("2")) { 
                    // Print emails gathered
                    cls();
                    System.out.println("\n");
                    Iterator<String> iter = emailsGathered.iterator();
                    int i = 1;
                    while (iter.hasNext()) {
                        System.out.println(i++ + ". " + iter.next()); 
                        if (i%1000 == 0) {
                            // Pause after 1000 items to ensure the console allows them to be viewed
                            System.out.println("Input any value to continue...");
                            keyboard.next();
                            System.out.println("\n"); } }
                    System.out.println("Input any value to finish...");
                    System.out.println(keyboard.next()); cls();} 
    
                else if (userInput.equals("3")) { 
                    // Print sites visited
                    cls();
                    System.out.println("\n");
                    Iterator<String> iter = visitedURLs.iterator();
                    int i = 1;
                    while (iter.hasNext()) {
                        System.out.println(i++ + ". " + iter.next()); 
                        if (i%1000 == 0) {
                            // Pause after 1000 items to ensure the console allows them to be viewed
                            System.out.println("Input any value to continue...");
                            keyboard.next();
                            System.out.println("\n"); } }
                    System.out.println("Input any value to finish...");
                    System.out.println(keyboard.next()); cls();} 
                else if (userInput.equals("4")) {break;}
                else {cls(); System.out.println("Error: invalid input...");} } 
            cls(); }

    //
    // INPUT FUNCTIONS 
    // >>>

    private static void cls() {
        // This function clears the console. Mostly sourced from StackOverflow
        try                            { new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); }
        catch (IOException          e) {System.out.println("Console clear failed...");}
        catch (InterruptedException e) {System.out.println("Console clear interrupted...");} }

    public static InputStream getStreamFromString(String getFromThisString) {
        // Try to open a webpage from a given string. Returns the input stream if successful 
        try                             {return URI.create(getFromThisString).toURL().openStream(); } 
        catch (MalformedURLException e) {return null; }
        catch (IOException           e) {return null; } }

    public static String getURLStringfromInput(Scanner keyboard, String mssg, String errorMssg, String exitCode) {
        // Repeatedly prompts the user to input a valid URL. Messages and the exit code are defined on call, and should provide correct info relating to each other
        System.out.println(mssg);
        String userInput = keyboard.next().toLowerCase().trim();
        if      (userInput.equals(exitCode.toLowerCase()))  {return null; }
        else if (isValidURL(userInput))                     {return userInput;}
        else                                                {cls(); System.out.println(errorMssg); return getURLStringfromInput(keyboard, mssg, errorMssg, exitCode); } }

    public static boolean isValidURL(String checkThis) {
        // Check if a String is a valid URL by attempting to open a stream on the webpage
        try                 {URI.create(checkThis).toURL().openStream().close(); return true;}
        catch (Exception e) {return false;} }
    
    //
    // LINK FORMATTING 
    // >>>
    
    public static String formatLink(String formatThis, String domain, String path) {
        // Try to format a string to be a link. This is case-based. It will try to format local links and/or add on protocols if necessary
        if          (isURL.matcher(formatThis).matches()) {return formatThis; } // Return the string if it already meets the formatting requirements 
        else {

            if      (formatThis.indexOf("//") == 0)   {return "https:" + formatThis; } // If there is no protocol, add it
            else if (isLocalLink(formatThis))             {formatThis = constructLinkDomain(formatThis, domain, path); } // Try to construct a global link from the local one 
            
            if      (isURL.matcher(formatThis).matches()) {return formatThis; } // If success, return it.
            else                                          {return null;} } // If fail, return null
    }

    private static boolean isLocalLink  (String checkThisString) {
        // Check if a link string is local
        try {
            if      (checkThisString.charAt(0) == '/' && !(checkThisString.charAt(1) == '/')) { return true;  } // First char is '/' and not '//'
            else if (checkThisString.charAt(0) == '.')                                              { return true;  } // First char is '.' ; handles ./ and ../ cases
            else if (!(checkThisString.charAt(0) == '/') && !(checkThisString.contains("//")))    { return true;  } // First char is just a letter 
            else                                                                                          { return false; } } 
        catch (Exception e) {return false;} }

    private static String constructLinkDomain (String formatThis, String domain, String path) {
        // Build a global link from a local link 

        ArrayList<String> pathSteps = new ArrayList<String>(); // Hold each subpage string as an element 

        String subString = "";
        for (int i=0; i<path.length(); i++) { // Break the given link down into its local path steps 
            if (!(path.charAt(i) == '/')) {subString += path.charAt(i);}
            else {pathSteps.add(subString); subString = "";} }
            pathSteps.add(subString) ; subString = "" ;

        // These checks remove path elements based on the logic of the link structure. They also format so that the string does not begin with a control character
        while (formatThis.indexOf("../") == 0) { // Jump back for ../
            pathSteps.removeLast();
            formatThis = formatThis.substring(3); }
        while (formatThis.indexOf("./") == 0) { // Remove the ./ 
            formatThis = formatThis.substring(2); }
        if (formatThis.indexOf("/") == 0) { 
            pathSteps.removeLast();
            formatThis = formatThis.substring(1); }
        
        // Add the necessary path elements 
        Iterator<String> iter =  pathSteps.iterator();
        while (iter.hasNext()) {
            String token = iter.next();
            if (!(formatThis.contains(token))) {subString += "/" + token; } }
        
        // If it's a local link refering to a location on the webpage, just remove the location reference and return the link
        // There will most likely be repeats if this occurs, but this is handled elsewhere already  
        if (formatThis.contains("#")) {formatThis = formatThis.substring(0, formatThis.indexOf("#")-1); }
        
        // Return a global link 
        return "https://" + domain + subString + "/" + formatThis;
    }

    //
    // EMAIL FORMATTING 
    // >>>

    public static String getEmailFromString(String getFromThisString) {
        // Try to get a valid email from a string if the string contains one
        Matcher matcher = isEmail.matcher(getFromThisString); 
        if (matcher.find()) { return matcher.group();  } 
        else { return null; } }
}

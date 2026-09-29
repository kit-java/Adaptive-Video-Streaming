import java.io.*; 
import java.net.*; 
import java.util.logging.*;

public class Server {
	
	//Δημιουργία logger
	public static final Logger logger = Logger.getLogger(Server.class.getName());
	
    public static void main(String[] args) throws IOException {
        int port = 5000; 
        
		try {
            
			// Δημιουργία αρχείου καταγραφής srLog.txt.
			FileHandler fih = new FileHandler("srLog.txt", true);
             fih.setFormatter(new SimpleFormatter()); 
            logger.addHandler(fih);
        } catch (Exception e){
            e.printStackTrace();
        }

	    //Δημιουργία των βίντεο που λείπουν πριν ανοίξει ο server.
        generateMissingVideos();
        
        ServerSocket server = new ServerSocket(port); 
        Server.logger.info("Ο Server είναι ενεργός");

        while (true) { 
        	// Αναμένει client
            Socket clientSocket =server.accept(); 
            
            
            Server.logger.info("Συνδέθηκε πελάτης : " + clientSocket.getInetAddress()); 
            
          //Κάθε client τρέχει σε δικό του threat
            ClientThreat cltThreat = new ClientThreat(clientSocket); 
            new Thread(cltThreat).start(); 
        }
    }
    
    // Η μέθοδος που σαρώνει και φτιάχνει αρχεία που λείπουν μέσω της ffmpeg
    public static void generateMissingVideos() {
        
    	File folder = new File("videos");
        

        String[] availableFiles = folder.list() ; 
        //Αν δεν υπάρχει ο φάκελος τερματίζει τη μεθοδο
        if (availableFiles == null) return;

        String[] formats = {"mp4", "avi", "mkv"};
        String[] res ={"240p", "360p", "480p", "720p", "1080p"};
        String[] dim = {"426x240", "640x360", "854x480", "1280x720", "1920x1080"};
        String[] movies = {"flowers", "milky", "planet"};

        
        //Nested βρόγχος για τον έλεγχο όλων των συνδυασμών ταινίας ανάλυσης και format
        for (String movie : movies) {
            String sourceFile =null;
            int maxindex = -1;

         // Αναζήτησ του βίντεο με την υψηλότερη ποιότητα που υπάρχει ήδη για χρήση ως πηγή για να κάνουμε μόνο downgrading
            for (String file : availableFiles) {
                if (file.startsWith(movie)){
                    for (int i = 0; i < res.length; i++) {
                        if (file.contains(res[i]) && i > maxindex) {
                            maxindex = i ;
                            sourceFile = file;
                        }
                    }
                }
            }

            if (sourceFile != null) {
                for (int index =0; index <= maxindex; index++) {
                    for (String format : formats) {
                        String newFileName = movie + "-" + res[index] + "." + format;
                        File newFile= new File(folder, newFileName);
                        
                        
                        
                        
                        
                        
                        
                        
                        
                      //Αν το αρχείο δεν υπάρχει δημιουργείται μέσω του ffmpeg
                        if (!newFile.exists()) {
                            Server.logger.info("Δημιουργία: " + newFileName);
                            try {
                            	
                            	//επειδή πάγωνε στην δημιουργια αρχείων έβαλα τη παράμετρο -loglevel error για να 
                            	//τυπώσει μονο τα απαραίτητα σφάλματα και να μην γεμίζει η μνήμη
                                ProcessBuilder pb = new ProcessBuilder("ffmpeg","-loglevel", "error", "-i", "videos/" + sourceFile, "-s", dim[index], "videos/" + newFileName);
                                pb.redirectErrorStream(true) ;
                                Process p = pb.start(); 
                                //Αναμονή μέχρι να τελειώσει η μετατροπή του βιντεο
                                p.waitFor(); 
                            } catch (Exception e) { 
							    Server.logger.log(Level.SEVERE, " Σφάλμα δημιουργίας αρχειου", e); 
							}} }}}}
        
        Server.logger.info("Ολοκληρώθηκε ο έλεγχος αρχείων ");
    }}


//Κλάση που χειρίζεται την επικοινωνία με έναν πελάτη
class ClientThreat implements Runnable { 
    private Socket socket; 
    private Process ffmpegProcess; 

    public ClientThreat(Socket socket) { 
        this.socket = socket; 
    }

    @Override
    public void run() { 
        try {
            ObjectOutputStream oos =new ObjectOutputStream(socket.getOutputStream()); 
            ObjectInputStream ois =new ObjectInputStream(socket.getInputStream()) ; 

            
            
            
            
            
            
            String initRequest= (String) ois.readObject() ; 
            if(initRequest != null && initRequest.startsWith("INIT")) {
                String[] initParts = initRequest.split("#");
                double speed =Double.parseDouble(initParts[1]);
                String requestedFormat = initParts[2] ;
                
                // Υπολογισμος μέγιστης ανάλυσης με βάση τον πίνακα της εκφώνησης
                int maxRes = 0; // Έχω βάλει : 0=240p, 1=360p, 2=480p, 3=720p, 4=1080p
                if (speed >= 3.0)maxRes = 4;
                else if (speed >= 1.5) maxRes = 3;
                else if (speed >= 0.5) maxRes = 2;
                else if (speed >= 0.4) maxRes = 1;
                else maxRes = 0;
                
                // Φιλτράρισμα αρχείων οπου ο server στέλνει στον client μόνο ότι επιτρέπει η ταχύτητα του
                File folder =new File("videos");
                String[] allFiles = folder.list();
                StringBuilder filteredList = new StringBuilder() ; 
                
                if (allFiles != null) {
                    for (String file : allFiles) {
                        if (file.endsWith("." + requestedFormat)) {
                            
                        	//Έλεγχος ανάλυσης αρχείου για να ταιριάζει με την μεγιστη δυνατή αναλυση
                            int fileResLevel = -1 ;
                            if (file.contains("240p")) fileResLevel = 0;
                            else if (file.contains("360p")) fileResLevel = 1;
                            else if (file.contains("480p")) fileResLevel = 2;
                            else if (file.contains("720p"))  fileResLevel = 3;
                            else if (file.contains("1080p")) fileResLevel = 4;
                            
                            if (fileResLevel != -1 && fileResLevel <=maxRes) {
                                if (filteredList.length() > 0) filteredList.append(",");
                                filteredList.append(file) ;
                            }
                        }
                    }
                }
                
                // Αποστολή της λίστας πίσω στον Client
                if (filteredList.length() ==0) {
                    oos.writeObject("ΚΕΝΟ");
                    oos.flush();
                } else {
                    oos.writeObject(filteredList.toString());
                    oos.flush() ;
                }
            }

             //Χρονόμετρο 
            long startTime = 0; 
            while (true) { 
                String request= (String) ois.readObject(); 
                if (request == null) break; 

                String[] parts= request.split("#"); 
                String command = parts[0]; 

                if (command.equals("STREAM")) { 
                	
                	//Αν έπαιζε ήδη βίντεο σταματάει για να ξεκινήσει το νέο.
                    if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                        ffmpegProcess.destroy(); 
                        
                        // ΠΡΟΣΘΗΚΗ: Αν ο χρήστης επιλέξει άλλο βίντεο ενώ έπαιζε ήδη ένα, καταγράφουμε τον χρόνο του προηγούμενου
                        if (startTime > 0) {
                            long secondsPlayed= (System.currentTimeMillis() - startTime) / 1000;
                            Server.logger.info("Αλλαγή βίντεο. Χρόνος αναπαραγωγής προηγούμενου : " + secondsPlayed + " \".") ;
                        }
                    }
                    
                    String videoName = parts[1]; 
                    String protocol = parts[2]; 
                    
                    
                    //  Δημιουργία url για το stream
                    String streamUrl = protocol + "://127.0.0.1:1234";
                    
                    
                    //Στο σενάριο όπου και server και client είναι και οι δύο tcp
                    if (protocol.equals("tcp")) {
                        streamUrl = streamUrl + "?listen=1" ;
                    }
                    
                    /*
                      Δεν λειτουργόυσε το rtp και δεν κατάφερα να βρω τον λόγο 
                      
                      String format = protocol.equals("rtp") ? "rtp" : "mpegts";
                    
                    */
                    
                    
                    //Εκκίνηση του ffmpeg για τo streaming του βιντεο
                    ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg","-loglevel", "error", "-re", "-i" , "videos/" + videoName, "-f" , "mpegts", streamUrl
                    );
                    pb.redirectErrorStream(true);
                    ffmpegProcess = pb.start() ; 
                    
                    Server.logger.info("Εκτελείται stream για το αρχείο: " + videoName); 
                    
                    //Έναρξη χρονομέτρου
                    startTime = System.currentTimeMillis();

                } else if (command.equals("STOP") || command.equals("EXIT")) { 
                     if (ffmpegProcess != null && ffmpegProcess.isAlive()) { 
                        ffmpegProcess.destroy(); 
                        
                        
                        if (startTime > 0) {
                            long secondsPlayed = (System.currentTimeMillis() - startTime) / 1000;
                            Server.logger.info("Η μετάδοση σταμάτησε. Χρόνος αναπαραγωγής: " + secondsPlayed + " \"");
                            startTime = 0; 
                        }
                    }
                    if (command.equals("EXIT")) break;
                }
            }
        }  catch (Exception e) {
            Server.logger.log(Level.WARNING, "Ο Client αποσυνδέθηκε.", e); 
        } finally {
            
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) ffmpegProcess.destroy(); 
             try { socket.close(); } catch (IOException e) {}
        }
    }
}
import java.io.*; 
import java.net.Socket; 
import javax.swing.*; 
import java.awt.GridLayout; 
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.SpeedTestReport;
import java.util.logging.*;





// Κλάση του Speed Test
 class SpeedTest implements ISpeedTestListener  {
    
    @Override
    public void onCompletion(SpeedTestReport report) {
    	
    	//Μετατροπή των bps σε Mbps 
        Client.downloadSpeedMbps = report.getTransferRateBit().doubleValue() /1000000.0 ;
        Client.logger.info("Εγινε το τεστ. Η ταχύτητα είναι " + String.format("%.2f", Client.downloadSpeedMbps) + " Mbps") ;
        
        // Το έκανα με wrapper γιατί αλλιώς μου κράσαρε σε αυτό το σημείο
        
        SwingUtilities.invokeLater (() -> {
            Client.startClientApp();
        });
        
        
        
    }

    @Override
    public void onError(SpeedTestError error ,  String errorMessage) 
    //Εκτελείται σε περίπτωση σφάλματος
    
    {
    	//Σε περίπτωση σφαλματος θέτω την ταχύτητα σε 1 Mbps
        Client.logger.warning ("Σφάλμα στο τεστ.Εφαρμογή default ταχύτητας 1 Mbps." );
        Client.downloadSpeedMbps =  1.0 ; 
        
        
        
        
        //Client.startClientApp(); 
        SwingUtilities.invokeLater(() -> {
            Client.startClientApp(); 
        });
        
        
      
        
        
    }
    @Override
    public void onProgress(float percent, SpeedTestReport report) {
        
    }
   

}

public class Client {
	
	//Ανοίγω streams για την αποστολή αντικειμένων (input και output)
    private static ObjectOutputStream oos; 
    private static ObjectInputStream ois; 
    
    private static Socket socket; 
    //Δημιουργώ logger για την καταγραφή
	public static final Logger logger =Logger.getLogger(Client.class.getName()) ;
    
	
	
    // Η μεταβλητή είναι public για να έχει πρόσβαση η κλάση SpeedTest
    public static double downloadSpeedMbps = 0.0; 

    public static void main(String[] args) {
		
		try {
            // Ορίζω τον Logger να καταγράφει στο αρχείο "clLog.txt"
            FileHandler fih =new FileHandler("clLog.txt", true);
            fih.setFormatter( new SimpleFormatter()) ;
            logger.addHandler(fih);
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        
        logger.info("Εκκίνηση Client. Ακολουθεί ελεγχος ταχύτητας ");
		
	
        SpeedTestSocket speedTestSocket =new SpeedTestSocket();
        SpeedTest listener = new SpeedTest();
        speedTestSocket.addSpeedTestListener(listener) ;
        
        
        // Ξεκινάει η λήψη αρχείου για 5000 ms
        speedTestSocket.startFixedDownload("http://speedtest.tele2.net/10MB.zip", 5000);
    }

    // Η μέθοδος είναι public για να μπορεί να κληθει από την κλάση SpeedTest
    public static void startClientApp() {
        String[] types = {"mp4", "avi", "mkv"};
        String selectedType = (String) JOptionPane.showInputDialog(null, 
                "Η ταχύτητά είναι: " + String.format("%.2f", downloadSpeedMbps) + " Mbps\n  Επιλέξτε format:", 
                "Αρχική επιλογή", JOptionPane.QUESTION_MESSAGE, null, types, types[0]);
        
        // Αν γίνει cancel κλείνει η εφαρμογή.        
        if (selectedType ==null) System.exit(0) ; 

        try {
        	
            socket= new Socket("127.0.0.1", 5000); 
            oos = new ObjectOutputStream(socket.getOutputStream()); 
            ois = new ObjectInputStream(socket.getInputStream()) ; 

            // Αποστολή ταχύτητας και format του βίντεο που επιλέχθηκε
            oos.writeObject("INIT#" + downloadSpeedMbps + "#" + selectedType);
            
            //Λήψη λίστας βίντεο από τον server
            String videoListStr = (String) ois.readObject(); 
            
            
         // Έλεγχος αν ο server βρήκε βίντεο
            if (videoListStr.equals("ΚΕΝΟ")) {
                JOptionPane.showMessageDialog(null, "Δεν βρέθηκαν αρχεία");
                 System.exit(0);
            }
            
            // Μετατροπη διεθέσιμων βίντεο σε πίνακα
            String[] videoFiles =videoListStr.split(","); 

            JFrame frame= new JFrame("Video Client "); 
            frame.setSize(450, 200); 
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE) ; 
            frame.setLayout(new GridLayout(3, 2, 10, 10)); 

            JComboBox<String> videoSel = new JComboBox<>(videoFiles); 
            JComboBox<String> protocolSel =new JComboBox<>(new String[] {"Αυτοματο", "tcp", "udp", "rtp"}) ;
            
           // Κουμπί έναρξης και διακοπής αντίστοιχα
            JButton streamButton = new JButton("Έναρξη"); 
            JButton stopButton = new JButton("Διακοπή"); 

            //Drowdown menu για επιλογή βίντεο και πρωτοκόλλου
            frame.add(new JLabel("Επιλογή Βίντεο:")); 
            frame.add(videoSel); 
            frame.add(new JLabel("Επιλογή Πρωτοκόλλου:") );
            frame.add(protocolSel);
            frame.add(streamButton); 
            frame.add(stopButton); 
            
            // Λειτουργία όταν πατήσουμε Εναρξη
            streamButton.addActionListener (e -> { 
                try {
                    String selectedVideo = (String) videoSel.getSelectedItem();
                    String selectedProtocol = (String)protocolSel.getSelectedItem() ;

                 //Αυτόματη επιλογή βάσει ανάλυσης.
                    if (selectedProtocol.equals("Αυτόματο")) {
                        if (selectedVideo.contains("240p")) selectedProtocol = "tcp";
                        else if (selectedVideo.contains("360p") || selectedVideo.contains("480p")) selectedProtocol = "udp";
                        else selectedProtocol = "rtp"; 
                        Client.logger.info("Αυτόματη επιλογή πρωτοκόλλου: " + selectedProtocol);
                    }
                    
                    //Αποστολή εντολής στον server
                    String msg ="STREAM#" + selectedVideo + "#" + selectedProtocol ; 
                    oos.writeObject(msg); 
                    
                    //έβαλα αυτή την εντολή για αποσυμφόρηση διότι το πρόγραμμα έκανε stream μόνο αφού έκλεινα το gui παράθυρο
                    oos.flush();

                    
                    String streamUrl= selectedProtocol + "://127.0.0.1:1234";
                    
                    //Για να προλάβει ο server να ξεκινήσει την μετάδοση
                     try {
                        Thread.sleep(1000) ; 
                    } catch (InterruptedException ie) {}
                    
                    
                   //  Έναρξη του ffplay 
                    ProcessBuilder pb= new ProcessBuilder("ffplay","-loglevel", "error", "-i", streamUrl);
                    pb.start() ;

                } catch (IOException ex) { Client.logger.log(Level.SEVERE, "Σφάλμα επικοινωνίας", ex); } 
            });

             stopButton.addActionListener(e -> { 
                try { 
                	oos.writeObject("STOP") ;
                	oos.flush();
                	} catch  (IOException ex) { }
            });

            
           //Διαχείριση κλεισίματος παραθυρου
            frame.addWindowListener(new java.awt.event.WindowAdapter() { 
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) { 
                    try { 
                        oos.writeObject("EXIT"); 
                        socket.close(); 
                    } catch (Exception e){}
                }
            });

             frame.setVisible(true); 

        } catch (Exception e) {
			Client.logger.log(Level.SEVERE, "Δεν βρέθηκε διαθέσιμος Server", e);
			JOptionPane.showMessageDialog(null, " Δεν βρέθηκε διαθέσιμος Server"); 
			}
    }
}
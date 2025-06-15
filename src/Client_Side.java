import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.JCheckBox;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// Networking seemed tough - worked Well.
// Threading seemed easy - Worked after a lot.
// Midi seemed tough - Worked Well.
// Gui seemed easy - Worked after a lot.

public class Client_Side {

    public static void main(String[] args) {
        new Client_Side().setUpGUI();
    }

    List<JCheckBox> checkBoxData ;// Ye to Midi ko bhi chahiye, GUI ko bhi chahiye, Networking ko bhi chahiye
    JTextArea message ; // Gui mai + Netwrking ke time
    JTextArea feed ;
    JFrame jFrame ; // Say -Dialog box ko display hone ke liye iska parameter chahiye.

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat",
            "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
            "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga",
            "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo",
            "Open Hi Conga"};
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63} ;

    public void setUpGUI(){
        jFrame = new JFrame() ;
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        checkBoxData = new ArrayList<>() ;

        MusicalAspects musicalAspects = new MusicalAspects() ;
        NetworkingAspect netWorkingAspect = new NetworkingAspect() ;

        Box left = new Box(BoxLayout.Y_AXIS) ;
        for(String instrument : instrumentNames){
            JLabel jLabel = new JLabel(instrument) ;
            left.add(jLabel) ;
            //left.add(Box.createVerticalGlue()); -- suggested by gpt - glue to be added for each label to take all space available
            jLabel.setBorder(BorderFactory.createEmptyBorder(4, 1, 4, 1)); // padding code taken
        }
        jFrame.getContentPane().add(BorderLayout.WEST,left) ;

        Box right = Box.createVerticalBox();

        JButton start = new JButton("Start") ;
        start.addActionListener(e -> musicalAspects.playSong());

        JButton stop = new JButton("Stop") ;
        stop.addActionListener(e -> sequencer.stop());

        JButton refresh = getjButton();

        JButton send = new JButton("Send it") ;
        send.addActionListener(
                e -> netWorkingAspect.sendToNetwork()
        );
        right.add(start) ; right.add(stop) ; right.add(refresh) ; right.add(send) ;

        Color defaultPanelColor = UIManager.getColor("Panel.background");

        // Preferred Size - wagerah sab krke dekh liye . Agar space hai - to pura hi kheech raha hai
        message = new JTextArea(1,20) ;
        message.setLineWrap(true);
        message.setWrapStyleWord(true) ;
        JScrollPane messageScroller = new JScrollPane(message);
        messageScroller.setBorder(BorderFactory.createLineBorder(defaultPanelColor, 10));

        feed = new JTextArea(4,20) ;
        feed.setLineWrap(true);feed.setWrapStyleWord(true) ;feed.setEditable(false);

        feed.setBackground(Color.BLACK);feed.setForeground(new Color(0, 255, 255)); // Setting text color to white for contrast

        feed.setMargin(new Insets(10, 10, 10, 10)); // Adding padding inside JTextArea
        JScrollPane feedScroller = new JScrollPane(feed);
        feedScroller.setBorder(BorderFactory.createLineBorder(defaultPanelColor, 10));

        right.add(messageScroller) ;
        right.add(Box.createVerticalStrut(10));
        right.add(feedScroller) ;

        jFrame.getContentPane().add(BorderLayout.EAST,right) ;

        // Todo:- Extract this method.
        GridLayout gridLayout = new GridLayout(16,16) ;
        gridLayout.setVgap(1); // padding code taken
        gridLayout.setHgap(2); // padding code taken

        JPanel jPanel = new JPanel(gridLayout) ;
        // phle mai grid layout mai checkbox add krke , usko frame ko de raha. Nahi kaam kiya
        checkBoxData = new ArrayList<>() ;

        for(int i=0;i<256;i++){
            JCheckBox jCheckBox = new JCheckBox() ;
            // Ye ek hi baar initialise ho raha
            jCheckBox.setSelected(false);

            jPanel.add(jCheckBox) ;
            // dusre method mai inki checked status dekhna tha
            checkBoxData.add(jCheckBox) ;
        }

        jFrame.getContentPane().add(BorderLayout.CENTER,jPanel) ;

        //jFrame.setSize(400,400);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    private JButton getjButton() {
        JButton refresh = new JButton("Refresh Beat") ;
        refresh.addActionListener(
                e -> {
                    sequencer.stop(); // business logic :)
                    int response = JOptionPane.showConfirmDialog(jFrame,
                            "Do You want to clear up your current developed beat?",
                            "Confirm",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);

                    if(response==JOptionPane.YES_OPTION){
                        for (JCheckBox checkBox : checkBoxData) {
                            checkBox.setSelected(false);
                            // Frame.repaint ki need nahi hai - setSelected method already handles rendering part.
                        }
                    }
                }
        );
        return refresh;
    }

    // Sequencer chahiye tha Stop Button ko
    // And since teeno connected , isliye abhi risk nahi liya.
    Sequencer sequencer ;
    Track track ;
    Sequence sequence ;

    public class MusicalAspects{

        MusicalAspects(){
            midiSetUp();
        }

        public void midiSetUp(){
            try {
                sequencer = MidiSystem.getSequencer() ;
                sequencer.open();

                sequence = new Sequence(Sequence.PPQ, 4) ;

                track = sequence.createTrack() ; // initially sequence will have an empty track.

                sequencer.setSequence(sequence);

                sequencer.start();
            } catch (MidiUnavailableException | InvalidMidiDataException e) {
                throw new RuntimeException(e);
            }
        }

        public void playSong() {
            sequence.deleteTrack(track);
            track = sequence.createTrack();

            // It is making track based on checkbox list.
            // So if I clean up check box list this will get cleaned. The prev track is any way getting cleaned.
            for (int i = 0; i < 16; i++) {
                int key = instruments[i];
                //List<Integer> ticks = new ArrayList<>() ;

                for (int j = 0; j < 16; j++) {
                    if (checkBoxData.get(i * 16 + j).isSelected()) {
                        track.add(makeMidiEvent(ShortMessage.NOTE_ON, 9, key, 100, j));
                        //ticks.add(j) ;
                        track.add(makeMidiEvent(ShortMessage.NOTE_OFF, 9, key, 100, j + 1));
                    }
                }
                track.add(makeMidiEvent(ShortMessage.CONTROL_CHANGE, 1, 127, 0, 16)); // copied
            }
            track.add(makeMidiEvent(ShortMessage.PROGRAM_CHANGE, 9, 1, 0, 15));

            try {
                sequencer.setSequence(sequence);
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY); // copied - isse chalega loop ai
                sequencer.setTempoInBPM(120.0f); // hr new made beat ke liye.
                sequencer.start(); // Ye to obvious.
            } catch (InvalidMidiDataException ex) {
                throw new RuntimeException(ex);
            }
        }

        public MidiEvent makeMidiEvent(int command, int channel, int key, int volume, int tick) {
            ShortMessage shortMessage = new ShortMessage() ;
            try {
                shortMessage.setMessage(command,channel,key,volume);
            } catch (InvalidMidiDataException e) {
                throw new RuntimeException(e);
            }
            return new MidiEvent(shortMessage,tick) ;
        }

    }

    // They are our heroes - they are doing our serialization and de-serialization.
    ObjectOutputStream objectOutputStream ;
    ObjectInputStream objectInputStream;

    public class NetworkingAspect implements AutoCloseable{

        NetworkingAspect(){
            setUpNetworking();
            ExecutorService executorService = Executors.newSingleThreadExecutor() ;
            executorService.submit( new ServerListeningTask() ) ; // Instance dena parta hai yaar runnable class ka bhi
        }

        public void setUpNetworking(){

            String serverHost = System.getenv("SERVER_HOST");
            if (serverHost == null || serverHost.isEmpty()) {
                serverHost = "127.0.0.1"; // Default for local execution
            }

            InetSocketAddress inetSocketAddress = new InetSocketAddress(serverHost,8080) ;

            try {
                SocketChannel channel = SocketChannel.open(inetSocketAddress) ;

                // Servers se Different users ke message ke liye continously listen
                // Then Deserialization and Re-Rendering
                // Reader reader = Channels.newReader(channel, StandardCharsets.UTF_8) ;
                // BufferedReader = new BufferedReader(reader) ;
                objectInputStream = new ObjectInputStream(channel.socket().getInputStream()) ;

                // Button mai send to server
                // Validation , then Serialization. Then send to Server
                // where my CRM thread is waiting and proceeds to do next stuff.
                // Writer writer = Channels.newWriter(channel,StandardCharsets.UTF_8) ;
                // printWriter = new PrintWriter(writer) ;
                objectOutputStream = new ObjectOutputStream( channel.socket().getOutputStream() ) ;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Said that it is a better alternative to using finalize , which has been deprecated.
        @Override
        public void close() throws IOException {
            objectOutputStream.flush();
            objectOutputStream.close();

            objectInputStream.close();

            //System.out.println("Closed Connection") ;
        }

        public class ServerListeningTask implements Runnable{
            SerializationClass serializationClass;

            @Override
            public void run() {
                try {
                    while( (serializationClass = (SerializationClass) objectInputStream.readObject()) != null ) {

                        //System.out.println("Message Came from Server") ;

                        // First , A Blocking dialog box shall appear.
                        int response = JOptionPane.showConfirmDialog(jFrame,
                                "A BeatMessage has arrived , do you want to use it ?",
                                "Confirm",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);

                        if (response == JOptionPane.YES_OPTION ) {
                            List<Boolean> serverSequence = serializationClass.getBeatSequence() ;
                            for(int i=0;i<256;i++){
                                checkBoxData.get(i).setSelected(serverSequence.get(i));
                                // Ek hi cient pai test krna hai , to jo sequence ho uska alternate kr do
                                // This way you can check ki beat sequence adequately serialize/de-serialize and sent.
                            } // This damn-thing here. It is taking care of it's re-rendering.

                            SwingUtilities.invokeLater(() -> {
                                // It is an Event Dispatch thread
                                // Provides safety from multi-threading issue
                                // Gui updation ke time Listening wala task bhi chalta rahe.
                                feed.setText(feed.getText() + serializationClass.getUserMessage());
                            });
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void sendToNetwork(){
            if (message.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(jFrame, "Message is empty. Please enter some text");
            }else{
                try {
                    //System.out.println("Sending message to server") ;
                    String complete = message.getText().trim()+"\n" ;

                    // Yeah baby , finally used Stream API , you can also use it with channel
                    List<Boolean> options = checkBoxData.stream().map(JCheckBox::isSelected).collect(Collectors.toList());

                    SerializationClass serializationClass = new SerializationClass(complete,options) ;
                    objectOutputStream.writeObject(serializationClass);
                    objectOutputStream.flush();

                    message.setText("");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

}
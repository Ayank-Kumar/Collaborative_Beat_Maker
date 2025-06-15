import java.io.Serializable;
import java.util.List;

public class SerializationClass implements Serializable {

    private final String userMessage ;
    private final List<Boolean> beatSequence ;

    public SerializationClass(String userMessage, List<Boolean> beatSequence) {
        this.userMessage = userMessage;
        this.beatSequence = beatSequence;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public List<Boolean> getBeatSequence() {
        return beatSequence;
    }

    @Override
    public String toString() {
        return "SerializationClass{" +
                "userMessage='" + userMessage + '\'' +
                '}';
    }
}

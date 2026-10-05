import com.example.rokidkeyboardbridge.RadioPlaybackPolicy;

public final class RadioPlaybackPolicyTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    private static byte[] samples(int amplitude) {
        byte[] pcm = new byte[640];
        for (int i = 0; i < pcm.length; i += 2) {
            int value = i % 4 == 0 ? amplitude : -amplitude;
            pcm[i] = (byte)value; pcm[i+1] = (byte)(value >> 8);
        }
        return pcm;
    }
    public static void main(String[] args) {
        check(RadioPlaybackPolicy.canPlay(true, 7, 7, 200, 100, false));
        check(!RadioPlaybackPolicy.canPlay(false, 7, 7, 200, 100, false));
        check(!RadioPlaybackPolicy.canPlay(true, 8, 7, 200, 100, false));
        check(!RadioPlaybackPolicy.canPlay(true, 7, 7, 100, 100, false));
        check(!RadioPlaybackPolicy.canPlay(true, 7, 7, 200, 100, true));
        RadioPlaybackPolicy detector = new RadioPlaybackPolicy();
        byte[] silence = samples(0), noise = samples(28), speech = samples(900);
        for(int i=0;i<5;i++)check(!detector.speechDetected(silence, silence.length));
        check(!detector.speechDetected(noise, noise.length));
        check(!detector.speechDetected(speech, speech.length));
        for(int i=0;i<5;i++)check(!detector.speechDetected(silence, silence.length));
        check(!detector.speechDetected(speech, speech.length));
        for(int i=0;i<33;i++)check(!detector.speechDetected(speech,speech.length)); check(detector.speechDetected(speech, speech.length));
        check(!new RadioPlaybackPolicy().speechDetected(new byte[1], 1));
        check(RadioPlaybackPolicy.voice(samples(32768), 640)); // -32768 must not overflow abs(short).
        check(!RadioPlaybackPolicy.voice(new byte[2], 500)); // Bound reads to actual PCM.
        RadioPlaybackPolicy noisyRoom = new RadioPlaybackPolicy(1000);
        check(!noisyRoom.speechDetected(speech, speech.length));
        byte[] loud = samples(1200);
        check(!noisyRoom.speechDetected(loud, loud.length));
        for(int i=0;i<33;i++)check(!noisyRoom.speechDetected(loud,loud.length)); check(noisyRoom.speechDetected(loud, loud.length));
        RadioPlaybackPolicy radioMic = new RadioPlaybackPolicy(120);
        byte[] lowNoise = samples(58), userVoice = samples(900);
        for (int i = 0; i < 50; i++) check(!radioMic.speechDetected(lowNoise, lowNoise.length));
        check(!radioMic.speechDetected(userVoice, userVoice.length));
        for(int i=0;i<33;i++)check(!radioMic.speechDetected(userVoice,userVoice.length)); check(radioMic.speechDetected(userVoice, userVoice.length));
        System.out.println("RadioPlaybackPolicyTest: " + checks + " checks passed");
    }
}

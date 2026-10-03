import com.example.rokidkeyboardbridge.RadioResumePolicy;
public final class RadioResumeTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        for (String mode : new String[]{"continuous", "sound", "vision", "both"}) {
            RadioResumePolicy p = new RadioResumePolicy();
            p.suspend(7); check(p.suspended());
            check(!p.ready(true, mode, 7, true, 0));
            check(!p.ready(true, mode, 7, false, 100));
            check(!p.ready(true, mode, 7, false, 2599));
            check(p.ready(true, mode, 7, false, 2600)); check(!p.suspended());
            p.suspend(7); check(!p.ready(true, mode, 7, false, 3000));
            check(!p.ready(true, mode, 7, true, 5000));
            check(!p.ready(true, mode, 7, false, 6000));
            check(!p.ready(true, mode, 7, false, 8499));
            check(p.ready(true, mode, 7, false, 8500));
            p.suspend(7); check(!p.ready(false, mode, 7, false, 9000)); check(!p.suspended());
            p.suspend(7); check(!p.ready(true, mode, 8, false, 9000)); check(!p.suspended());
            p.suspend(7); p.clear(); check(!p.suspended());
        }
        RadioResumePolicy p = new RadioResumePolicy(); p.suspend(7);
        check(!p.ready(true, "scheduled", 7, false, 0)); check(!p.suspended());
        System.out.println("RadioResumeTest: " + checks + " checks passed");
    }
}

package ApJavaProject;

import org.junit.Test;
import static org.junit.Assert.*;

public class ParameterTest {
    @Test
    public void getArgPort() {
        assertEquals(1234, Cli.getArgPort(new String[]{"1234"}));
        assertEquals(-1, Cli.getArgPort(new String[]{"-1234"}));
        assertEquals(-1, Cli.getArgPort(new String[]{"1234.0"}));
        assertEquals(-1, Cli.getArgPort(new String[]{"1-2.3"}));
        assertEquals(-1, Cli.getArgPort(new String[]{"ABC"}));
        assertEquals(-1, Cli.getArgPort(new String[]{""}));
        assertEquals(-1, Cli.getArgPort(new String[]{}));
    }

    @Test
    public void getArgIP() {
        assertEquals("0.0.0.0", Cli.getArgIP(new String[]{null, "0.0.0.0"}));
        assertEquals("192.168.1.2", Cli.getArgIP(new String[]{null, "192.168.1.2"}));
        assertNull(Cli.getArgIP(new String[]{null, "192000.168000.1000.2000"}));
        assertNull(Cli.getArgIP(new String[]{null}));
        assertNull(Cli.getArgIP(new String[]{}));
    }

    @Test
    public void getArgID() {
        assertEquals("", Cli.getArgID(new String[]{null, null, ""}));
        assertEquals("A", Cli.getArgID(new String[]{null, null, "A"}));
        assertNull(Cli.getArgID(new String[]{null, null}));
        assertNull(Cli.getArgID(new String[]{}));
    }
}
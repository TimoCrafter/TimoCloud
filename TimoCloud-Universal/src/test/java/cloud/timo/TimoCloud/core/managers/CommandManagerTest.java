package cloud.timo.TimoCloud.core.managers;

import cloud.timo.TimoCloud.core.TimoCloudCore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TimoCloudCore.class)
public class CommandManagerTest {

    @Mock
    private TimoCloudCore timoCloudCore;
    @Mock
    private CoreServerManager serverManager;

    private CommandManager commandManager;

    @Before
    public void setUp() {
        commandManager = new CommandManager();
        PowerMockito.mockStatic(TimoCloudCore.class);
        when(TimoCloudCore.getInstance()).thenReturn(timoCloudCore);
        when(timoCloudCore.getServerManager()).thenReturn(serverManager);
    }

    @Test
    public void onCommandReload() {
        CoreFileManager fileManager = mock(CoreFileManager.class);
        when(timoCloudCore.getFileManager()).thenReturn(fileManager);
        Consumer<String> sendMessage = mock(Consumer.class);
        commandManager.onCommand(sendMessage, true, "reload");
        verify(fileManager, times(1)).load();
        verify(serverManager, times(1)).loadGroups();
        verify(sendMessage, times(1)).accept(anyString());
    }
}
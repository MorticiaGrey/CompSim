package com.morticia.compsim;

import com.morticia.compsim.IO.IOHandler;
import com.morticia.compsim.Machine.MachineHandler;

public class RuntimeHandler {
    /**
     * Function called to start default routine, but the point is to be a library not an application
     */
    public static void entryPoint() {
        // Uncomment this when graphical development is more relevant
        //GraphicalEntry.launch();
        IOHandler ioHandler = new IOHandler();
        MachineHandler machineHandler = new MachineHandler();

        machineHandler.initDefaultMachines();

        ioHandler.start();
        machineHandler.start();
    }
}
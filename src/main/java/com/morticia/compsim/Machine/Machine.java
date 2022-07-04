package com.morticia.compsim.Machine;

import com.morticia.compsim.Machine.Device.StaticDevice;
import com.morticia.compsim.Machine.Event.Event;
import com.morticia.compsim.Machine.Event.EventHandler;
import com.morticia.compsim.Machine.Filesystem.Filesystem;
import com.morticia.compsim.Util.Constants;
import com.morticia.compsim.Util.Disk.DataHandler;
import com.morticia.compsim.Util.Disk.DiskFile;
import com.morticia.compsim.Util.Disk.DiskUtil;

import java.util.ArrayList;
import java.util.List;

public class Machine {
    public int id;
    public String desig; // Stands for designation

    public Filesystem filesystem;

    public DiskFile metaFile;
    public DataHandler dataHandler;

    public EventHandler eventHandler;

    public List<StaticDevice> staticDevices;

    public Machine(String desig) {
        this.id = MachineHandler.assignId();
        this.desig = id + "_" + desig;

        if (!DiskUtil.populateMachine(this.desig)) {
            printError("machine population failed");
        }

        this.filesystem = new Filesystem(this);

        // Keep at the end
        this.metaFile = new DiskFile(getMachineDir(), "meta.dt", true);
        this.dataHandler = new DataHandler(metaFile);
        if (!dataHandler.load()) {
            save();
        }
        
        this.eventHandler = new EventHandler(this);
        // TODO: 7/4/22 Remove this post debugging stage
        this.eventHandler.events.add(new Event(this, "tst", "misc"));

        // TODO: 7/3/22 Make this initialize from metafile (current setup is for debugging)
        staticDevices = new ArrayList<>();
        staticDevices.add(new StaticDevice("debug", this));

        // Execute boot script
        filesystem.executeScript("/boot/boot.lua");

        // TODO: 7/4/22 Remove after debugging stage
        eventHandler.triggerEvent("tst", new ArrayList<>());
    }

    public void tick() {

    }

    public void save() {
        dataHandler.add(desig, Constants.str_type, "machine_desig");
        dataHandler.save();
    }

    public String getMachineDir() {
        return "/Machines/" + desig;
    }

    private void printError(String message) {
        System.out.println("[" + id + "/" + desig + "]: " + message);
    }
}

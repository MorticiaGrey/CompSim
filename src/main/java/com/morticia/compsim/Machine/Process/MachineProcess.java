package com.morticia.compsim.Machine.Process;

import com.morticia.compsim.IO.GUI.Terminal;
import com.morticia.compsim.Machine.Filesystem.ExecutionPermissions;
import com.morticia.compsim.Machine.Filesystem.VirtualFile;
import com.morticia.compsim.Machine.Filesystem.VirtualFolder;
import com.morticia.compsim.Machine.Machine;
import com.morticia.compsim.Machine.MachineIOStream.MachineIOStream;
import com.morticia.compsim.Util.Disk.DiskUtil;
import com.morticia.compsim.Util.Lua.Lib.ProcessLib;
import com.morticia.compsim.Util.Lua.Lib.TerminalLib;
import com.morticia.compsim.Util.Lua.LuaLib;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;

/**
 * This class mostly serves to group files and make sharing data between files easier
 *
 * @author Morticia
 * @version 1.0
 * @since 7/10/22
 */

public class MachineProcess {
    public Machine machine;
    public ProcessHandler handler;
    public int id;
    public String processName;
    // 0 = active, 1 = ready, 2 = waiting, 3 = interrupted
    public int statusCode;
    public String statusMsg;
    public boolean continuous;

    public VirtualFolder workingDir;
    public VirtualFile rootFile;
    public VirtualFile currFile;

    public ExecutionPermissions execPerms;

    public Globals globals;
    public boolean resetGlobalsWhenComplete;
    public boolean passGlobalsToFork;

    public MachineIOStream stream;

    public LuaTable processTable;

    /**
     * Constructor
     *
     * @param handler The process handler this is attached to
     * @param processName The name of this process
     * @param rootFilePath The path to the root file, which is called on start
     */
    public MachineProcess(ProcessHandler handler, String processName, String rootFilePath) {
        this.machine = handler.machine;
        this.handler = handler;
        this.id = handler.assignId();
        this.processName = processName;
        this.statusCode = 2;
        updateStatusMsg();
        // TODO: 7/10/22 Somehow find a way to know if things are background or not
        this.continuous = false;
        this.rootFile = machine.filesystem.getFile(rootFilePath);
        try {
            this.workingDir = rootFile.parent;
        } catch (NullPointerException e) {
            System.out.println("[" + rootFilePath + "] file not found");
        }
        this.currFile = rootFile;
        this.execPerms = new ExecutionPermissions();
        execPerms.canExecute = true;
        execPerms.setLibAccess(new String[] {
                "std"
        });
        updateGlobals();
        this.resetGlobalsWhenComplete = false;
        this.passGlobalsToFork = false;
        this.stream = machine.defaultStream;
        this.processTable = new LuaTable();
    }

    /**
     * Resets the lua globals this process uses for execution
     */
    public void updateGlobals() {
        LuaLib lib = new LuaLib(execPerms);
        if (stream == null) {
            this.globals = lib.prepUserGlobals(machine);
        } else {
            this.globals = lib.prepUserGlobals(machine, stream);
        }
    }

    /**
     * Sets the status code and updates the message
     *
     * @param code The new status code
     */
    public void setStatus(int code) {
        this.statusCode = code;
        updateStatusMsg();
    }

    /**
     * Updates the status message to match the code
     */
    public void updateStatusMsg() {
        switch (this.statusCode) {
            case 0 -> statusMsg = "active";
            case 1 -> statusMsg = "ready";
            case 2 -> statusMsg = "waiting";
            case 3 -> statusMsg = "interrupted";
        }
    }

    /**
     * Starts the process
     */
    public void start() {
        setStatus(0);
        execFile(rootFile.getPath());
    }

    /**
     * Executes a file with the process globals and passing the relevant data
     *
     * @param path Path to the script to be executed
     */
    public void execFile(String path) {
        VirtualFile f = machine.filesystem.getFile(path);
        if (f == null) {
            System.out.println("[" + path + "] file not found");
            return;
        }

        // Not using the DiskFile#execute so I have more granularity
        if (execPerms.canExecute && statusCode != 3) {
            // Add data
            try {
                setStatus(0);
                LuaTable paramsTable = new LuaTable();
                if (stream.component instanceof Terminal) {
                    paramsTable.set("terminal", stream.component.toTable());
                } else {
                    paramsTable.set("terminal", TerminalLib.getBlankTerminalTable(machine, -1));
                    paramsTable.set("stream", stream.component.toTable());
                }
                paramsTable.set("process", toTable());
                globals.set("params", paramsTable);
                globals.set("process_table", processTable);
                LuaValue val =  globals.loadfile(f.trueFile.path.toString()).call();
                try {
                    if (val.get("globals") != null) machine.machineGlobals = (LuaTable) val.get("globals");
                } catch (Exception ignored) {}
                try {
                    if (val.get("kernel_table") != null) machine.kernelGlobals = (LuaTable) val.get("kernel_table");
                } catch (Exception ignored) {}
                try {
                    if (val.get("process_table") != null) processTable = (LuaTable) val.get("process_table");
                } catch (Exception ignored) {}
                if (resetGlobalsWhenComplete) {
                    updateGlobals();
                }
                setStatus(1);
            } catch (Exception e) {
                machine.guiHandler.p_terminal.println(Terminal.wrapInColor(DiskUtil.removeObjectivePaths(e.getMessage(), machine.desig), "f7261b"));
                e.printStackTrace();
                setStatus(1);
            }
        }
    }

    /**
     * Kills the process
     */
    public void kill() {
        statusCode = 3;
        updateStatusMsg();
    }

    /**
     * Converts this process to a lua table, a data structure compatible with lua execution
     *
     * @return The lua table formatted
     */
    public LuaTable toTable() {
        LuaTable table = new LuaTable();
        table.set("is_null", LuaValue.valueOf(false));
        table.set("id", id);
        table.set("object_type", "process");
        table.set("name", processName);
        table.set("add_globals", new ProcessLib.add_globals(this));
        table.set("reset_globals", new ProcessLib.reset_globals(this));
        table.set("reset_globals_when_complete", new ProcessLib.reset_globals_when_complete(this));
        table.set("pass_globals", new ProcessLib.pass_globals(this));
        table.set("fork", new ProcessLib.fork(this));
        table.set("set_file", new ProcessLib.set_file(this));
        table.set("start", new ProcessLib.start(this));
        table.set("run", new ProcessLib.run(this));
        return table;
    }

    /**
     * Gets a table formatted similar to a full table
     *
     * @param id Id of the process
     * @return The blank table
     */
    public static LuaTable getBlankTable(int id) {
        LuaTable table = new LuaTable();
        table.set("is_null", LuaValue.valueOf(true));
        table.set("id", id);
        table.set("object_type", "process");
        table.set("name", "null");
        return table;
    }
}

package com.morticia.compsim.Machine.GUI;

import com.morticia.compsim.IO.GUI.Terminal;
import com.morticia.compsim.Machine.Event.Event;
import com.morticia.compsim.Machine.Machine;
import com.morticia.compsim.RuntimeHandler;

import java.util.ArrayList;
import java.util.List;

// This class will just do terminals for now but it's functionality will be expanded later
public class GUIHandler {
    public Machine machine;

    public List<Terminal> terminals;
    public Terminal p_terminal; // Primary terminal, the one currently in focus

    public GUIHandler(Machine machine) {
        this.machine = machine;
        this.terminals = new ArrayList<>();
    }

    public void registerKeyEvents() {
        /*
        key pressed, key released, text_entered
         */
        machine.eventHandler.events.add(new Event(machine, "key_pressed", "key"));
        machine.eventHandler.events.add(new Event(machine, "key_released", "key"));
        machine.eventHandler.events.add(new Event(machine, "text_entered", "key"));
    }

    public void registerMouseEvents() {
        /*
        mouse button pressed, mouse scroll, mouse moved
         */
        machine.eventHandler.events.add(new Event(machine, "button_pressed", "mouse"));
        machine.eventHandler.events.add(new Event(machine, "scrolled", "mouse"));
        machine.eventHandler.events.add(new Event(machine, "moved", "mouse"));
    }

    // This function is somewhat temporary as graphical capability will be built into lua, however
    // this is a lot more convenient for debugging as I'm familiar with coding this kind of thing
    // and it will greatly reduce the number of moving parts to do it like this. And it won't be
    // hard to change later. It uses events so the GUI will run in the IO thread
    public void startTerminal() {
        // TODO: 7/4/22 Add ids for terminals, doing later because I'm tired
        int id = terminals.size() - 1;

        RuntimeHandler.ioHandler.events.add(new Event(machine, "start_terminal", Integer.toString(id)));
        // Terminal is added to our side by event handling in IOHandler
    }

    public void endTerminal(int id) {
        RuntimeHandler.ioHandler.events.add(new Event(machine, "end_terminal", Integer.toString(id)));
        this.terminals.removeIf(i -> i.id == id);
    }
}

globals["is_shell_enabled"] = false

tempIO = {}
stream.set_output(tempIO)

local t = terminal.new_terminal()
while not t.is_ready() do

end
t = t.update()

t.set_output()
print = t.print
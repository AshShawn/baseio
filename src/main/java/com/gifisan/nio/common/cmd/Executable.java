package com.gifisan.nio.common.cmd;

import java.util.HashMap;

public interface Executable {

	CmdResponse exec(CommandContext context, HashMap<String, String> params);
}

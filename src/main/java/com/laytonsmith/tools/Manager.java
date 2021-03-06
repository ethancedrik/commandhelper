package com.laytonsmith.tools;

import com.laytonsmith.PureUtilities.FileUtility;
import com.laytonsmith.PureUtilities.StringUtils;
import static com.laytonsmith.PureUtilities.TermColors.*;
import com.laytonsmith.core.CHLog;
import com.laytonsmith.core.Installer;
import com.laytonsmith.core.MethodScriptCompiler;
import com.laytonsmith.core.MethodScriptExecutionQueue;
import com.laytonsmith.core.PermissionsResolver;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.environments.GlobalEnv;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.profiler.Profiler;
import com.laytonsmith.persistance.DataSource;
import com.laytonsmith.persistance.DataSourceException;
import com.laytonsmith.persistance.DataSourceFactory;
import com.laytonsmith.persistance.PersistanceNetwork;
import com.laytonsmith.persistance.ReadOnlyException;
import com.laytonsmith.persistance.SerializedPersistance;
import com.laytonsmith.persistance.io.ConnectionMixinFactory;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author layton
 */
public class Manager {

	private static Profiler profiler;
	private static GlobalEnv gEnv;
	private static final File jarLocation = new File(Interpreter.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
	private static final File chDirectory = new File(jarLocation, "CommandHelper");
	private static PersistanceNetwork persistanceNetwork;

	public static void start() throws IOException, DataSourceException, URISyntaxException {
		ConnectionMixinFactory.ConnectionMixinOptions options = new ConnectionMixinFactory.ConnectionMixinOptions();
		options.setWorkingDirectory(chDirectory);
		persistanceNetwork = new PersistanceNetwork(new File(chDirectory, "persistance.config"), 
				new File(chDirectory, "persistance.db").toURI(), options);
		Installer.Install(chDirectory);
		CHLog.initialize(chDirectory);
		profiler = new Profiler(new File(chDirectory, "profiler.config"));
		gEnv = new GlobalEnv(new MethodScriptExecutionQueue("Manager", "default"), profiler, persistanceNetwork, new PermissionsResolver.PermissiveResolver(), chDirectory);
		cls();
		pl("\n" + Static.Logo() + "\n\n" + Static.DataManagerLogo());


		pl("Starting the Data Manager...");
		try {
			Environment env = Environment.createEnvironment(gEnv, new CommandHelperEnvironment());
			MethodScriptCompiler.execute(MethodScriptCompiler.compile(MethodScriptCompiler.lex("player()", null, true)), env, null, null);
		} catch (ConfigCompileException ex) {
		}
		pl(GREEN + "Welcome to the CommandHelper " + CYAN + "Data Manager!");
		pl(BLINKON + RED + "Warning!" + BLINKOFF + YELLOW + " Be sure your server is not running before using this tool to make changes to your database!");
		pl("------------------------");
		boolean finished = false;
		do {
			pl(YELLOW + "What function would you like to run? Type \"help\" for a full list of options.");
			String input = prompt();
			pl();
			if (input.toLowerCase().startsWith("help")) {
				help(input.replaceFirst("help ?", "").toLowerCase().split(" "));
			} else if (input.equalsIgnoreCase("refactor")) {
				pl("refactor - That feature isn't implemented yet :(");
			} else if (input.toLowerCase().startsWith("print")) {
				print(input.replaceFirst("print ?", "").toLowerCase().split(" "));
			} else if (input.equalsIgnoreCase("cleardb")) {
				cleardb();
			} else if (input.equalsIgnoreCase("edit")) {
				edit();
			} else if (input.equalsIgnoreCase("upgrade")) {
				upgrade();
			} else if (input.equalsIgnoreCase("merge")) {
				merge();
			} else if (input.equalsIgnoreCase("interpreter")) {
				Interpreter.start(null);
			} else if (input.equalsIgnoreCase("exit")) {
				pl("Thanks for using the " + CYAN + "Data Manager!" + reset());
				finished = true;
			} else {
				pl("I'm sorry, that's not a valid command. Here's the help:");
				help(new String[]{});
			}
		} while (finished == false);
	}

	public static void merge() {
		cls();
		pl(GREEN + "Transferring a database takes all the keys from a database connection, and puts\n"
				+ "them straight into another database. If there are key conflicts, this tool will prompt\n"
				+ "you for an action.");
		ConnectionMixinFactory.ConnectionMixinOptions mixinOptions = new ConnectionMixinFactory.ConnectionMixinOptions();
		mixinOptions.setWorkingDirectory(chDirectory);
		DataSource source;
		DataSource destination;

		do {
			//Verify that the destination doesn't have any of the keys in the source, barring keys that have
			//the exact same value.
			do {
				//Get the source connection set up
				pl(YELLOW + "What is the source connection you would like to read the keys from?\n"
						+ "(Type the connection exactly as you would in the persistance configuration,\n"
						+ "aliases are not supported)");
				String ssource = prompt();
				try {
					source = DataSourceFactory.GetDataSource(ssource, mixinOptions);
					break;
				} catch (DataSourceException ex) {
					pl(RED + ex.getMessage());
				} catch (URISyntaxException ex) {
					pl(RED + ex.getMessage());
				}
			} while (true);

			do {
				//Get the destination connection set up
				pl(YELLOW + "What is the destination connection?");
				String sdestination = prompt();
				try {
					destination = DataSourceFactory.GetDataSource(sdestination, mixinOptions);
					break;
				} catch (DataSourceException ex) {
					pl(RED + ex.getMessage());
				} catch (URISyntaxException ex) {
					pl(RED + ex.getMessage());
				}
			} while (true);
			try {
				//Run through all the source's keys, and check to see that either the
				//destination's key doesn't exist, or the value at that key is the
				//exact same as the source's value
				boolean acceptAllDestination = false;
				boolean acceptAllSource = false;
				for (String[] key : source.keySet()) {
					if (destination.hasKey(key)) {
						if (!source.get(key).equals(destination.get(key))) {
							String data;
							//If the key is null, it's empty, so we can just stick it in, no
							//problem. If there is data there, it's a conflict, and we need to
							//ask.
							if (destination.get(key) != null) {
								boolean useSource = false;
								boolean useDestination = false;
								if (acceptAllDestination || acceptAllSource) {
									p(RED + "Conflict found for " + StringUtils.Join(key, ".") + ", using ");
									if (useSource) {
										useSource = true;
										p("source");
									} else {
										useDestination = true;
										p("destination");
									}
									pl(" value.");

								} else {
									pl(BG_RED + BRIGHT_WHITE + "The key " + StringUtils.Join(key, ".") + " has a different value"
											+ " in the source and the destination: " + reset());
									pl(WHITE + "Source: " + source.get(key));
									pl(WHITE + "Destination: " + destination.get(key));
									do {
										pl(YELLOW + "Would you like to keep " + CYAN + "S" + YELLOW + "ource, "
												+ "keep " + GREEN + "D" + YELLOW + "estination, keep " + MAGENTA
												+ "A" + YELLOW + "ll " + MAGENTA + "S" + YELLOW + "ource, or keep "
												+ BLUE + "A" + YELLOW + "ll " + BLUE + "D" + YELLOW + "estination?");
										pl(WHITE + "["
												+ CYAN + "S"
												+ WHITE + "/"
												+ GREEN + "D"
												+ WHITE + "/"
												+ MAGENTA + "AS"
												+ WHITE + "/"
												+ BLUE + "AD"
												+ WHITE + "]");
										String response = prompt();
										if ("AS".equalsIgnoreCase(response)) {
											acceptAllSource = true;
											useSource = true;
										} else if ("AD".equalsIgnoreCase(response)) {
											acceptAllDestination = true;
											useDestination = true;
										} else if ("S".equalsIgnoreCase(response)) {
											useSource = true;
										} else if ("D".equalsIgnoreCase(response)) {
											useDestination = true;
										} else {
											continue;
										}
										break;
									} while (true);
								}
								if (useSource) {
									data = source.get(key);
								} else if (useDestination) {
									data = destination.get(key);
								} else {
									throw new RuntimeException("Invalid state, both useSource and useDestination are false");
								}
								//Ok, now put the data in the destination
							} else {
								//Otherwise, just use the data in the source
								data = source.get(key);
							}
							destination.set(key, data);
						}
					} else {
						//Else there is no conflict, it's not in the destination.
						destination.set(key, source.get(key));
					}
				}
				break;
			} catch (DataSourceException ex) {
				pl(RED + ex.getMessage());
			} catch (ReadOnlyException ex) {
				pl(RED + ex.getMessage());
			} catch (IOException ex) {
				pl(RED + ex.getMessage());
			}
		} while (true);
		pl(GREEN + "Done merging!");
	}

	public static void cleardb() {
		try{
			pl(RED + "Are you absolutely sure you want to clear out your database? " + BLINKON + "No backup is going to be made." + BLINKOFF);
			pl(WHITE + "This will completely wipe your persistance information out. (No other data will be changed)");
			pl("[YES/No]");
			String choice = prompt();
			if (choice.equals("YES")) {
				pl("Positive? [YES/No]");
				if (prompt().equals("YES")) {
					p("Ok, here we go... ");
					Set<String[]> keySet = persistanceNetwork.getNamespace(new String[]{}).keySet();
					for(String [] key : keySet){
						try {
							persistanceNetwork.clearKey(key);
						} catch (ReadOnlyException ex) {
							pl(RED + "Read only data source found: " + ex.getMessage());
						}
					}
					pl("Done!");
				}
			} else if (choice.equalsIgnoreCase("yes")) {
				pl("No, you have to type YES exactly.");
			}
		} catch(DataSourceException ex){
			pl(RED + ex.getMessage());
		} catch(IOException ex){
			pl(RED + ex.getMessage());
		}
	}

	public static void help(String[] args) {
		if (args.length < 1 || args[0].isEmpty()) {
			pl("Currently, your options are:\n"
					+ "\t" + BLUE + "refactor" + WHITE + " - Allows you to shuffle data around in the persistance network more granularly than the merge tool.\n"
					+ "\t" + GREEN + "upgrade" + WHITE + " - Runs upgrade scripts on your persisted data\n"
					+ "\t" + GREEN + "print" + WHITE + " - Prints out the information from your persisted data\n"
					+ "\t" + GREEN + "cleardb" + WHITE + " - Clears out your database of persisted data\n"
					+ "\t" + GREEN + "edit" + WHITE + " - Allows you to edit individual fields\n"
					+ "\t" + GREEN + "interpreter" + WHITE + " - Command Line Interpreter mode. Most minecraft related functions don't work.\n"
					+ "\t" + GREEN + "merge" + WHITE + " - Merges an entire database from one backend into another, even across formats. (Not individual keys.)\n"
					+ "\t\tYou can also use this tool to an extent to import or export data.\n"
					+ "\n\t" + RED + "exit" + WHITE + " - Quits the Data Manager\n");

			pl("Type " + MAGENTA + "help <command>" + WHITE + " for more details about a specific command");
		} else {
			if("refactor".equals(args[0])){
				pl("Not yet implemented");
			} else if ("upgrade".equals(args[0])) {
				pl("Converts any old formatted data into the new format. Any data that doesn't explicitely"
						+ " match the old format is not touched. Do not use this utility unless specifically"
						+ " told to during upgrade notices.");
			} else if ("print".equals(args[0])) {
				pl("Prints out the information in your persistance file. Entries may be narrowed down by"
						+ " specifying the namespace (for instance " + MAGENTA + "print user.username" + WHITE
						+ " will only show that particular users's aliases.) This is namespace based, so you"
						+ " must provide the entire namespace that your are trying to narrow down."
						+ "(" + MAGENTA + "print storage" + WHITE + " is valid, but " + MAGENTA + "print stor"
						+ WHITE + " is not)");
			} else if ("cleardb".equals(args[0])) {
				pl("Wipes your database clean of CommandHelper's persistance entries, but not other data. This"
						+ " includes any data that CommandHelper would have inserted into the database, or data"
						+ " that CommandHelper otherwise knows how to use. If using SerializedPersistance, this"
						+ " means the entire file. For other data backends, this may vary slightly, for instance,"
						+ " an SQL backend would only have the CH specific tables truncated, but the rest of the"
						+ " database would remain untouched.");
			} else if ("edit".equals(args[0])) {
				pl("Allows you to manually edit the values in the database. You have the option to add or edit an existing"
						+ " value, delete a single value, or view the value of an individual key.");
			} else if ("interpreter".equals(args[0])) {
				pl("Generally speaking, works the same as the in game interpreter mode, but none"
						+ " of the minecraft related functions will work. You should not"
						+ " run this while the server is operational.");
			} else if ("merge".equals(args[0])){
				pl("The merge tool allows you to shuffle persisted data around as entire databases, not as individual keys, however."
						+ " You specify the source database, and the output database, and it copies all the database entries. This"
						+ " can be used to an extent to import and export values, but it is not granular at all. Key conflicts are"
						+ " handled by prompting the user for an action, whether to overwrite the destination's value, or to keep"
						+ " it as is. Thusly, this operation is very safe from accidentally deleting your data. Keys that don't exist"
						+ " in the destination already are simply copied, and keys that have the same value are skipped. No changes"
						+ " are made to the source database.");
			} else if ("exit".equals(args[0])) {
				pl("Exits the data manager");
			} else {
				pl("That's not a recognized command: '" + args[0] + "'");
			}
		}
	}

	public static void edit() {
		cls();
		while (true) {
			pl("Would you like to " + GREEN + "(a)dd/edit" + WHITE
					+ " a value, " + RED + "(r)emove" + WHITE + " a value, " + CYAN
					+ "(v)iew" + WHITE + " a single value, or "
					+ MAGENTA + "(s)top" + WHITE + " editting? [" + GREEN + "A" + WHITE + "/"
					+ RED + "R" + WHITE + "/" + CYAN + "V" + WHITE + "/" + MAGENTA + "S" + WHITE + "]");
			String choice = prompt();
			if (choice.equalsIgnoreCase("s") || choice.equalsIgnoreCase("exit")) {
				break;
			} else if (choice.equalsIgnoreCase("a")) {
				pl("Type the name of the key " + YELLOW + "EXACTLY" + WHITE + " as shown in the"
						+ " persistance format,\nnot the format you use when using store_value().");
				String key = prompt();
				pl("Provide a value for " + CYAN + key + WHITE + ". This value you provide will"
						+ " be interpreted as pure MethodScript. (So things like array() will work)");
				String value = prompt();
				if (doAddEdit(key, value)) {
					pl("Value changed!");
				}
			} else if (choice.equalsIgnoreCase("r")) {
				pl("Type the name of the key " + YELLOW + "EXACTLY" + WHITE + " as shown in the"
						+ " persistance format,\nnot the format you use when using store_value().");
				String key = prompt();
				if (doRemove(key)) {
					pl("Value removed!");
				} else {
					pl("That value wasn't in the database to start with");
				}
			} else if (choice.equalsIgnoreCase("v")) {
				pl("Type the name of the key " + YELLOW + "EXACTLY" + WHITE + " as shown in the"
						+ " persistance format,\nnot the format you use when using store_value().");
				String key = prompt();
				doView(key);
			} else {
				pl("I'm sorry, that's not a valid choice.");
			}
		}

	}

	public static boolean doView(String key) {
		try {
			String [] k = key.split("\\.");
			if (!persistanceNetwork.hasKey(k)) {
				pl(RED + "That value is not set!");
				return true;
			}
			pl(CYAN + key + ":" + WHITE + persistanceNetwork.get(k));
			return true;
		} catch (DataSourceException ex) {
			pl(RED + ex.getMessage());
			return false;
		}
	}

	public static boolean doAddEdit(String key, String valueScript) {
		try {
			Environment env = Environment.createEnvironment(gEnv, new CommandHelperEnvironment());
			Construct c = MethodScriptCompiler.execute(MethodScriptCompiler.compile(MethodScriptCompiler.lex(valueScript, null, true)), env, null, null);
			String value = Construct.json_encode(c, Target.UNKNOWN);
			pl(CYAN + "Adding: " + WHITE + value);
			String [] k = key.split("\\.");
			persistanceNetwork.set(k, value);
			return true;
		} catch (Exception ex) {
			pl(RED + ex.getMessage());
			return false;
		}
	}

	public static boolean doRemove(String key) {
		try {
			String [] k = key.split("\\.");
			if (persistanceNetwork.hasKey(k)) {
				persistanceNetwork.clearKey(k);
				return true;
			} else {
				return false;
			}
		} catch (Exception ex) {
			pl(RED + ex.getMessage());
			return false;
		}
	}

	public static void print(String[] args) {
		try{
			int count = 0;
			for(String [] key : persistanceNetwork.getNamespace(new String[]{}).keySet()){
				count++;
				pl(CYAN + StringUtils.Join(key, ".") + ": " + WHITE + persistanceNetwork.get(key));
			}
			pl(BLUE + count + " items found");
		} catch(Exception e){
			pl(RED + e.getMessage());
		}
	}

	public static void upgrade() {
		pl("\nThis will automatically detect and upgrade your persisted data. Though this will"
				+ " create a backup for you, you should manually back up your data before running"
				+ " this utility. You should not use this utility unless you are instructed to do"
				+ " so in the release notes.");
		pl("Would you like to continue? [" + GREEN + "Y" + WHITE + "/"
				+ RED + "N" + WHITE + "]");
		String choice = prompt();
		pl();
		if (choice.equalsIgnoreCase("y")) {
			//First we have to read in the preferences file, and see what persistance type they are using
			//Only serialization is supported right now
			String backingType = "serialization";
			if (backingType.equals("serialization")) {
				try {
					//Back up the persistance.ser file
					File db = new File("CommandHelper/persistance.ser");
					if (!db.exists()) {
						pl("Looks like you haven't used your persistance file yet.");
						return;
					}
					FileUtility.copy(db, new File("CommandHelper/persistance.ser.bak"), null);
					//Now, load in all the data
					SerializedPersistance sp;
					try {
						sp = new SerializedPersistance(db);
						try {
							sp.load();
						} catch (Exception ex) {
							pl(RED + ex.getMessage());
						}
						Map<String, String> data = sp.rawData();
						if (data.isEmpty()) {
							pl("Looks like you haven't used your persistance file yet.");
							return;
						}
						sp.clearAllData(); //Bye bye!
						//Ok, now we need to determine the type of data we're currently working with
						p(WHITE + "Working");
						int counter = 0;
						int changes = 0;
						Color[] colors = new Color[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA};
						for (String key : data.keySet()) {
							counter++;
							int c = counter / 20;
							if (c == ((double) counter / 20.0)) {
								p(color(colors[c % 6]) + ".");
							}
							if (key.matches("^plugin\\.com\\.sk89q\\.commandhelper\\.CommandHelperPlugin\\.commandhelper\\.function\\.storage\\..*")) {
								//We're in version 1, and we need to upgrade to version 2
								String newKey = "storage." + key.replaceFirst("plugin\\.com\\.sk89q\\.commandhelper\\.CommandHelperPlugin\\.commandhelper\\.function\\.storage\\.", "");
								sp.rawData().put(newKey, data.get(key));
								changes++;
							} else if (key.matches("^plugin\\.com\\.sk89q\\.commandhelper\\.CommandHelperPlugin\\..*?\\.aliases\\.\\d+$")) {
								//Pull out the parts we need
								Pattern p = Pattern.compile("^plugin\\.com\\.sk89q\\.commandhelper\\.CommandHelperPlugin\\.(.*?)\\.aliases\\.(\\d+)$");
								Matcher m = p.matcher(key);
								String newKey = null;
								if (m.find()) {
									String username = m.group(1);
									String id = m.group(2);
									newKey = "user." + username + ".aliases." + id;
								}
								//If something went wrong, just put the old one back in
								if (newKey == null) {
									sp.rawData().put(key, data.get(key));
								} else {
									sp.rawData().put(newKey, data.get(key));
									changes++;
								}
							} else {
								sp.rawData().put(key, data.get(key));
							}
						}
						try {
							sp.save();
						} catch (Exception ex) {
							pl(RED + ex.getMessage());
						}
						pl();
						pl(GREEN + "Assuming there are no error messages above, it should be upgraded now! (Use print to verify)");
						pl(CYAN.toString() + changes + " change" + (changes == 1 ? " was" : "s were") + " made");
					} catch (DataSourceException ex) {
						Logger.getLogger(Manager.class.getName()).log(Level.SEVERE, null, ex);
					}

				} catch (IOException ex) {
					pl(RED + ex.getMessage());
				}
			}
		} else {
			pl(RED + "Upgrade Cancelled");
		}
	}
}

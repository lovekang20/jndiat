
//JNDIAT by Quentin HARDY
//quentin.hardy@protonmail.com
import java.lang.Integer;
import java.lang.NumberFormatException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
//Import logging
import weblogic.logging.LoggingHelper;
import weblogic.logging.WLLevel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.io.File;
//Import commons.cli
/*
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
*/
//import others classes
//Dweblogic.StdoutDebugEnabled
//-Dssl.debug=true 
/*
    SEVERE
    WARNING
    INFO
    CONFIG
    FINE
    FINER
    FINEST
*/
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparsers;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentGroup;

public class Jndiat extends MyPrinter {
	// CONSTANTES
	public static final String PROGRAM_VERSION = "0.01";
	// Others
	private static Logger myLogger = Logger.getLogger("JNDIAT");
	private static boolean outputColored;

	public Jndiat(String args[]) {
		super();

		// Create arguments
		ArgumentParsers.setTerminalWidthDetection(true);
		ArgumentParser parser = ArgumentParsers.newArgumentParser("JndiAT").defaultHelp(true) // the default values of
																								// arguments are printed
																								// in help message.
				.description("JNDI Attacking Tool").version("${prog} version " + PROGRAM_VERSION + " (built: "
						+ new SimpleDateFormat("yyyy/MM/dd").format(Calendar.getInstance().getTime()) + ")");
		parser.addArgument("--version").dest("version").action(Arguments.version()).help("print version");
		// Create subcommands for arguments
		Subparsers subparsers = parser.addSubparsers().dest("module").title("subcommands")
				.description("valid operations").help("info").metavar("operations:");
		// All module
		// Subparser parserAll = subparsers.addParser("all").help("run all modules");
		// Scan module
		Subparser parserScan = subparsers.addParser("scan").help("scan to search JNDI through T3 protocol")
				.defaultHelp(true);
		addUsefulOptions(parserScan);
		ArgumentGroup commonGroupParserScan = parserScan.addArgumentGroup("common options");
		addServerOption(commonGroupParserScan);
		addCredentialsOptions(commonGroupParserScan);
		ArgumentGroup mandatoryGroupParserScan = parserScan.addArgumentGroup("a mandatory command");
		mandatoryGroupParserScan.addArgument("--ports").dest("ports").setDefault("").help("port(s) to scan");
		// BruteForce module
		Subparser parserBruteForce = subparsers.addParser("bruteforce").help("search valid credentials")
				.defaultHelp(true);
		addUsefulOptions(parserBruteForce);
		ArgumentGroup commonGroupParserBruteForce = parserBruteForce.addArgumentGroup("common options");
		addServerOption(commonGroupParserBruteForce);
		addPortArgument(commonGroupParserBruteForce);
		ArgumentGroup specificGroupParserBruteForce = parserBruteForce.addArgumentGroup("specific options");
		specificGroupParserBruteForce.addArgument("--cred-file").dest("cred-file").setDefault("")
				.help("credentials file to use");
		specificGroupParserBruteForce.addArgument("--separator").dest("separator").setDefault("/")
				.help("separator between login and pwd");
		// List module
		Subparser parserListJndi = subparsers.addParser("list").help("list all jndi").defaultHelp(true);
		addUsefulOptions(parserListJndi);
		addCommonOptions(parserListJndi);
		// DataSource
		Subparser parserDataSource = subparsers.addParser("datasource").help("connect to DataSource").defaultHelp(true);
		addUsefulOptions(parserDataSource);
		addCommonOptions(parserDataSource);
		ArgumentGroup specificGroupParserDataSource = parserDataSource.addArgumentGroup("specific options");
		specificGroupParserDataSource.addArgument("--datasource").dest("datasource")
				.help("specify the datasource to use");
		ArgumentGroup mandatoryDataSourceGroup = parserDataSource.addArgumentGroup("a mandatory command");
		mandatoryDataSourceGroup.addArgument("--sql-shell").dest("sqlshell").action(Arguments.storeTrue())
				.help("get a sql shell");
		mandatoryDataSourceGroup.addArgument("--listen-port").dest("listen-port").type(Integer.class)
				.help("listen for SQL requests locally");
		// MEJB
		Subparser parserMejb = subparsers.addParser("mejb").help("connect to MEJB").defaultHelp(true);
		addUsefulOptions(parserMejb);
		addCommonOptions(parserMejb);
		// DEPLOY
		Subparser parserDeployer = subparsers.addParser("deployer").help("deploy/undeploy an application")
				.defaultHelp(true);
		addUsefulOptions(parserDeployer);
		addCommonOptions(parserDeployer);
		ArgumentGroup mandatoryDeployerGroup = parserDeployer.addArgumentGroup("a mandatory command");
		mandatoryDeployerGroup.addArgument("--deploy").dest("deploy").action(Arguments.storeTrue())
				.help("deploy an application");
		mandatoryDeployerGroup.addArgument("--undeploy").dest("undeploy").action(Arguments.storeTrue())
				.help("undeploy an application");
		mandatoryDeployerGroup.addArgument("--list").dest("list").action(Arguments.storeTrue())
				.help("list applications");
		ArgumentGroup specificGroupParserDeployer = parserDeployer.addArgumentGroup("specific options");
		specificGroupParserDeployer.addArgument("--appl-file").dest("app-file").setDefault("cmd-linux.war")
				.help("application file");
		specificGroupParserDeployer.addArgument("--display-name").dest("display-name").setDefault("jndiat-application")
				.help("application display name");
		specificGroupParserDeployer.addArgument("--target").dest("target").setDefault("")
				.help("target on the remote server");
		// parse arguments given by user
		Namespace namespace = null;
		namespace = handlerErrorCatch(args, parser, namespace);

		// Confgure default logging
		myLogger.setUseParentHandlers(false);
		Handler conHdlr = new ConsoleHandler();
		conHdlr.setFormatter(new MyLogFormatter());
		myLogger.addHandler(conHdlr);

		// Parse common options
		// -> Color
		this.outputColored = true;
		colorEnable(namespace);
		// -> Verbosity
		System.setProperty("weblogic.log.StdoutSeverity", "off"); // http://edocs.weblogicfans.net/wls/docs81/ConsoleHelp/domain_server_logging_server.html
		System.setProperty("weblogic.log.StdoutLogStack", "false"); // Specifies whether to dump stack traces to the
																	// console when included in logged message.
		System.setProperty("weblogic.log.RedirectStdoutToServerLogEnabled", "true");
		System.setProperty("weblogic.log.RedirectStderrToServerLogEnabled", "true");
		setLevel(namespace);
		this.setWeblogicVerbosity();
		// print arguments and options given
		myLogger.fine("Arguments and Options given: " + namespace);
		scanningT3(namespace);
		// Search valid credentials
		searchValidCredential(namespace);
		// connect and list JNDI
		listJndi(namespace);
		// datasource module
		getSQLconnection(namespace);

		// mejb module
		accessingMejb(namespace);

		// deploy module
		deployOrUndeploy(namespace);
	}

	private void deployOrUndeploy(Namespace namespace) {
		if (namespace.getString("module") == "deployer") {
			Deployer deployer = new Deployer(namespace.getString("server"), namespace.getInt("port"), namespace.getString("username"),
					namespace.getString("password"), namespace.getString("target"));
			if (namespace.getBoolean("deploy") == true) {
				this.printTitle("Deploy the application " + namespace.getString("app-file"));
				if (new File(namespace.getString("app-file")).exists() == false) {
					this.printBadNews("The file " + namespace.getString("app-file")
							+ " doesn't exist. You must define an application to deploy with --appl-file");
				} else {
					myLogger.fine("The file " + namespace.getString("app-file") + " exists, continue...");
					myLogger.info("You want to deploy the application " + namespace.getString("app-file") + " in "
							+ namespace.getString("server") + ":" + namespace.getInt("port") + ", starting...");
					boolean status = deployer.deploy(namespace.getString("app-file"), namespace.getString("display-name"));
					if (status == false) {
						this.printBadNews("The application named " + namespace.getString("display-name") + " ("
								+ namespace.getString("app-file")
								+ ") has not been deployed. You have perhaps not enough privileges...");
					} else {
						this.printGoodNews("The application named " + namespace.getString("display-name") + " ("
								+ namespace.getString("app-file") + ") has been deployed");
					}
				}
			} else if (namespace.getBoolean("undeploy") == true) {
				this.printTitle("Undeploy the application named " + namespace.getString("display-name") + " remotely");
				myLogger.info("You want to undeploy the application " + namespace.getString("display-name") + " from "
						+ namespace.getString("server") + ":" + namespace.getInt("port") + ", starting...");
				boolean status = deployer.undeploy(namespace.getString("display-name"));
				if (status == false) {
					this.printBadNews(
							"The application named " + namespace.getString("display-name") + " has not been undeployed");
				} else {
					this.printGoodNews(
							"The application named " + namespace.getString("display-name") + " has been undeployed");
				}
			} else if (namespace.getBoolean("list") == true) {
				this.printTitle("List applications deployed");
				boolean status = deployer.listApplications();
			} else {
				myLogger.severe(
						"You must to choose a mandatory command (--deploy, --list or --undeploy) to run this module");
			}
		}
	}

	private void accessingMejb(Namespace namespace) {
		if (namespace.getString("module") == "mejb") {
			this.printTitle("Accessing the MEJB through T3 protocol");
			myLogger.info("You want to access the MEJB on the " + namespace.getString("server") + " target on port "
					+ namespace.getInt("port") + ", starting...");
			Mejb mejb = new Mejb(namespace.getString("server"), namespace.getInt("port"), namespace.getString("username"),
					namespace.getString("password"));
			mejb.getAllJMONames();
		}
	}

	private void getSQLconnection(Namespace namespace) {
		if (namespace.getString("module") == "datasource") {
			String datasourceToUse = "";
			myLogger.info("You want to get SQL connection from a datasource on the " + namespace.getString("server")
					+ " target on port " + namespace.getInt("port") + ", starting...");
			SQLDataSource sqlDataSource = new SQLDataSource(namespace.getString("server"), namespace.getInt("port"),
					namespace.getString("username"), namespace.getString("password"));
			// sqlshell
			if (namespace.getBoolean("sqlshell") == true) {
				this.printTitle("You want a SQL shell");
				String dataSourceArg = namespace.getString("datasource");
				if (dataSourceArg == null) {
					dataSourceArg = sqlDataSource.letHimSelectDatasource();
				}
				if (dataSourceArg != null) {
					myLogger.fine("You will use the datasource '" + dataSourceArg + "'");
					sqlDataSource.SQLshell(dataSourceArg);
				} else {
					myLogger.severe("No one datasource (JDBC) in JNDI list, cancelation ...");
				}
			}
			// listen for requests
			else if (namespace.getInt("listen-port") != null) {
				int port = namespace.getInt("listen-port");
				this.printTitle("You want listen for SQL requests");
				String dataSourceArg = namespace.getString("datasource");
				if (dataSourceArg == null) {
					dataSourceArg = sqlDataSource.letHimSelectDatasource();
				}
				if (dataSourceArg != null) {
					this.printGoodNews("Listening SQL requests for " + dataSourceArg + " on port " + port);
					sqlDataSource.listenFromRequest(port, dataSourceArg);
				} else {
					myLogger.severe("No one datasource (JDBC) in JNDI list, cancelation ...");
				}
			} else {
				myLogger.severe(
						"You must to choose a mandatory command (--sql-shell, --listen-port)  to run this module");
			}

		}
	}

	private void listJndi(Namespace namespace) {
		if (namespace.getString("module") == "list") {
			this.printTitle("Listing JNDI accessible with the T3 protocol");
			myLogger.info("You want to list JNDI on the " + namespace.getString("server") + " target on port "
					+ namespace.getInt("port") + ", starting...");
			JndiListing jndiListing = new JndiListing(namespace.getString("server"), namespace.getInt("port"),
					namespace.getString("username"), namespace.getString("password"));
			jndiListing.printJndi();
		}
	}

	private void searchValidCredential(Namespace namespace) {
		if (namespace.getString("module") == "bruteforce") {
			this.printTitle("Searching valid credentials");
			myLogger.info("You want to search valid credentials to " + namespace.getString("server") + " target on port "
					+ namespace.getString("port") + ", starting...");
			BruteForce bruteForce = new BruteForce(namespace.getString("server"), namespace.getInt("port"),
					"weblogic.jndi.WLInitialContextFactory", false, namespace.getString("cred-file"),
					namespace.getString("separator"));
			this.disableColorInObjectIfNeeded(bruteForce);
			bruteForce.searchValidCreds();
			bruteForce.printValidCreds();
		}
	}

	private void scanningT3(Namespace namespace) {
		if (namespace.getString("module") == "scan") {
			if (namespace.getString("ports") != "") {
				this.printTitle("Scanning port(s) with the T3 protocol");
				myLogger.info("You want to scan the " + namespace.getString("server") + " target on port(s) "
						+ namespace.getString("ports") + ", starting...");
				Scanner scanner = new Scanner();
				this.disableColorInObjectIfNeeded(scanner);
				scanner.scan(namespace.getString("server"), namespace.getString("ports"), namespace.getString("username"),
						namespace.getString("password"));
				scanner.printOpenedPorts();
			} else {
				this.printBadNews("You must to use --ports option in order to specify ports to scan");
			}
		}
	}

	private void setLevel(Namespace namespace) {
		if (namespace.getInt("verbose") != 0) {
			int level = namespace.getInt("verbose");
			switch (level) {
			case 1:
				myLogger.setLevel(Level.INFO);
				break;
			case 2:
				myLogger.setLevel(Level.FINE);
				break;
			case 3:
				myLogger.setLevel(Level.FINER);
				break;
			case 4:
				System.setProperty("weblogic.log.StdoutLogStack", "true"); // Specifies whether to dump stack traces to
																			// the console when included in logged
																			// message.
				System.setProperty("weblogic.log.StdoutSeverity", "INFO");
				myLogger.setLevel(Level.FINEST);
				break;
			default:
				myLogger.setLevel(Level.FINEST);
			}
			for (Handler h : myLogger.getHandlers()) {
				h.setLevel(myLogger.getLevel());
			}
		} else {
			for (Handler h : myLogger.getHandlers()) {
				h.setLevel(Level.WARNING);
			}
		}
	}

	private void colorEnable(Namespace ns) {
		if (ns.getBoolean("no-color") == false) {
			this.outputColored = false;
			this.disableColor();
		}
	}

	private Namespace handlerErrorCatch(String[] args, ArgumentParser parser, Namespace ns) {
		try {
			ns = parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}
		return ns;
	}

	private void addPortArgument(ArgumentGroup commonGroup) {
		commonGroup.addArgument("-p").dest("port").setDefault(7001).type(Integer.class).help("tcp port to use");
	}

	private void addServerOption(ArgumentGroup commonGroup) {
		commonGroup.addArgument("-s").dest("server").help("target");
	}

	public void addCredentialsOptions(ArgumentGroup commonGroup) {
		commonGroup.addArgument("-U").dest("username").setDefault("").help("username");
		commonGroup.addArgument("-P").dest("password").setDefault("").help("password");
	}

	private void addCommonOptions(Subparser parser) {
		ArgumentGroup commonGroup = parser.addArgumentGroup("common options");
		addServerOption(commonGroup);
		addCredentialsOptions(commonGroup);
		addPortArgument(commonGroup);
	}

	public void addUsefulOptions(Subparser parser) {
		ArgumentGroup usefulGroup = parser.addArgumentGroup("useful options");
		usefulGroup.addArgument("-v", "--verbose").dest("verbose").action(Arguments.count()).help("set verbosity");
		;
		usefulGroup.addArgument("--no-color").dest("no-color").action(Arguments.storeFalse())
				.help("no color in output");
	}

	public static void main(String args[]) {
		new Jndiat(args);
	}

	// Return True if disabled. Otherwise return False
	public boolean disableColorInObjectIfNeeded(MyPrinter theClass) {
		if (outputColored == false) {
			theClass.disableColor();
			return true;
		}
		;
		return false;
	}

	// set weblogic verbosity
	private void setWeblogicVerbosity() {
		/*
		 * myLogger.finer("setting logging level in weblogic modules"); Logger
		 * serverlogger = LoggingHelper.getServerLogger(); if (serverlogger==null)
		 * {myLogger.finer("serverlogger==null");}; Handler[] handlerArray =
		 * serverlogger.getHandlers(); for (int i=0; i < handlerArray.length; i++) {
		 * Handler h = handlerArray[i];
		 * if(h.getClass().getName().equals("weblogic.logging.ConsoleHandler")){
		 * h.setLevel(WLLevel.ALERT); } }
		 */
	}
}
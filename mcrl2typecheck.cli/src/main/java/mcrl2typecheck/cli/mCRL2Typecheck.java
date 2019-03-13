package mcrl2typecheck.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.action.EndNamedGoal;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.action.TransformActionContrib;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.IMessagePrinter;
import org.metaborg.core.messages.Message;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.messages.MessageType;
import org.metaborg.core.messages.WithLocationStreamMessagePrinter;
import org.metaborg.core.project.IProject;
import org.metaborg.core.transform.ITransformConfig;
import org.metaborg.core.transform.TransformConfig;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Iterables;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.RunLast;
import picocli.CommandLine.Spec;

@Command(name = "java -jar mcrl2typecheck.jar", description = "Type check mcrl2typecheck input files.")
public class mCRL2Typecheck implements Callable<Void> {

    private static final ILogger logger = LoggerUtils.logger(mCRL2Typecheck.class);

    @Spec private CommandSpec spec;
    @Option(names = { "-h", "--help" }, description = "show usage help", usageHelp = true) private boolean usageHelp;
    @Option(names = { "-t", "--types" }, description = "show types") private boolean showTypes;
    @Option(names = { "-s", "--typing" }, description = "show typing") private boolean showTyping;
    @Option(names = { "-m", "--min-typings" },
            description = "show minimal typing derivations (one expression)") private boolean showMinTypings;
    @Option(names = { "-a", "--all-typings" },
            description = "show all typing derivations (one expression)") private boolean showAllTypings;
    @Parameters(paramLabel = "FILE", description = "files to type check") private String file;

    private Spoofax S;
    private CLIUtils cli;
    private ILanguageImpl lang;
    private IProject project;
    private IContext context;
    private IMessagePrinter messagePrinter;

    @Override public Void call() throws MetaborgException {
        init();
        final Optional<ISpoofaxAnalyzeUnit> maybeAnalysisUnit = loadFile(file);
        if(!maybeAnalysisUnit.isPresent()) {
            return null;
        }
        final ISpoofaxAnalyzeUnit analysisUnit = maybeAnalysisUnit.get();
        final TransformActionContrib showTypingAction;
        if(this.showAllTypings) {
            showTypingAction = getAction("Show All Typings", lang);
        } else if(this.showMinTypings) {
            showTypingAction = getAction("Show Minimal Typings", lang);
        } else if(this.showTyping) {
            showTypingAction = getAction("Show Typings", lang);
        } else if(this.showTypes) {
            showTypingAction = getAction("Show Types", lang);
        } else {
            showTypingAction = null;
        }
        if(showTypingAction != null) {
            final String typing = transform(analysisUnit, showTypingAction);
            final String typingMsg = typing.replaceAll("(?m)^", "     ");
            messagePrinter.print(new Message("Showing typing(s):\n" + typingMsg, MessageSeverity.NOTE,
                    MessageType.INTERNAL, analysisUnit.source(), null, null), false);
        }
        return null;
    }

    private void init() throws MetaborgException {
        S = new Spoofax();
        cli = new CLIUtils(S);
        lang = loadLanguage();
        project = cli.getOrCreateCWDProject();
        if(!S.contextService.available(lang)) {
            throw new MetaborgException("Cannot create project context.");
        }
        context = S.contextService.get(project.location(), project, lang);
        final PrintStream msgStream = System.out;
        messagePrinter = new WithLocationStreamMessagePrinter(S.sourceTextService, S.projectService, msgStream);
    }

    private ILanguageImpl loadLanguage() throws MetaborgException {
        ILanguageImpl lang;
        // try loading from path
        cli.loadLanguagesFromPath();
        ILanguage L = S.languageService.getLanguage("mcrl2typecheck");
        lang = L != null ? L.activeImpl() : null;
        if(lang != null) {
            return lang;
        }
        // try loading from resources
        final FileObject langResource;
        try {
            langResource = S.resourceService.resolve("res:mcrl2typecheck.spoofax-language");
        } catch(MetaborgRuntimeException ex) {
            throw new MetaborgException("Failed to load language.", ex);
        }
        lang = cli.loadLanguage(langResource);
        if(lang != null) {
            return lang;
        }
        throw new MetaborgException("Failed to load language from path or resources.");
    }

    private Optional<ISpoofaxAnalyzeUnit> loadFile(String file) throws MetaborgException {
        final FileObject resource = S.resourceService.resolve(file);
        final String text;
        try {
            text = S.sourceTextService.text(resource);
        } catch(IOException e) {
            throw new MetaborgException("Cannot find " + file, e);
        }
        final ISpoofaxInputUnit inputUnit = S.unitService.inputUnit(resource, text, lang, null);
        final Optional<ISpoofaxParseUnit> parseUnit = parse(inputUnit);
        if(!parseUnit.isPresent()) {
            return Optional.empty();
        }
        final Optional<ISpoofaxAnalyzeUnit> analysisUnit = analyze(parseUnit.get());
        return analysisUnit;
    }

    private Optional<ISpoofaxParseUnit> parse(ISpoofaxInputUnit inputUnit) throws MetaborgException {
        final ILanguageImpl lang = context.language();
        if(!S.syntaxService.available(lang)) {
            throw new MetaborgException("Parsing not available.");
        }
        final ISpoofaxParseUnit parseUnit = S.syntaxService.parse(inputUnit);
        if(!parseUnit.valid()) {
            throw new MetaborgException("Parsing failed.");
        }
        for(IMessage message : parseUnit.messages()) {
            messagePrinter.print(message, false);
        }
        if(!parseUnit.success()) {
            logger.info("{} has syntax errors", inputUnit.source());
            return Optional.empty();
        }
        return Optional.of(parseUnit);
    }

    private Optional<ISpoofaxAnalyzeUnit> analyze(ISpoofaxParseUnit parseUnit) throws MetaborgException {
        if(!S.analysisService.available(lang) || !S.contextService.available(lang)) {
            throw new MetaborgException("Analysis not available.");
        }
        final ISpoofaxAnalyzeUnit analysisUnit;
        try(IClosableLock lock = context.write()) {
            analysisUnit = S.analysisService.analyze(parseUnit, context).result();
        }
        if(!analysisUnit.valid()) {
            throw new MetaborgException("Analysis failed.");
        }
        for(IMessage message : analysisUnit.messages()) {
            messagePrinter.print(message, false);
        }
        if(!analysisUnit.success()) {
            logger.info("{} has type errors.", parseUnit.source());
            return Optional.empty();
        }
        return Optional.of(analysisUnit);
    }

    private TransformActionContrib getAction(String name, ILanguageImpl lang) throws MetaborgException {
        final ITransformGoal goal = new EndNamedGoal(name);
        if(!S.actionService.available(lang, goal)) {
            throw new MetaborgException("Cannot find transformation " + name);
        }
        final TransformActionContrib action;
        try {
            action = Iterables.getOnlyElement(S.actionService.actionContributions(lang, goal));
        } catch(NoSuchElementException ex) {
            throw new MetaborgException("Transformation " + name + " not a singleton.");
        }
        return action;
    }

    private String transform(ISpoofaxAnalyzeUnit analysisUnit, TransformActionContrib action) throws MetaborgException {
        final ITransformConfig config = new TransformConfig(true);
        final ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit> transformUnit =
                S.transformService.transform(analysisUnit, context, action, config);
        if(!transformUnit.valid()) {
            throw new MetaborgException("Failed to transform " + analysisUnit.source());
        }
        final String details = S.strategoCommon.toString(transformUnit.ast());
        return details;
    }

    public static void main(String... args) {
        final CommandLine cmd = new CommandLine(new mCRL2Typecheck());
        cmd.parseWithHandlers(new RunLast().andExit(0), CommandLine.defaultExceptionHandler().andExit(1), args);
    }

}

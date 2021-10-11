package tp2;

import java.awt.*;
import java.awt.Dimension;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import guru.nidi.graphviz.engine.Format;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

import javax.swing.*;

public class ParserAST {
    public static final String projectPath = "C:\\Users\\Alex\\Documents\\GitHub\\TP3_Refactoring\\After_Refactoring\\GoodBank";
    //    public static final String projectPath = "C:\\Users\\Alex\\Documents\\GitHub\\uaa-develop\\server";
    public static final String projectSourcePath = projectPath + "/src";
    public static final String jrePath = "C:\\Program Files\\Java\\jre1.8.0_301";

    public static int numberOfClass = 0; // 1
    public static int numberOfCodeLines = 0; // 2
    public static int numberOfMethods = 0; // 3
    public static List<String> packageList = new ArrayList<>(); // 4
    public static int numberOfMethodsPerClass = 0; // 5
    public static int linesOfCodePerMethod = 0; // 6
    public static int numberOfAttributes = 0; // 7

    // 8
    public static HashMap<String, Integer> classesMethodsHashMap = new HashMap<>();
    public static List<Map<String, Integer>> methodsWithNumberOfLinesByClass = new ArrayList<>();
    public static List<String> classesWithMostMethods = new ArrayList<>();

    // 9
    public static HashMap<String, Integer> classesFieldsHashMap = new HashMap<>();
    public static List<String> classesWithMostFields = new ArrayList<>();

    public static int max_parameter = 0; // 13

    public static List<String> methodInvocations = new ArrayList<>(); // bonus

    public static void main(String[] args) throws IOException {
        final File folder = new File(projectSourcePath);
        ArrayList<File> javaFiles = listJavaFilesForFolder(folder);

        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry);
            CompilationUnit parse = parse(content.toCharArray());

            printMethodInvocationInfo(parse); // bonus
            printNumberOfClass(parse); // 1
            getTotalNumberOfLines(parse); // 2
            printNumberOfMethods(parse); // 3
            getNumberOfPackages(parse); // 4
            getAverageNumberOfMethodsPerClass(parse); // 5
            getLinesOfCodePerMethod(parse); // 6
            getNumberOfAttributes(parse); // 7
            putClassesMethodsInHashMap(parse); // 8
            getMethodsWithLines(parse); // 8
            putClassesFieldsInHashMap(parse); // 9
            getMaxParameters(parse); // 13
        }
        getClassesWithMostMethods(); // 8
        getClassesWithMostFields(); // 9

        // Questions 1.1 : requêtes 1 à 13 (la requête 11 demande un nombre de méthode en entrée)
//        printStatistics();

        // Question 1.2 (bonus) : ouvre une fenêtre avec les résultats des questions
        window();

        // Question 2.1 + Question 2.1 (bonus) : créer l'AST sous format .dot et l'export en png
//        createDiagram();
    }

    public static void printStatistics() {
        System.out.println("1. Nombre de classe : " + numberOfClass + "\n");
        System.out.println("2. Nombre de lignes de code : " + numberOfCodeLines + "\n");
        System.out.println("3. Nombre de méthodes : " + numberOfMethods + "\n");
        System.out.println("4. Nombre de packages : " + (int) packageList.stream().distinct().count() + "\n");
        System.out.println("5. Nombre moyen de méthodes par classe : " + numberOfMethodsPerClass / numberOfClass + "\n");
        System.out.println("6. Nombre moyen de lignes de code par méthode : " + linesOfCodePerMethod / numberOfMethods + "\n");
        System.out.println("7. Nombre moyen d’attributs par classe : " + numberOfAttributes / numberOfClass + "\n");

        System.out.println("8. Les 10% des classes qui possèdent le plus grand nombre de méthodes : \n");
        classesWithMostMethods.forEach(System.out::println);

        System.out.println("\n9. Les 10% des classes qui possèdent le plus grand nombre d’attributs : \n");
        classesWithMostFields.forEach(System.out::println);

        System.out.println("\n10. Les classes qui font partie en même temps des deux catégories précédentes : \n");
        getClassesWithMostFieldsAndMethods().forEach(System.out::println);

        System.out.println("\n11. Veuillez entrer un nombre afin de voir les classes qui possèdent plus que ce nombre de méthode (par exemple 60 pour un gros projet comme uua) : ");
        Scanner userScan = new Scanner(System.in);
        String x = userScan.nextLine();
        System.out.println("Voici les différentes classes avec plus de " + x + " méthodes : ");
        moreThanXMethods(Integer.parseInt(x));

        System.out.println("\n12. Les 10% des méthodes qui possèdent le plus grand nombre de lignes de code (par classe) : \n");
        getMethodsWithMostLines().forEach(System.out::println);

        System.out.println("\n13. Le nombre maximal de paramètres par rapport à toutes les méthodes : " + max_parameter);
    }

    // 1. Nombre de classes de l’application.
    public static void printNumberOfClass(CompilationUnit parse) {
        TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
        parse.accept(visitor);
        numberOfClass += (int) visitor.getTypes().stream().filter(typeDeclaration -> !typeDeclaration.isInterface()).count();
    }

    // 2. Nombre de lignes de code de l’application.
    public static void getTotalNumberOfLines(CompilationUnit parse) {
        TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
        parse.accept(visitor);

        visitor.getTypes().forEach(typeDeclaration -> {
            int beginning = parse.getLineNumber(typeDeclaration.getStartPosition());
            int end = parse.getLineNumber(typeDeclaration.getStartPosition() + typeDeclaration.getLength() - 1);
            numberOfCodeLines += Math.max((end - beginning), 0);
        });
        PackageVisitor visitor1 = new PackageVisitor();
        parse.accept(visitor1);
        numberOfCodeLines += visitor1.getPackageDeclarations().size();

        ImportDeclarationVisitor visitor2 = new ImportDeclarationVisitor();
        parse.accept(visitor2);
        numberOfCodeLines += visitor2.getImports().size();

        numberOfCodeLines += parse.getCommentList().size();
    }

    // 3. Nombre total de méthodes de l’application.
    public static void printNumberOfMethods(CompilationUnit parse) {
        MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
        parse.accept(visitor);
        numberOfMethods += visitor.getMethods().size();
    }

    // 4. Nombre total de packages de l’application.
    public static void getNumberOfPackages(CompilationUnit parse) {
        PackageVisitor visitor = new PackageVisitor();
        parse.accept(visitor);

        visitor.getPackageDeclarations().forEach(packageDeclaration -> packageList.add(packageDeclaration.getName().toString()));
    }

    // 5. Nombre moyen de méthodes par classe.
    public static void getAverageNumberOfMethodsPerClass(CompilationUnit parse) {
        TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
        parse.accept(visitor);
        numberOfMethodsPerClass += visitor.getTypes().stream().filter(
                typeDeclaration -> !typeDeclaration.isInterface()).mapToInt(c -> c.getMethods().length).sum();
    }

    // 6. Nombre moyen de lignes de code par méthode de l’application.
    public static void getLinesOfCodePerMethod(CompilationUnit parse) {
        MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
        parse.accept(visitor);

        for (MethodDeclaration method : visitor.getMethods()) {
            Block body = method.getBody();
            if (body == null)
                continue;
            linesOfCodePerMethod += body.statements().size();
        }
    }

    // 7. Nombre moyen d’attributs par classe.
    public static void getNumberOfAttributes(CompilationUnit parse) {
        TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
        parse.accept(visitor);

        for (TypeDeclaration typeDeclaration : visitor.getTypes()) {
            numberOfAttributes += typeDeclaration.getFields().length;
        }
    }

    // 8. Les 10% des classes qui possèdent le plus grand nombre de méthodes.
    public static void getMethodsWithLines(CompilationUnit parse) {
        TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
        parse.accept(visitor);

        for (TypeDeclaration type : visitor.getTypes()) {
            if (type.isInterface())
                continue;

            Map<String, Integer> methodsWithLines = new HashMap<>();

            for (MethodDeclaration method : type.getMethods())
                methodsWithLines.put(type.getName() + "." + method.getName(), getNumberOfLineOfAMethod(parse, method));

            methodsWithNumberOfLinesByClass.add(methodsWithLines);
        }
    }

    public static int getNumberOfLineOfAMethod(CompilationUnit parse, MethodDeclaration method) {
        if (method.getBody() == null) {
            return 0;
        }

        int beginning = parse.getLineNumber(method.getBody().getStartPosition());
        int end = parse.getLineNumber(method.getBody().getStartPosition() + method.getBody().getLength() - 1);

        return Math.max(end - beginning - 1, 0);
    }

    public static void putClassesMethodsInHashMap(CompilationUnit parse) {
        TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
        parse.accept(visitor);
        visitor.getTypes().forEach(type -> {
            if (!type.isInterface())
                classesMethodsHashMap.put(type.getName().toString(), type.getMethods().length);
        });
    }

    public static void getClassesWithMostMethods() {
        int numberOfClasses = (int) Math.ceil(0.1 * classesMethodsHashMap.size());

        List<String> classes = classesMethodsHashMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        classesWithMostMethods = classes.subList(0, numberOfClasses);
    }

    // 9. Les 10% des classes qui possèdent le plus grand nombre d’attributs.
    public static void putClassesFieldsInHashMap(CompilationUnit parse) {
        TypeDeclarationVisitor visitor = new TypeDeclarationVisitor();
        parse.accept(visitor);

        visitor.getTypes().forEach(type -> {
            if (!type.isInterface()) {
                classesFieldsHashMap.put(type.getName().toString(), type.getFields().length);
            }
        });
    }

    public static void getClassesWithMostFields() {
        int numberOfClasses = (int) Math.ceil(0.1 * classesFieldsHashMap.size());

        List<String> classes = classesFieldsHashMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        classesWithMostFields = classes.subList(0, numberOfClasses);
    }

    // 10. Les classes qui font partie en même temps des deux catégories précédentes.
    public static List<String> getClassesWithMostFieldsAndMethods() {
        List<String> res = new ArrayList<>(classesWithMostMethods);
        res.retainAll(classesWithMostFields);
        return res;
    }

    // 11. Les classes qui possèdent plus de X méthodes (la valeur de X est donnée).
    public static void moreThanXMethods(int x) {
        classesMethodsHashMap.forEach((key, value) -> {
            if (value > x) {
                System.out.println(key);
            }
        });
    }

    public static void moreThanXMethodPanel(int x, JPanel panel) {
        classesMethodsHashMap.forEach((key, value) -> {
            if (value > x) {
                panel.add(new JLabel(key));
            }
        });
    }

    // 12. Les 10% des méthodes qui possèdent le plus grand nombre de lignes de code (par classe)
    public static List<String> getMethodsWithMostLines() {
        List<String> methodsWithMostLines = new ArrayList<>();

        for (Map<String, Integer> methodsWithLines : methodsWithNumberOfLinesByClass) {
            int numberOfMethods = (int) Math.ceil(0.1 * methodsWithLines.size());

            List<String> methods = methodsWithLines.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            methodsWithMostLines.addAll(methods.subList(0, numberOfMethods));
        }

        return methodsWithMostLines;
    }

    // 13. Le nombre maximal de paramètres par rapport à toutes les méthodes de l'application.
    public static void getMaxParameters(CompilationUnit parse) {
        MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
        parse.accept(visitor);
        visitor.getMethods().forEach(methodDeclaration -> {
            if (methodDeclaration.parameters().size() > max_parameter) {
                max_parameter = methodDeclaration.parameters().size();
            }
        });
    }

    // bonus 1.2
    public static void window() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Calculs statistiques pour une application OO");
        Toolkit k = Toolkit.getDefaultToolkit();
        Dimension tailleEcran = k.getScreenSize();
        int largeurEcran = tailleEcran.width;
        int hauteurEcran = tailleEcran.height;
        frame.setSize(largeurEcran / 2, hauteurEcran);

        JPanel panel = new JPanel();
        int row = 16 + methodsWithNumberOfLinesByClass.size() + classesWithMostMethods.size() + classesWithMostFields.size() + getClassesWithMostFieldsAndMethods().size() + classesWithMostMethods.size() + getMethodsWithMostLines().size();
        panel.setLayout(new GridLayout(row, 1));

        panel.add(new JLabel("> 1. Nombre de classe : " + numberOfClass));
        panel.add(new JLabel(""));
        panel.add(new JLabel("> 2. Nombre de lignes de code : " + numberOfCodeLines));
        panel.add(new JLabel(""));
        panel.add(new JLabel("> 3. Nombre de méthodes : " + numberOfMethods));
        panel.add(new JLabel(""));
        panel.add(new JLabel("> 4. Nombre de packages : " + (int) packageList.stream().distinct().count()));
        panel.add(new JLabel(""));
        panel.add(new JLabel("> 5. Nombre moyen de méthodes par classe : " + numberOfMethodsPerClass / numberOfClass));
        panel.add(new JLabel(""));
        panel.add(new JLabel("> 6. Nombre moyen de lignes de code par méthode : " + linesOfCodePerMethod / numberOfMethods));
        panel.add(new JLabel(""));
        panel.add(new JLabel("> 7. Nombre moyen d’attributs par classe : " + numberOfAttributes / numberOfClass));
        panel.add(new JLabel(""));

        panel.add(new JLabel("> 8. Les 10% des classes qui possèdent le plus grand nombre de méthodes : "));
        panel.add(new JLabel(""));
        classesWithMostMethods.forEach(parameter -> panel.add(new JLabel(parameter)));
        panel.add(new JLabel(""));

        panel.add(new JLabel("> 9. Les 10% des classes qui possèdent le plus grand nombre d’attributs : "));
        panel.add(new JLabel(""));
        classesWithMostFields.forEach(parameter -> panel.add(new JLabel(parameter)));
        panel.add(new JLabel(""));

        panel.add(new JLabel("> 10. Les classes qui font partie en même temps des deux catégories précédentes : "));
        panel.add(new JLabel(""));
        getClassesWithMostFieldsAndMethods().forEach(parameter -> panel.add(new JLabel(parameter)));
        panel.add(new JLabel(""));

        String x = "10";
        panel.add(new JLabel("> 11. Les classes qui possèdent plus de X méthodes (pour X = " + x + ")"));
        panel.add(new JLabel(""));
        panel.add(new JLabel("Voici les différentes classes avec plus de " + x + " méthodes : "));
        panel.add(new JLabel(""));
        moreThanXMethodPanel(Integer.parseInt(x), panel);

        panel.add(new JLabel("> 12. Les 10% des méthodes qui possèdent le plus grand nombre de lignes de code : "));
        panel.add(new JLabel(""));
        getMethodsWithMostLines().forEach(parameter -> panel.add(new JLabel(parameter)));

        panel.add(new JLabel("> 13. Le nombre maximal de paramètres par rapport à toutes les méthodes : " + max_parameter));

        frame.add(new JScrollPane(panel));
        frame.setVisible(true);
    }

    // bonus 2.2
    public static void createDiagram() {
        System.setProperty("java.awt.headless", "false");
        try {
            String name = UUID.randomUUID().toString();
            FileWriter writer = new FileWriter("export/dot/" + name + ".dot");
            writer.write("digraph \"call-graph\" {\n");
            methodInvocations.stream().distinct().collect(Collectors.toList()).forEach(methodInvocation -> {
                try {
                    writer.write(methodInvocation);
                } catch (IOException e) {
                    System.out.println("Une erreur est survenue au niveau de l'écriture des liens");
                }
            });
            writer.write("}");
            writer.close();
            System.out.println("");
            System.out.println("un fichier a bien été créé");
            convertDiagramToPng(name);
        } catch (IOException e) {
            System.out.println("Une erreur s'est produite.");
        }
    }

    public static void convertDiagramToPng(String name) {
        try (InputStream dot = new FileInputStream("export/dot/" + name + ".dot")) {
            MutableGraph g = new Parser().read(dot);
            Graphviz.fromGraph(g).width(10000).render(Format.PNG).toFile(new File("export/images/" + name + ".png"));
            System.out.println("Votre graphique a été généré au format PNG");
            showView("export/images/" + name + ".png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showView(String filename) throws IOException {
        Desktop.getDesktop().open(new File(filename));
    }

    // read all java files from specific folder
    public static ArrayList<File> listJavaFilesForFolder(final File folder) {
        ArrayList<File> javaFiles = new ArrayList<>();
        for (File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (fileEntry.isDirectory()) {
                javaFiles.addAll(listJavaFilesForFolder(fileEntry));
            } else if (fileEntry.getName().contains(".java")) {
                // System.out.println(fileEntry.getName());
                javaFiles.add(fileEntry);
            }
        }
        return javaFiles;
    }

    // create AST
    private static CompilationUnit parse(char[] classSource) {
        ASTParser parser = ASTParser.newParser(AST.JLS4); // java +1.6
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);
        Map<String, String> options = JavaCore.getOptions();
        parser.setCompilerOptions(options);
        parser.setUnitName("");
        String[] sources = {projectSourcePath};
        String[] classpath = {jrePath};
        parser.setEnvironment(classpath, sources, new String[]{"UTF-8"}, true);
        parser.setSource(classSource);
        return (CompilationUnit) parser.createAST(null); // create and parse
    }

    // navigate method information
    public static void printMethodInfo(CompilationUnit parse) {
        MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
        parse.accept(visitor);
        for (MethodDeclaration method : visitor.getMethods()) {
            System.out.println("Method name: " + method.getName()
                    + " Return type: " + method.getReturnType2());
        }
    }

    // navigate variables inside method
    public static void printVariableInfo(CompilationUnit parse) {
        MethodDeclarationVisitor visitor1 = new MethodDeclarationVisitor();
        parse.accept(visitor1);
        for (MethodDeclaration method : visitor1.getMethods()) {

            VariableDeclarationFragmentVisitor visitor2 = new VariableDeclarationFragmentVisitor();
            method.accept(visitor2);

            for (VariableDeclarationFragment variableDeclarationFragment : visitor2
                    .getVariables()) {
                System.out.println("variable name: "
                        + variableDeclarationFragment.getName()
                        + " variable Initializer: "
                        + variableDeclarationFragment.getInitializer());
            }

        }
    }

    // navigate method invocations inside method
    public static void printMethodInvocationInfo(CompilationUnit parse) {
        MethodDeclarationVisitor visitor1 = new MethodDeclarationVisitor();
        parse.accept(visitor1);
        for (MethodDeclaration method : visitor1.getMethods()) {

            MethodInvocationVisitor visitor2 = new MethodInvocationVisitor();
            method.accept(visitor2);
            StringBuilder methodName = new StringBuilder();
            if (method.resolveBinding() != null) {
                if (method.resolveBinding().getDeclaringClass() != null) {
                    methodName.append(method.resolveBinding().getDeclaringClass().getName()).append(".");
                }
            }
            methodName.append(method.getName().toString()).append("()");
            for (MethodInvocation methodInvocation : visitor2.getMethods()) {
                IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
                StringBuilder methodInvoName = new StringBuilder();
                if (methodBinding != null) {
                    ITypeBinding classTypeBinding = methodBinding.getDeclaringClass();
                    if (classTypeBinding != null) {
                        methodInvoName.append(classTypeBinding.getName()).append(".");
                    }
                }
                methodInvoName.append(methodInvocation.getName());
                methodInvocations.add("\t" + "\"" + methodName + "\"->\"" + methodInvoName + "()\";\n");
            }
        }
    }
}
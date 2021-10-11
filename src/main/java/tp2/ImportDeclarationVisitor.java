package tp2;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ImportDeclarationVisitor extends ASTVisitor {
    List<ImportDeclaration> imports = new ArrayList<>();

    @Override
    public boolean visit(ImportDeclaration node) {
        imports.add(node);
        return super.visit(node);
    }

    public List<ImportDeclaration> getImports() {
        return imports;
    }
}
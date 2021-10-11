package tp2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class PackageVisitor extends ASTVisitor {
    List<PackageDeclaration> packagesDeclarations = new ArrayList<>();

    public boolean visit(PackageDeclaration node) {
        packagesDeclarations.add(node);
        return super.visit(node);
    }

    public List<PackageDeclaration> getPackageDeclarations() {
        return packagesDeclarations;
    }
}

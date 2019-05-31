package de.upb.soot.util.printer;

import de.upb.soot.core.Body;
import de.upb.soot.core.SootField;
import de.upb.soot.core.SootMethod;
import de.upb.soot.jimple.basic.StmtBox;
import de.upb.soot.jimple.common.ref.IdentityRef;
import de.upb.soot.jimple.common.stmt.Stmt;
import de.upb.soot.types.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class LabeledStmtPrinter extends AbstractStmtPrinter {
  /** branch targets * */
  protected Map<Stmt, String> labels;
  /** for unit references in Phi nodes * */
  protected Map<Stmt, String> references;

  protected String labelIndent = "\u0020\u0020\u0020\u0020\u0020";

  public LabeledStmtPrinter(Body b) {
    createLabelMaps(b);
  }

  public Map<Stmt, String> labels() {
    return labels;
  }

  public Map<Stmt, String> references() {
    return references;
  }

  @Override
  public abstract void literal(String s);

  @Override
  public abstract void method(SootMethod m);

  @Override
  public abstract void field(SootField f);

  @Override
  public abstract void identityRef(IdentityRef r);

  @Override
  public abstract void typeSignature(Type t);

  @Override
  public void stmtRef(Stmt u, boolean branchTarget) {
    String oldIndent = getIndent();

    // normal case, ie labels
    if (branchTarget) {
      setIndent(labelIndent);
      handleIndent();
      setIndent(oldIndent);
      String label = labels.get(u);
      if (label == null || "<unnamed>".equals(label)) {
        label = "[?= " + u + "]";
      }
      output.append(label);
    }
    // refs to control flow predecessors (for Shimple)
    else {
      String ref = references.get(u);

      if (startOfLine) {
        String newIndent = "(" + ref + ")" + indent.substring(ref.length() + 2);

        setIndent(newIndent);
        handleIndent();
        setIndent(oldIndent);
      } else {
        output.append(ref);
      }
    }
  }

  private void createLabelMaps(Body body) {
    Collection<Stmt> stmts = body.getStmts();

    labels = new HashMap<>(stmts.size() * 2 + 1, 0.7f);
    references = new HashMap<>(stmts.size() * 2 + 1, 0.7f);

    // Create statement name table
    Set<Stmt> labelStmts = new HashSet<>();
    Set<Stmt> refStmts = new HashSet<>();

    // Build labelStmts and refStmts
    for (StmtBox box : body.getAllStmtBoxes()) {
      Stmt stmt = box.getStmt();

      if (box.isBranchTarget()) {
        labelStmts.add(stmt);
      } else {
        refStmts.add(stmt);
      }
    }

    // left side zero padding for all labels
    // this simplifies debugging the jimple code in simple editors, as it
    // avoids the situation where a label is the prefix of another label
    final int maxDigits = 1 + (int) Math.log10(labelStmts.size());
    final String formatString = "label%0" + maxDigits + "d";

    int labelCount = 0;
    int refCount = 0;

    // Traverse the stmts and assign a label if necessary
    for (Stmt s : stmts) {
      if (labelStmts.contains(s)) {
        labels.put(s, String.format(formatString, ++labelCount));
      }

      if (refStmts.contains(s)) {
        references.put(s, Integer.toString(refCount++));
      }
    }
  }
}

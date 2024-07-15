package qilin.core.builder.callgraph;

import qilin.core.context.Context;
import qilin.core.pag.ContextMethod;
import qilin.util.Invalidable;
import sootup.callgraph.CallGraph;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.InvokableStmt;
import sootup.core.model.SootMethod;

/**
 * Represents a single edge in a call graph.
 *
 * @author Ondrej Lhotak
 */
public final class Edge extends CallGraph.Call implements Invalidable {

  /**
   * The method in which the call occurs; may be null for calls not occurring in a specific method
   * (eg. implicit calls by the VM)
   */
  private ContextMethod src;

  /** The target method of the call edge. */
  private ContextMethod tgt;

  /**
   * The kind of edge. Note: kind should not be tested by other classes; instead, accessors such as
   * isExplicit() should be added.
   */
  private final Kind kind;

  private boolean invalid = false;

  public Edge(ContextMethod src, InvokableStmt srcUnit, ContextMethod tgt, Kind kind) {
    super(src.method().getSignature(), tgt.method().getSignature(), srcUnit);
    this.src = src;
    this.tgt = tgt;
    this.kind = kind;
  }

  public Edge(ContextMethod src, InvokableStmt srcUnit, ContextMethod tgt) {
    super(src.method().getSignature(), tgt.method().getSignature(), srcUnit);
    this.kind = ieToKind(srcUnit.getInvokeExpr().get());
    this.src = src;
    this.tgt = tgt;
  }

  public SootMethod src() {
    return (src == null) ? null : src.method();
  }

  public Context srcCtxt() {
    return (src == null) ? null : src.context();
  }

  public ContextMethod getSrc() {
    return src;
  }

  public InvokableStmt srcUnit() {
    return getInvokableStmt();
  }

  public InvokableStmt srcStmt() {
    return getInvokableStmt();
  }

  public SootMethod tgt() {
    return (tgt == null) ? null : tgt.method();
  }

  public Context tgtCtxt() {
    return (tgt == null) ? null : tgt.context();
  }

  public ContextMethod getTgt() {
    return tgt;
  }

  public Kind kind() {
    return kind;
  }

  public static Kind ieToKind(AbstractInvokeExpr ie) {
    if (ie instanceof JVirtualInvokeExpr) {
      return Kind.VIRTUAL;
    } else if (ie instanceof JSpecialInvokeExpr) {
      return Kind.SPECIAL;
    } else if (ie instanceof JInterfaceInvokeExpr) {
      return Kind.INTERFACE;
    } else if (ie instanceof JStaticInvokeExpr) {
      return Kind.STATIC;
    } else {
      throw new RuntimeException();
    }
  }

  /** Returns true if the call is due to an explicit invoke statement. */
  public boolean isExplicit() {
    return Kind.isExplicit(this.kind);
  }

  /** Returns true if the call is due to an explicit instance invoke statement. */
  public boolean isInstance() {
    return Kind.isInstance(this.kind);
  }

  public boolean isVirtual() {
    return Kind.isVirtual(this.kind);
  }

  public boolean isSpecial() {
    return Kind.isSpecial(this.kind);
  }

  /** Returns true if the call is to static initializer. */
  public boolean isClinit() {
    return Kind.isClinit(this.kind);
  }

  /** Returns true if the call is due to an explicit static invoke statement. */
  public boolean isStatic() {
    return Kind.isStatic(this.kind);
  }

  public boolean isThreadRunCall() {
    return Kind.isThread(this.kind);
  }

  public boolean passesParameters() {
    return Kind.passesParameters(this.kind);
  }

  @Override
  public boolean isInvalid() {
    return invalid;
  }

  @Override
  public void invalidate() {
    // Since the edge remains in the QueueReaders for a while, the GC could not claim old units.
    src = null;
    tgt = null;
    invalid = true;
  }

  @Override
  public int hashCode() {
    if (invalid) {
      return 0;
    }
    int ret = (tgt.hashCode() + 20) + (kind == null ? 0 : kind.getNumber());
    if (src != null) {
      ret = ret * 32 + src.hashCode();
    }
    if (getInvokableStmt() != null) {
      ret = ret * 32 + getInvokableStmt().hashCode();
    }
    return ret;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Edge)) {
      return false;
    }
    Edge o = (Edge) other;
    return (o.src == this.src)
        && (o.srcStmt() == this.srcStmt())
        && (o.tgt == tgt)
        && (o.kind == kind);
  }

  @Override
  public String toString() {
    return this.kind + " edge: " + srcStmt() + " in " + src + " ==> " + tgt;
  }

  private Edge nextByUnit = this;
  private Edge prevByUnit = this;
  private Edge nextBySrc = this;
  private Edge prevBySrc = this;
  private Edge nextByTgt = this;
  private Edge prevByTgt = this;

  void insertAfterByUnit(Edge other) {
    nextByUnit = other.nextByUnit;
    nextByUnit.prevByUnit = this;
    other.nextByUnit = this;
    prevByUnit = other;
  }

  void insertAfterBySrc(Edge other) {
    nextBySrc = other.nextBySrc;
    nextBySrc.prevBySrc = this;
    other.nextBySrc = this;
    prevBySrc = other;
  }

  void insertAfterByTgt(Edge other) {
    nextByTgt = other.nextByTgt;
    nextByTgt.prevByTgt = this;
    other.nextByTgt = this;
    prevByTgt = other;
  }

  void insertBeforeByUnit(Edge other) {
    prevByUnit = other.prevByUnit;
    prevByUnit.nextByUnit = this;
    other.prevByUnit = this;
    nextByUnit = other;
  }

  void insertBeforeBySrc(Edge other) {
    prevBySrc = other.prevBySrc;
    prevBySrc.nextBySrc = this;
    other.prevBySrc = this;
    nextBySrc = other;
  }

  void insertBeforeByTgt(Edge other) {
    prevByTgt = other.prevByTgt;
    prevByTgt.nextByTgt = this;
    other.prevByTgt = this;
    nextByTgt = other;
  }

  void remove() {
    invalid = true;
    nextByUnit.prevByUnit = prevByUnit;
    prevByUnit.nextByUnit = nextByUnit;
    nextBySrc.prevBySrc = prevBySrc;
    prevBySrc.nextBySrc = nextBySrc;
    nextByTgt.prevByTgt = prevByTgt;
    prevByTgt.nextByTgt = nextByTgt;
  }

  Edge nextByUnit() {
    return nextByUnit;
  }

  Edge nextBySrc() {
    return nextBySrc;
  }

  Edge nextByTgt() {
    return nextByTgt;
  }

  Edge prevByUnit() {
    return prevByUnit;
  }

  Edge prevBySrc() {
    return prevBySrc;
  }

  Edge prevByTgt() {
    return prevByTgt;
  }
}
